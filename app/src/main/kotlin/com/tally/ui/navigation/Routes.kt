package com.tally.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes (ARCHITECTURE §2). Each destination is a
 * `@Serializable` object so `navigate(...)` and `composable<...>` are checked by
 * the compiler — no string routes.
 *
 * The five [topLevelDestinations] are the bottom-bar tabs; the Add flow is a
 * `ModalBottomSheet` (see `TallyAppScaffold`), not a nav destination.
 *
 * NOTE: the tab set is inferred from the documented feature roadmap and is the
 * obvious thing to revisit with the PM — renaming a tab is a one-line change.
 */
@Serializable object Today

@Serializable object History

@Serializable object Insights

@Serializable object Reports

@Serializable object Settings

/** A bottom-bar tab: its route, label, and icon. */
data class TopLevelDestination(
    val route: Any,
    val label: String,
    val icon: ImageVector,
)

val topLevelDestinations: List<TopLevelDestination> =
    listOf(
        TopLevelDestination(Today, "Today", Icons.Outlined.Today),
        TopLevelDestination(History, "History", Icons.AutoMirrored.Outlined.ReceiptLong),
        TopLevelDestination(Insights, "Insights", Icons.Outlined.Insights),
        TopLevelDestination(Reports, "Reports", Icons.Outlined.CalendarMonth),
        TopLevelDestination(Settings, "Settings", Icons.Outlined.Settings),
    )
