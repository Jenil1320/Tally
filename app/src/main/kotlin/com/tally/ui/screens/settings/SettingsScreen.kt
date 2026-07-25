package com.tally.ui.screens.settings

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tally.data.notify.TallyNotificationListenerService
import com.tally.ui.components.TCard
import com.tally.ui.components.TCatDot
import com.tally.ui.components.TText
import com.tally.ui.components.TTextField
import com.tally.ui.theme.TallyTheme

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // The OS "Notification access" grant is not a runtime permission and can be
    // revoked outside the app, so re-check it whenever Settings resumes.
    var accessGranted by remember { mutableStateOf(isNotificationAccessGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    accessGranted = isNotificationAccessGranted(context)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // On Android 13+ the reminder needs POST_NOTIFICATIONS; ask only when the
    // user turns the reminder on (contextual permission per the constitution).
    val reminderPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.setReminderEnabled(true)
        }

    fun onReminderToggle(enabled: Boolean) {
        if (!enabled) {
            viewModel.setReminderEnabled(false)
            return
        }
        val needsPermission =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            reminderPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setReminderEnabled(true)
        }
    }

    // Capture also posts confirmation notifications, so ensure POST_NOTIFICATIONS
    // first; once that resolves, send the user to grant the separate, essential
    // "Notification access" (which lets Tally read Google Pay's notifications).
    val capturePermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (!accessGranted) openNotificationAccessSettings(context)
        }

    fun onCaptureToggle(enabled: Boolean) {
        viewModel.setCaptureEnabled(enabled)
        if (!enabled) return
        val needsPost =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        when {
            needsPost -> capturePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            !accessGranted -> openNotificationAccessSettings(context)
        }
    }

    SettingsContent(
        state = state,
        accessGranted = accessGranted,
        onReminderToggle = ::onReminderToggle,
        onCaptureToggle = ::onCaptureToggle,
        onOpenAccessSettings = { openNotificationAccessSettings(context) },
        onLimitChange = viewModel::setCategoryLimit,
        modifier = modifier,
    )
}

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    accessGranted: Boolean,
    onReminderToggle: (Boolean) -> Unit,
    onCaptureToggle: (Boolean) -> Unit,
    onOpenAccessSettings: () -> Unit,
    onLimitChange: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TallyTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.bg0)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = TallyTheme.spacing.lg,
                    end = TallyTheme.spacing.lg,
                    top = TallyTheme.spacing.lg,
                    bottom = 96.dp,
                ),
        verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md),
    ) {
        TText(text = "Settings", style = TallyTheme.typography.headline)

        TCard {
            SettingRow(
                title = "Daily reminder",
                caption = "A nudge at ${state.reminderTime} to log the day.",
                checked = state.reminderEnabled,
                onCheckedChange = onReminderToggle,
            )
        }

        TCard {
            val needsGrant = state.captureEnabled && !accessGranted
            val caption =
                when {
                    !state.captureEnabled -> "Log spending automatically from Google Pay notifications."
                    needsGrant -> "Grant Spendwise ‘Notification access’ to start capturing. Tap to open settings."
                    else -> "On. New payments log automatically; edit or remove them like any entry."
                }
            SettingRow(
                title = "Capture from Google Pay",
                caption = caption,
                checked = state.captureEnabled,
                onCheckedChange = onCaptureToggle,
                onCaptionClick = if (needsGrant) onOpenAccessSettings else null,
            )
        }

        if (state.limitRows.isNotEmpty()) {
            TCard {
                TText(text = "Monthly limits")
                TText(
                    text = "A limit you set for a category. Insights then shows what is left, per day.",
                    style = TallyTheme.typography.caption,
                    color = colors.textLo,
                    modifier = Modifier.padding(top = 2.dp, bottom = TallyTheme.spacing.sm),
                )
                state.limitRows.forEach { row ->
                    LimitEditorRow(row = row, onLimitChange = onLimitChange)
                }
            }
        }

        TText(
            text = "Export and biometric lock arrive in later milestones.",
            style = TallyTheme.typography.caption,
            color = colors.textDim,
        )
    }
}

/**
 * One category's limit field. Holds the typed text locally so a partially typed
 * amount is never written, and commits on every valid edit (blank clears).
 */
@Composable
private fun LimitEditorRow(
    row: CategoryLimitRow,
    onLimitChange: (String, String) -> Unit,
) {
    val colors = TallyTheme.colors
    var text by remember(row.categoryId, row.limitText) { mutableStateOf(row.limitText) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = TallyTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm),
    ) {
        TCatDot(color = colors.category(row.colorIndex))
        TText(text = row.name, modifier = Modifier.weight(1f), maxLines = 1)
        TTextField(
            value = text,
            onValueChange = {
                text = it
                onLimitChange(row.categoryId, it)
            },
            placeholder = "None",
            keyboardType = KeyboardType.Decimal,
            modifier = Modifier.width(104.dp),
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    caption: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onCaptionClick: (() -> Unit)? = null,
) {
    val colors = TallyTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            TText(text = title)
            TText(
                text = caption,
                style = TallyTheme.typography.caption,
                color = colors.textLo,
                modifier = if (onCaptionClick != null) Modifier.clickable { onCaptionClick() } else Modifier,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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

/** True when Tally holds the OS "Notification access" grant. */
private fun isNotificationAccessGranted(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

/**
 * Opens the system "Notification access" screen. On API 30+ deep-links straight
 * to Tally's own toggle; below that (and on any failure) falls back to the full
 * listener list.
 */
private fun openNotificationAccessSettings(context: Context) {
    val fallback = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
    val intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val component = ComponentName(context, TallyNotificationListenerService::class.java)
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
        } else {
            fallback
        }
    runCatching { context.startActivity(intent) }
        .onFailure { runCatching { context.startActivity(fallback) } }
}
