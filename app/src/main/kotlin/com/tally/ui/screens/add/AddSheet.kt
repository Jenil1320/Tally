package com.tally.ui.screens.add

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.domain.model.Category
import com.tally.ui.components.TButton
import com.tally.ui.components.TCatDot
import com.tally.ui.components.TKeypad
import com.tally.ui.components.TNum
import com.tally.ui.components.TNumSize
import com.tally.ui.components.TText
import com.tally.ui.components.TTextField
import com.tally.ui.theme.TallyTheme

/**
 * Add/Edit sheet content. Opened fresh for a new entry or with [editingId] to
 * edit. Amount (keypad), merchant, auto-suggested category, optional note, and
 * an editable date. Editing reveals Duplicate and Delete.
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
    val colors = TallyTheme.colors
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = TallyTheme.spacing.xl)
                .padding(bottom = TallyTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TText(
            text = if (state.isEditing) "Edit transaction" else "Add transaction",
            style = TallyTheme.typography.label,
            color = colors.textMid,
        )

        TNum(amount = state.amount, size = TNumSize.Large)

        TTextField(
            value = state.merchant,
            onValueChange = viewModel::onMerchantChange,
            placeholder = "Merchant",
            modifier = Modifier.fillMaxWidth(),
        )

        CategoryChips(
            categories = state.categories,
            selectedId = state.selectedCategoryId,
            onSelect = viewModel::onCategorySelected,
        )

        DateField(label = state.occurredAtLabel, onClick = { showDatePicker = true })

        TTextField(
            value = state.note,
            onValueChange = viewModel::onNoteChange,
            placeholder = "Note (optional)",
            keyboardType = KeyboardType.Text,
            modifier = Modifier.fillMaxWidth(),
        )

        TKeypad(
            onDigit = viewModel::onDigit,
            onDoubleZero = viewModel::onDoubleZero,
            onBackspace = viewModel::onBackspace,
        )

        TButton(
            text = if (state.isEditing) "Save changes" else "Save",
            onClick = { viewModel.save(onSaved = onDismiss) },
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.isEditing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                TextAction(label = "Duplicate", color = colors.textMid) { viewModel.duplicate(onDone = onDismiss) }
                TextAction(label = "Delete", color = colors.danger) { viewModel.delete(onDone = onDismiss) }
            }
        }
    }

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
private fun DateField(
    label: String,
    onClick: () -> Unit,
) {
    val colors = TallyTheme.colors
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(TallyTheme.shapes.small)
                .background(colors.bg3)
                .clickable(onClick = onClick)
                .heightIn(min = TallyTheme.spacing.minTouchTarget)
                .padding(horizontal = TallyTheme.spacing.md, vertical = TallyTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md),
    ) {
        Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = colors.textLo)
        TText(text = "Date", color = colors.textLo, modifier = Modifier.weight(1f))
        TText(text = label, color = colors.textHi)
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
                .clickable(onClick = onClick)
                .padding(horizontal = TallyTheme.spacing.lg, vertical = TallyTheme.spacing.md),
    )
}

@Composable
private fun CategoryChips(
    categories: List<Category>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        categories.forEach { category ->
            val selected = category.id == selectedId
            Row(
                modifier =
                    Modifier
                        .clip(TallyTheme.shapes.pill)
                        .background(if (selected) colors.accentWash else colors.bg3)
                        .border(
                            width = 1.dp,
                            color = if (selected) colors.accent else colors.line,
                            shape = TallyTheme.shapes.pill,
                        )
                        .clickable { onSelect(category.id) }
                        .padding(horizontal = TallyTheme.spacing.md, vertical = TallyTheme.spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TCatDot(color = colors.category(category.colorIndex))
                TText(
                    text = category.name,
                    style = TallyTheme.typography.label,
                    color = if (selected) colors.textHi else colors.textMid,
                )
            }
        }
    }
}
