package com.tally.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tally.ui.theme.TallyTheme

/**
 * The top of a screen: an optional [kicker] above a large [title], with room for
 * one quiet [trailing] element on the right (a month switcher, a count).
 */
@Composable
fun TScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (kicker != null) TKicker(kicker)
            TText(
                text = title,
                style = TallyTheme.typography.headline,
                modifier = Modifier.padding(top = if (kicker != null) 2.dp else 0.dp),
            )
        }
        trailing?.invoke()
    }
}

/**
 * A title for a group of cards, with an optional text action on the right
 * ("See all"). The action keeps a 44 dp tall hit area.
 */
@Composable
fun TSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TText(text = title, style = TallyTheme.typography.bodyStrong, modifier = Modifier.padding(horizontal = 4.dp))
        if (action != null && onAction != null) {
            TText(
                text = action,
                style = TallyTheme.typography.numericSmall,
                color = TallyTheme.colors.textMid,
                modifier =
                    Modifier
                        .heightIn(min = TallyTheme.spacing.minTouchTarget)
                        .clickable(role = Role.Button, onClick = onAction)
                        .padding(horizontal = 4.dp)
                        .padding(vertical = 12.dp),
            )
        }
    }
}
