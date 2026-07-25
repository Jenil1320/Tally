package com.tally.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tally.core.money.MoneyFormat
import com.tally.core.money.Pence
import com.tally.domain.insight.CategoryLimitStore
import com.tally.domain.notify.NotificationIngestPreferences
import com.tally.domain.reminder.ReminderPreferences
import com.tally.domain.reminder.ReminderScheduler
import com.tally.domain.repo.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One category as shown in the limits editor. [limitText] is blank when unset. */
data class CategoryLimitRow(
    val categoryId: String,
    val name: String,
    val colorIndex: Int,
    val limitText: String,
)

data class SettingsUiState(
    val reminderEnabled: Boolean = true,
    val reminderTime: String = "22:00",
    val captureEnabled: Boolean = false,
    val limitRows: List<CategoryLimitRow> = emptyList(),
    val loaded: Boolean = false,
)

/**
 * Backs the Settings screen: the daily-reminder toggle (M2) and the payment
 * notification-capture toggle (M4). Capture-enabled is Tally's own switch; the
 * screen separately checks the OS "Notification access" grant.
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val reminderPreferences: ReminderPreferences,
        private val reminderScheduler: ReminderScheduler,
        private val notificationIngestPreferences: NotificationIngestPreferences,
        private val categoryLimitStore: CategoryLimitStore,
        categoryRepository: CategoryRepository,
    ) : ViewModel() {
        val uiState: StateFlow<SettingsUiState> =
            combine(
                reminderPreferences.state,
                notificationIngestPreferences.enabled,
                categoryRepository.observeAll(),
                categoryLimitStore.limits,
            ) { reminder, captureEnabled, categories, limits ->
                SettingsUiState(
                    reminderEnabled = reminder.enabled,
                    reminderTime = "%02d:%02d".format(reminder.hour, reminder.minute),
                    captureEnabled = captureEnabled,
                    limitRows =
                        categories.map { category ->
                            CategoryLimitRow(
                                categoryId = category.id,
                                name = category.name,
                                colorIndex = category.colorIndex,
                                limitText =
                                    limits[category.id]
                                        ?.let { MoneyFormat.format(it, symbol = false, grouped = false) }
                                        .orEmpty(),
                            )
                        },
                    loaded = true,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

        /**
         * Applies a typed limit for [categoryId]. Blank clears it; anything that
         * isn't a valid GBP amount is ignored, so a half-typed value never wipes
         * a saved limit.
         */
        fun setCategoryLimit(
            categoryId: String,
            text: String,
        ) {
            val trimmed = text.trim()
            val limit: Pence? = if (trimmed.isEmpty()) null else MoneyFormat.parse(trimmed) ?: return
            viewModelScope.launch { categoryLimitStore.setLimit(categoryId, limit) }
        }

        fun setReminderEnabled(enabled: Boolean) {
            viewModelScope.launch {
                reminderPreferences.setEnabled(enabled)
                val state = reminderPreferences.state.first()
                if (enabled) {
                    reminderScheduler.schedule(state.hour, state.minute)
                } else {
                    reminderScheduler.cancel()
                }
            }
        }

        fun setCaptureEnabled(enabled: Boolean) {
            viewModelScope.launch {
                notificationIngestPreferences.setEnabled(enabled)
            }
        }
    }
