package com.tally.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes (ARCHITECTURE §2). Each destination is a
 * `@Serializable` object so `navigate(...)` and `composable<...>` are checked by
 * the compiler — no string routes.
 *
 * The five [topLevelDestinations] are the bottom-bar tabs. The Add flow is a
 * `ModalBottomSheet` (see `TallyAppScaffold`), not a nav destination, and its
 * button lives on the Transactions screen only.
 */
@Serializable object Home

@Serializable object Transactions

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
        TopLevelDestination(Home, "Home", Icons.Outlined.Home),
        TopLevelDestination(Transactions, "Transactions", Icons.AutoMirrored.Outlined.ReceiptLong),
        TopLevelDestination(Insights, "Insights", Icons.Outlined.Insights),
        TopLevelDestination(Reports, "Reports", Icons.Outlined.CalendarMonth),
        TopLevelDestination(Settings, "Settings", Icons.Outlined.Settings),
    )
