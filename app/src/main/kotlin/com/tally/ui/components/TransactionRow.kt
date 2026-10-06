package com.tally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tally.core.money.Pence
import com.tally.ui.theme.TallyTheme

/**
 * View state for one transaction row, resolved by the ViewModel (the category
 * is already joined, so the row stays a dumb renderer).
 *
 * @property amount the full amount paid. This is what cash flow counts.
 * @property share the user's own share, only when it is smaller than [amount]
 *   (a shared meal). Shown as a footnote under the amount; null otherwise.
 */
data class TransactionRowUi(
    val id: String,
    val merchant: String,
    val amount: Pence,
    val categoryName: String?,
    val colorIndex: Int?,
    val subtitle: String,
    val share: Pence? = null,
)

/**
 * A single transaction line: category chip, merchant with a quiet subtitle, and
 * the amount (always through `TNum`). A shared meal adds a "your share"
 * footnote under the amount. Place rows inside one `TCard` per group; set
 * [showDivider] on every row except the last to draw the hairline between them.
 */
@Composable
fun TTransactionRow(
    item: TransactionRowUi,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = false,
) {
    val colors = TallyTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(),
                                role = Role.Button,
                                onClickLabel = "Open ${item.merchant}",
                                onClick = onClick,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TCatChip(name = item.categoryName, color = categoryColor(item.colorIndex))
            Column(modifier = Modifier.weight(1f)) {
                TText(
                    text = item.merchant,
                    style = TallyTheme.typography.bodyStrong,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TText(
                    text = item.subtitle,
                    style = TallyTheme.typography.caption,
                    color = colors.textLo,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                TNum(amount = item.amount)
                item.share?.let { share ->
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TText(text = "your share", style = TallyTheme.typography.caption, color = colors.textLo)
                        TNum(amount = share, size = TNumSize.Small, color = colors.accent)
                    }
                }
            }
        }
        if (showDivider) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 64.dp)
                        .height(1.dp)
                        .background(colors.lineSoft),
            )
        }
    }
}
