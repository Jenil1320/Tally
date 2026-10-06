package com.tally.ui.screens.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.core.money.Pence
import com.tally.core.time.Clock
import com.tally.domain.category.MerchantRuleStore
import com.tally.domain.category.normaliseMerchant
import com.tally.domain.category.suggestCategory
import com.tally.domain.insight.isEatingOut
import com.tally.domain.model.Category
import com.tally.domain.model.NewTransaction
import com.tally.domain.model.TransactionSource
import com.tally.domain.repo.CategoryRepository
import com.tally.domain.repo.TransactionRepository
import com.tally.ui.model.dayLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

private const val MAX_PENCE = 9_999_999_99L // £9,999,999.99 cap

/** How much of an eating-out bill was the user's own. */
enum class ShareChoice(val label: String) {
    All("All mine"),
    Half("Half"),
    Third("A third"),
    Custom("Custom"),
}

/** Which figure the keypad is typing into. */
enum class AmountField { Total, Share }

/**
 * State for the Add/Edit sheet.
 *
 * Amount is a running pence accumulator. [occurredAtMillis] is the editable
 * transaction date (defaults to now for a new entry, or the original on edit),
 * shown as [occurredAtLabel]. [canSave] gates Save; [editingId] non-null means
 * edit mode (revealing Delete/Duplicate).
 *
 * For an Eating out entry the sheet also asks for the user's share
 * ([asksForShare]). [myShare] is the resolved figure that is saved. It is null
 * for every other category, and when the whole bill was the user's own.
 * [source] is kept from the loaded transaction, so editing an auto-captured
 * payment does not turn it into a manual one.
 */
data class AddUiState(
    val amountPence: Long = 0L,
    val merchant: String = "",
    val note: String = "",
    val selectedCategoryId: String? = null,
    val categories: List<Category> = emptyList(),
    val categoryManuallySet: Boolean = false,
    val occurredAtMillis: Long = 0L,
    val occurredAtLabel: String = "Today",
    val editingId: String? = null,
    val saving: Boolean = false,
    val source: TransactionSource = TransactionSource.MANUAL,
    val shareChoice: ShareChoice = ShareChoice.All,
    val customSharePence: Long? = null,
    val activeField: AmountField = AmountField.Total,
) {
    val amount: Pence get() = Pence(amountPence)
    val isEditing: Boolean get() = editingId != null

    /** True when the chosen category is Eating out, so the share question shows. */
    val asksForShare: Boolean get() = categories.firstOrNull { it.id == selectedCategoryId }?.isEatingOut == true

    /** The user's share to save, or null when the whole amount is theirs (or it was not asked). */
    val myShare: Pence?
        get() {
            if (!asksForShare || amountPence <= 0) return null
            return when (shareChoice) {
                ShareChoice.All -> null
                ShareChoice.Half -> amount.splitEvenly(2).first()
                ShareChoice.Third -> amount.splitEvenly(3).first()
                ShareChoice.Custom -> customSharePence?.let { Pence(it.coerceAtMost(amountPence)) }
            }
        }

    private val customShareMissing: Boolean
        get() = asksForShare && shareChoice == ShareChoice.Custom && customSharePence == null

    val canSave: Boolean get() = amountPence > 0 && merchant.isNotBlank() && !saving && !customShareMissing
}

/**
 * Drives the Add/Edit sheet: keypad amount, auto-suggested category (learned then
 * bundled, taught on save), an editable date, and edit/delete/duplicate.
 */
