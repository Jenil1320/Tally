package com.tally.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tally.ui.components.TText
import com.tally.ui.screens.add.AddScreen
import com.tally.ui.screens.history.HistoryScreen
import com.tally.ui.screens.insights.InsightsScreen
import com.tally.ui.screens.reports.ReportsScreen
import com.tally.ui.screens.settings.SettingsScreen
import com.tally.ui.screens.today.TodayScreen
import com.tally.ui.theme.TallyTheme

/**
 * The app shell: a bottom [NavigationBar] of the five [topLevelDestinations], a
 * centre Add FAB, and the type-safe [NavHost]. Tab clicks single-top and restore
 * state so re-tapping a tab returns to where the user was.
 */
@Composable
fun TallyAppScaffold(navController: NavHostController = rememberNavController()) {
    val colors = TallyTheme.colors
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        containerColor = colors.bg0,
        bottomBar = {
            NavigationBar(containerColor = colors.bg1) {
                topLevelDestinations.forEach { destination ->
                    val selected =
                        currentDestination?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { TText(destination.label, style = TallyTheme.typography.caption) },
                        colors =
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.onAccent,
                                indicatorColor = colors.accent,
                                selectedTextColor = colors.textHi,
                                unselectedIconColor = colors.textLo,
                                unselectedTextColor = colors.textLo,
                            ),
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Add) },
                containerColor = colors.accent,
                contentColor = colors.onAccent,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add transaction")
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Today,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<Today> { TodayScreen() }
            composable<History> { HistoryScreen() }
            composable<Insights> { InsightsScreen() }
            composable<Reports> { ReportsScreen() }
            composable<Settings> { SettingsScreen() }
            composable<Add> { AddScreen(onClose = { navController.popBackStack() }) }
        }
    }
}
