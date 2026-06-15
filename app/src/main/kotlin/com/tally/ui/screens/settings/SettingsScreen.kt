package com.tally.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.ui.components.TCard
import com.tally.ui.components.TText
import com.tally.ui.theme.TallyTheme

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // On Android 13+ the reminder needs POST_NOTIFICATIONS; ask only when the
    // user turns the reminder on (contextual permission per the constitution).
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.setReminderEnabled(true)
        }

    fun onToggle(enabled: Boolean) {
        if (!enabled) {
            viewModel.setReminderEnabled(false)
            return
        }
        val needsPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setReminderEnabled(true)
        }
    }

    SettingsContent(state = state, onReminderToggle = ::onToggle, modifier = modifier)
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onReminderToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    Column(
        modifier = modifier.fillMaxSize().background(colors.bg0).padding(TallyTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md),
    ) {
        TText(text = "Settings", style = TallyTheme.typography.headline)

        TCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    TText(text = "Daily reminder")
                    TText(
                        text = "A nudge at ${state.reminderTime} to log the day.",
                        style = TallyTheme.typography.caption,
                        color = colors.textLo,
                    )
                }
                Switch(
                    checked = state.reminderEnabled,
                    onCheckedChange = onReminderToggle,
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = colors.onAccent,
                            checkedTrackColor = colors.accent,
                            uncheckedThumbColor = colors.textLo,
                            uncheckedTrackColor = colors.bg3,
                        ),
                )
            }
        }

        TText(
            text = "SMS sources, export, and biometric lock arrive in later milestones.",
            style = TallyTheme.typography.caption,
            color = colors.textDim,
        )
    }
}
