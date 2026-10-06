package com.tally.ui.screens.add

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.domain.model.Category
import com.tally.ui.components.TButton
import com.tally.ui.components.TCard
import com.tally.ui.components.TCatChip
import com.tally.ui.components.TKeypad
import com.tally.ui.components.TKicker
import com.tally.ui.components.TNum
import com.tally.ui.components.TNumSize
import com.tally.ui.components.TText
import com.tally.ui.components.categoryColor
import com.tally.ui.theme.TallyMotion
import com.tally.ui.theme.TallyTheme

/** Everything the sheet can ask of its ViewModel, so [AddSheetContent] can be drawn without one. */
internal data class AddActions(
    val onMerchantChange: (String) -> Unit = {},
    val onNoteChange: (String) -> Unit = {},
    val onCategorySelected: (String) -> Unit = {},
    val onShareChoice: (ShareChoice) -> Unit = {},
    val onFieldSelected: (AmountField) -> Unit = {},
    val onDigit: (Int) -> Unit = {},
    val onDoubleZero: () -> Unit = {},
    val onBackspace: () -> Unit = {},
    val onDateClick: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onDuplicate: () -> Unit = {},
    val onDelete: () -> Unit = {},
)

/**
 * Add/Edit sheet. Opened fresh for a new entry or with [editingId] to edit.
 * Amount (keypad), merchant, auto-suggested category, optional note, and an
 * editable date. Choosing Eating out also asks for the user's share. Editing
 * reveals Duplicate and Delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSheet(
    editingId: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.start(editingId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    AddSheetContent(
        state = state,
        actions =
            AddActions(
                onMerchantChange = viewModel::onMerchantChange,
                onNoteChange = viewModel::onNoteChange,
                onCategorySelected = viewModel::onCategorySelected,
                onShareChoice = viewModel::onShareChoice,
                onFieldSelected = viewModel::onFieldSelected,
                onDigit = viewModel::onDigit,
                onDoubleZero = viewModel::onDoubleZero,
                onBackspace = viewModel::onBackspace,
                onDateClick = { showDatePicker = true },
                onSave = { viewModel.save(onSaved = onDismiss) },
                onDuplicate = { viewModel.duplicate(onDone = onDismiss) },
                onDelete = { viewModel.delete(onDone = onDismiss) },
            ),
        modifier = modifier,
    )

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.occurredAtMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let(viewModel::onDateSelected)
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
internal fun AddSheetContent(
    state: AddUiState,
    actions: AddActions,
    modifier: Modifier = Modifier,
) {
    var noteOpen by remember { mutableStateOf(false) }
    val showNote = noteOpen || state.note.isNotEmpty()

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = TallyTheme.spacing.lg)
                .padding(bottom = TallyTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            TKicker(if (state.isEditing) "Edit transaction" else "New transaction")
            TText(text = "Spend", style = TallyTheme.typography.title, modifier = Modifier.padding(top = 2.dp))
        }

        AmountBlock(state = state, onFieldSelected = actions.onFieldSelected)

        WhereCard(
            state = state,
            showNote = showNote,
            onToggleNote = { noteOpen = !noteOpen },
            onMerchantChange = actions.onMerchantChange,
            onNoteChange = actions.onNoteChange,
        )

        TKicker("Category", modifier = Modifier.padding(horizontal = 4.dp))
        CategoryTiles(
            categories = state.categories,
            selectedId = state.selectedCategoryId,
            onSelect = actions.onCategorySelected,
        )

        AnimatedVisibility(
            visible = state.asksForShare,
            enter = expandVertically(TallyMotion.standard()) + fadeIn(TallyMotion.standard()),
            exit = shrinkVertically(TallyMotion.standard()) + fadeOut(TallyMotion.standard()),
        ) {
            ShareCard(state = state, onChoice = actions.onShareChoice, onFieldSelected = actions.onFieldSelected)
        }

        DateRow(label = state.occurredAtLabel, onClick = actions.onDateClick)

        TKeypad(
            onDigit = actions.onDigit,
            onDoubleZero = actions.onDoubleZero,
            onBackspace = actions.onBackspace,
        )

        TButton(
            text = if (state.isEditing) "Save changes" else "Save",
            onClick = actions.onSave,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.isEditing) {
            val colors = TallyTheme.colors
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextAction(label = "Duplicate", color = colors.textMid, onClick = actions.onDuplicate)
                TextAction(label = "Delete", color = colors.danger, onClick = actions.onDelete)
            }
        }
    }
}

/**
 * The big figure. For an Eating out entry the figure is the amount paid, and
 * tapping it points the keypad back at the total after typing a custom share.
 */
