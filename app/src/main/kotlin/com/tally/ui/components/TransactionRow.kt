package com.tally.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.tally.core.money.Pence
import com.tally.ui.theme.TallyTheme

/**
 * View state for one transaction row, resolved by the ViewModel (the category
 * is already joined, so the row stays a dumb renderer).
 */
data class TransactionRowUi(
    val id: String,
    val merchant: String,
    val amount: Pence,
    val categoryName: String?,
    val colorIndex: Int?,
    val subtitle: String,
)

/**
 * A single transaction line: category dot, merchant + subtitle, and the amount
 * (always through `TNum`). Tappable for edit/delete in a later PR.
 */
@Composable
fun TTransactionRow(
    item: TransactionRowUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = TallyTheme.colors
    val dotColor = item.colorIndex?.let { colors.category(it) } ?: colors.textDim
    TCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        onClickLabel = "Open ${item.merchant}",
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md),
        ) {
            TCatDot(color = dotColor)
            Column(modifier = Modifier.weight(1f)) {
                TText(text = item.merchant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                TText(
                    text = item.subtitle,
                    style = TallyTheme.typography.caption,
                    color = colors.textLo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TNum(amount = item.amount)
        }
    }
}
