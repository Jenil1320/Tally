package com.tally.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tally.ui.theme.TallyMotion
import com.tally.ui.theme.TallyTheme

/** One destination in the [TBottomBar]. */
data class TBarItem(
    val label: String,
    val icon: ImageVector,
)

/**
 * The bottom navigation. Icon over label, quiet until selected: the selected
 * item takes the accent colour and the rest stay low. There is no indicator
 * pill and no add button here — adding a transaction lives on the Transactions
 * screen only. The colour change eases over [TallyMotion.STANDARD_MS].
 */
@Composable
fun TBottomBar(
    items: List<TBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.bg0)
                .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.lineSoft))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = TallyTheme.spacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            items.forEachIndexed { index, item ->
                BarItem(
                    item = item,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BarItem(
    item: TBarItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    val tint by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.textLo,
        animationSpec = TallyMotion.standard(),
        label = "barTint",
    )
    Column(
        modifier =
            modifier
                .heightIn(min = 60.dp)
                .selectable(
                    selected = selected,
                    onClick = onClick,
                    role = Role.Tab,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = 36.dp),
                )
                .padding(vertical = TallyTheme.spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(imageVector = item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        TText(
            text = item.label,
            style = TallyTheme.typography.caption.copy(fontSize = 11.sp),
            color = tint,
            modifier = Modifier.padding(top = 3.dp),
            maxLines = 1,
        )
    }
}
