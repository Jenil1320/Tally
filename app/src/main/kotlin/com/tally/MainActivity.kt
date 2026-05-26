package com.tally

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import dagger.hilt.android.AndroidEntryPoint

/**
 * Launcher activity. Owns the Compose root and applies privacy/UX guarantees
 * required by the constitution:
 *
 *  - `FLAG_SECURE` — task-switcher previews never expose balances (§12).
 *  - Edge-to-edge — Tally draws its own status/navigation bar backgrounds.
 *  - Single dark theme — no `DayNight` toggle (N1).
 *
 * The actual UI scaffold (5 tabs + FAB) lands in PR3. PR2 introduces the
 * Tally theme and atoms; this PR shows only a placeholder so the APK builds.
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
        setContent { TallyPlaceholderRoot() }
    }
}

/**
 * Placeholder root. Replaced in PR2 by `TallyTheme { TallyAppScaffold() }`.
 * Background is the closest sRGB approximation of the canvas's
 * `oklch(0.16 0.008 250)` page colour; PR2 reads it from the theme instead.
 */
@Composable
private fun TallyPlaceholderRoot() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF14181B),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFF14181B))
                    .wrapContentSize(),
        ) {
            Text(
                text = "Tally",
                color = Color(0xFFF9FAFA),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF14181B)
@Composable
private fun TallyPlaceholderRootPreview() {
    TallyPlaceholderRoot()
}
