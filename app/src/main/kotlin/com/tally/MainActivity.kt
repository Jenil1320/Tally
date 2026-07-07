package com.tally

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.tally.ui.navigation.TallyAppScaffold
import com.tally.ui.theme.TallyTheme
import com.tally.work.CaptureNotifier
import dagger.hilt.android.AndroidEntryPoint

/**
 * Launcher activity. Owns the Compose root and applies privacy/UX guarantees
 * required by the constitution:
 *
 *  - `FLAG_SECURE` — task-switcher previews never expose balances (§12).
 *  - Edge-to-edge — Tally draws its own status/navigation bar backgrounds.
 *  - Single dark theme — no `DayNight` toggle (N1).
 *
 * Also honours the deep link from a capture notification: tapping one carries
 * [CaptureNotifier.EXTRA_EDIT_TRANSACTION_ID], which opens that transaction in
 * the edit sheet (works on cold start and via [onNewIntent] when already open —
 * the activity is `singleTop`).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val editTransactionId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE,
        )
        editTransactionId.value = intent.editTransactionId()
        enableEdgeToEdge()
        setContent {
            val editId by editTransactionId
            TallyTheme {
                TallyAppScaffold(
                    openEditTransactionId = editId,
                    onEditConsumed = { editTransactionId.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        editTransactionId.value = intent.editTransactionId()
    }

    private fun Intent.editTransactionId(): String? = getStringExtra(CaptureNotifier.EXTRA_EDIT_TRANSACTION_ID)
}
