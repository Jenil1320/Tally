package com.tally

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tally.ui.navigation.TallyAppScaffold
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
 * PR3 wires the real app shell: `TallyTheme { TallyAppScaffold() }` (5 tabs +
 * Add FAB + type-safe NavHost). Biometric gating on cold start lands in M5.
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
                TallyAppScaffold()
            }
        }
    }
}