@HiltViewModel
class AddViewModel
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        categoryRepository: CategoryRepository,
        private val merchantRuleStore: MerchantRuleStore,
        private val clock: Clock,
    ) : ViewModel() {
        private val timeZone = TimeZone.currentSystemDefault()
        private val _uiState = MutableStateFlow(AddUiState())
        val uiState = _uiState.asStateFlow()

        private var learnedRules: Map<String, String> = emptyMap()

        init {
            val now = clock.nowMillis()
            _uiState.update { it.copy(occurredAtMillis = now, occurredAtLabel = labelFor(now)) }
            viewModelScope.launch {
                categoryRepository.observeAll().collect { categories ->
                    _uiState.update { state ->
                        state.copy(
                            categories = categories,
                            selectedCategoryId = state.selectedCategoryId ?: categories.firstOrNull()?.id,
                        )
                    }
                }
            }
            viewModelScope.launch {
                merchantRuleStore.rules.collect { learnedRules = it }
            }
        }

        /** Initialise the sheet: a fresh entry (dated now) when [editingId] is null, else load it. */
        fun start(editingId: String?) {
            if (editingId == null) {
                resetForm()
                return
            }
            viewModelScope.launch {
                val transaction = transactionRepository.findById(editingId) ?: return@launch
                val millis = transaction.occurredAt.toEpochMilliseconds()
                val (choice, custom) = shareStateFor(transaction.amount, transaction.myShare)
                _uiState.update { state ->
                    state.copy(
                        amountPence = transaction.amount.value,
                        merchant = transaction.merchant,
                        note = transaction.note.orEmpty(),
                        selectedCategoryId = transaction.categoryId ?: state.categories.firstOrNull()?.id,
                        categoryManuallySet = true,
                        occurredAtMillis = millis,
                        occurredAtLabel = labelFor(millis),
                        editingId = editingId,
                        saving = false,
                        source = transaction.source,
                        shareChoice = choice,
                        customSharePence = custom,
                        activeField = AmountField.Total,
                    )
                }
            }
        }

        fun onDigit(digit: Int) = typeInto { (it * 10 + digit).coerceAtMost(MAX_PENCE) }

        fun onDoubleZero() = typeInto { (it * 100).coerceAtMost(MAX_PENCE) }

        fun onBackspace() = typeInto { it / 10 }

        /** Applies [edit] to whichever figure the keypad is typing into: the total or the custom share. */
        private fun typeInto(edit: (Long) -> Long) {
            _uiState.update { state ->
                if (state.asksForShare && state.activeField == AmountField.Share) {
                    state.copy(customSharePence = edit(state.customSharePence ?: 0L))
                } else {
                    state.copy(amountPence = edit(state.amountPence))
                }
            }
        }

        /** Picks how the bill was split. Custom hands the keypad to the share figure. */
        fun onShareChoice(choice: ShareChoice) {
            _uiState.update {
                it.copy(
                    shareChoice = choice,
                    activeField = if (choice == ShareChoice.Custom) AmountField.Share else AmountField.Total,
                )
            }
        }

        /** Points the keypad at the total or at the share. Choosing the share implies Custom. */
        fun onFieldSelected(field: AmountField) {
            _uiState.update {
                if (field == AmountField.Share && it.asksForShare) {
                    it.copy(shareChoice = ShareChoice.Custom, activeField = AmountField.Share)
                } else {
                    it.copy(activeField = AmountField.Total)
                }
            }
        }

        fun onMerchantChange(value: String) {
            _uiState.update { state ->
                val suggestion =
                    if (state.categoryManuallySet) {
                        state.selectedCategoryId
                    } else {
                        suggestCategory(value, state.categories, learnedRules)
                            ?: state.categories.firstOrNull()?.id
                    }
                state.copy(merchant = value, selectedCategoryId = suggestion)
            }
        }

        fun onNoteChange(value: String) {
            _uiState.update { it.copy(note = value) }
        }

        fun onCategorySelected(categoryId: String) {
            _uiState.update { it.copy(selectedCategoryId = categoryId, categoryManuallySet = true) }
        }

        /**
         * Apply a date from the Material date picker (which reports the picked day
         * as UTC midnight). Keeps the current time-of-day so ordering within a day
         * is preserved.
         */
        fun onDateSelected(utcMidnightMillis: Long) {
            val pickedDate = Instant.fromEpochMilliseconds(utcMidnightMillis).toLocalDateTime(TimeZone.UTC).date
            val currentTime = Instant.fromEpochMilliseconds(_uiState.value.occurredAtMillis).toLocalDateTime(timeZone).time
            val combined = LocalDateTime(pickedDate, currentTime).toInstant(timeZone).toEpochMilliseconds()
            _uiState.update { it.copy(occurredAtMillis = combined, occurredAtLabel = labelFor(combined)) }
        }

        fun save(onSaved: () -> Unit) {
            val state = _uiState.value
            if (!state.canSave) return
            _uiState.update { it.copy(saving = true) }
            viewModelScope.launch {
                val merchant = state.merchant.trim()
                val categoryId = state.selectedCategoryId
                val draft =
                    NewTransaction(
                        amount = state.amount,
                        merchant = merchant,
                        occurredAt = Instant.fromEpochMilliseconds(state.occurredAtMillis),
                        note = state.note.trim().ifBlank { null },
                        categoryId = categoryId,
                        source = state.source,
                        myShare = state.myShare,
                    )
                val editingId = state.editingId
                if (editingId != null) {
                    transactionRepository.update(editingId, draft)
                } else {
                    transactionRepository.add(draft)
                }
                if (categoryId != null && merchant.isNotBlank()) {
                    merchantRuleStore.learn(normaliseMerchant(merchant), categoryId)
                }
                resetForm()
                onSaved()
            }
        }

        fun delete(onDone: () -> Unit) {
            val editingId = _uiState.value.editingId ?: return
            viewModelScope.launch {
                transactionRepository.softDelete(editingId)
                resetForm()
                onDone()
            }
        }

        /** Create a fresh copy of the current entry, dated now. */
        fun duplicate(onDone: () -> Unit) {
            val state = _uiState.value
            val merchant = state.merchant.trim()
            if (state.amountPence <= 0 || merchant.isBlank()) return
            viewModelScope.launch {
                transactionRepository.add(
                    NewTransaction(
                        amount = state.amount,
                        merchant = merchant,
                        occurredAt = clock.now(),
                        note = state.note.trim().ifBlank { null },
                        categoryId = state.selectedCategoryId,
                        source = TransactionSource.MANUAL,
                        myShare = state.myShare,
                    ),
                )
                resetForm()
                onDone()
            }
        }

        /** Maps a saved share back onto the sheet's choices, so editing shows what was set. */
        private fun shareStateFor(
            amount: Pence,
            myShare: Pence?,
        ): Pair<ShareChoice, Long?> =
            when {
                myShare == null || amount.value <= 0 || myShare >= amount -> ShareChoice.All to null
                myShare == amount.splitEvenly(2).first() -> ShareChoice.Half to null
                myShare == amount.splitEvenly(3).first() -> ShareChoice.Third to null
                else -> ShareChoice.Custom to myShare.value
            }

        private fun labelFor(millis: Long): String {
            val date = Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone).date
            val today = clock.now().toLocalDateTime(timeZone).date
            return dayLabel(date, today)
        }

        private fun resetForm() {
            val now = clock.nowMillis()
            _uiState.value =
                AddUiState(
                    categories = _uiState.value.categories,
                    selectedCategoryId = _uiState.value.categories.firstOrNull()?.id,
                    occurredAtMillis = now,
                    occurredAtLabel = labelFor(now),
                )
        }
    }
