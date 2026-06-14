package com.tally

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tally.core.money.Pence
import com.tally.ui.components.TCard
import com.tally.ui.components.TNum
import com.tally.ui.components.TNumSize
import com.tally.ui.components.TText
import com.tally.ui.theme.TallyTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Launcher activity. Owns the Compose root and applies privacy/UX guarantees
 * required by the constitution:
 *
 *  - `FLAG_SECURE` — task-switcher previews never expose balances (§12).
 *  - Edge-to-edge — Tally draws its own status/navigation bar backgrounds.
 *  - Single dark theme — no `DayNight` toggle (N1).
 *
 * PR2 introduces the Tally theme and atoms; this root now renders through
 * `TallyTheme`. The 5-tab + FAB scaffold replaces the placeholder in PR3.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        enableEdgeToEdge()
        setContent {
            TallyTheme {
                TallyPlaceholderRoot()
            }
        }
    }
}

/**
 * Themed placeholder. Exercises the PR2 design system (theme tokens + atoms) so
 * the APK shows the real palette and type instead of a hard-coded hex. Replaced
 * by `TallyAppScaffold` (5 tabs + FAB) in PR3.
 */
@Composable
private fun TallyPlaceholderRoot() {
    val colors = TallyTheme.colors
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.bg0)
                .padding(TallyTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TText(text = "Tally", style = TallyTheme.typography.headline)
        TCard(border = true) {
            TText(
                text = "Spent today",
                style = TallyTheme.typography.caption,
                color = colors.textLo,
            )
            TNum(amount = Pence(0), size = TNumSize.Large)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14181B)
@Composable
private fun TallyPlaceholderRootPreview() {
    TallyTheme {
        TallyPlaceholderRoot()
    }
}