@Composable
private fun AmountBlock(
    state: AddUiState,
    onFieldSelected: (AmountField) -> Unit,
) {
    val colors = TallyTheme.colors
    val shareActive = state.asksForShare && state.activeField == AmountField.Share
    val amountColor =
        when {
            shareActive -> colors.textMid
            state.amountPence == 0L -> colors.textDim
            else -> colors.textHi
        }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = state.asksForShare,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClickLabel = "Type the amount paid",
                    onClick = { onFieldSelected(AmountField.Total) },
                )
                .padding(vertical = TallyTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TKicker(if (state.asksForShare) "You paid" else "You spent")
        TNum(
            amount = state.amount,
            size = TNumSize.Hero,
            color = amountColor,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun WhereCard(
    state: AddUiState,
    showNote: Boolean,
    onToggleNote: () -> Unit,
    onMerchantChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
) {
    val colors = TallyTheme.colors
    TCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                TKicker("Where")
                InlineField(
                    value = state.merchant,
                    onValueChange = onMerchantChange,
                    placeholder = "Merchant",
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            TText(
                text = if (showNote && state.note.isEmpty()) "Hide note" else "+ note",
                style = TallyTheme.typography.numericSmall,
                color = colors.accent,
                modifier =
                    Modifier
                        .clip(TallyTheme.shapes.small)
                        .clickable(role = Role.Button, onClick = onToggleNote)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
            )
        }
        AnimatedVisibility(
            visible = showNote,
            enter = expandVertically(TallyMotion.standard()) + fadeIn(TallyMotion.standard()),
            exit = shrinkVertically(TallyMotion.standard()) + fadeOut(TallyMotion.standard()),
        ) {
            Column {
                Box(modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth().height(1.dp).background(colors.lineSoft))
                InlineField(
                    value = state.note,
                    onValueChange = onNoteChange,
                    placeholder = "Note (optional)",
                )
            }
        }
    }
}

/** A plain text field that sits inside a card: no fill, an accent cursor, a quiet placeholder. */
@Composable
private fun InlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = TallyTheme.typography.bodyStrong.copy(color = colors.textHi),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
        decorationBox = { inner ->
            Box(modifier = Modifier.height(22.dp)) {
                if (value.isEmpty()) {
                    TText(text = placeholder, style = TallyTheme.typography.bodyStrong, color = colors.textDim)
                }
                inner()
            }
        },
    )
}

/** A scrolling row of category tiles. The chosen tile is tinted and scrolled into view. */
@Composable
private fun CategoryTiles(
    categories: List<Category>,
    selectedId: String?,
    onSelect: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedId, categories.size) {
        val index = categories.indexOfFirst { it.id == selectedId }
        if (index >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == index }) {
            listState.animateScrollToItem(index)
        }
    }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm),
    ) {
        items(items = categories, key = { it.id }) { category ->
            CategoryTile(category = category, selected = category.id == selectedId, onClick = { onSelect(category.id) })
        }
    }
}

@Composable
private fun CategoryTile(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = TallyTheme.colors
    val tint = categoryColor(category.colorIndex)
    val shape = TallyTheme.shapes.card
    Column(
        modifier =
            Modifier
                .width(84.dp)
                .clip(shape)
                .background(if (selected) tint.copy(alpha = 0.16f) else colors.bg1)
                .border(if (selected) 1.5.dp else 1.dp, if (selected) tint else colors.lineSoft, shape)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TCatChip(name = category.name, color = tint, size = 30.dp)
        TText(
            text = category.name,
            style = TallyTheme.typography.caption,
            color = if (selected) colors.textHi else colors.textMid,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * "What was your share?" for an Eating out entry. Four quick choices, then the
 * resulting share beside the amount paid. Custom hands the keypad to the share.
 */
@Composable
private fun ShareCard(
    state: AddUiState,
    onChoice: (ShareChoice) -> Unit,
    onFieldSelected: (AmountField) -> Unit,
) {
    val colors = TallyTheme.colors
    val shareActive = state.activeField == AmountField.Share
    TCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(14.dp)) {
        TKicker("Your share")
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm),
        ) {
            ShareChoice.entries.forEach { choice ->
                ChoicePill(
                    label = choice.label,
                    selected = state.shareChoice == choice,
                    onClick = { onChoice(choice) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(
                modifier =
                    Modifier
                        .clip(TallyTheme.shapes.small)
                        .clickable(role = Role.Button, onClickLabel = "Type your share") {
                            onFieldSelected(AmountField.Share)
                        },
            ) {
                TKicker("Mine", color = if (shareActive) colors.accent else colors.textLo)
                TNum(
                    amount = state.myShare ?: state.amount,
                    size = TNumSize.Title,
                    color = if (shareActive) colors.accent else colors.textHi,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                TKicker("Paid")
                TNum(amount = state.amount, size = TNumSize.Medium, color = colors.textMid, modifier = Modifier.padding(top = 4.dp))
            }
        }
        TText(
            text =
                if (state.shareChoice == ShareChoice.Custom && state.customSharePence == null) {
                    "Type your share on the keypad."
                } else {
                    "Cash flow still counts the full amount paid."
                },
            style = TallyTheme.typography.caption,
            color = colors.textLo,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun ChoicePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    val shape = TallyTheme.shapes.pill
    Box(
        modifier =
            modifier
                .height(TallyTheme.spacing.minTouchTarget)
                .clip(shape)
                .background(if (selected) colors.accentWash else colors.bg2)
                .border(1.dp, if (selected) colors.accent else colors.lineSoft, shape)
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        TText(
            text = label,
            style = TallyTheme.typography.label,
            color = if (selected) colors.textHi else colors.textMid,
            maxLines = 1,
        )
    }
}

@Composable
private fun DateRow(
    label: String,
    onClick: () -> Unit,
) {
    val colors = TallyTheme.colors
    TCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        onClickLabel = "Change the date",
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = colors.textLo)
            TKicker("Date", modifier = Modifier.weight(1f))
            TText(text = label, style = TallyTheme.typography.bodyStrong)
        }
    }
}

@Composable
private fun TextAction(
    label: String,
    color: Color,
    onClick: () -> Unit,
) {
    TText(
        text = label,
        style = TallyTheme.typography.label,
        color = color,
        modifier =
            Modifier
                .clip(TallyTheme.shapes.small)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = TallyTheme.spacing.lg, vertical = TallyTheme.spacing.md),
    )
}
