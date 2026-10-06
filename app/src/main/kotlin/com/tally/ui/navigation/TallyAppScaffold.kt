package com.tally.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tally.ui.components.TBarItem
import com.tally.ui.components.TBottomBar
import com.tally.ui.screens.add.AddSheet
import com.tally.ui.screens.home.HomeScreen
import com.tally.ui.screens.insights.InsightsScreen
import com.tally.ui.screens.reports.ReportsScreen
import com.tally.ui.screens.settings.SettingsScreen
import com.tally.ui.screens.transactions.TransactionsScreen
import com.tally.ui.theme.LocalReduceMotion
import com.tally.ui.theme.TallyMotion
import com.tally.ui.theme.TallyTheme
import kotlinx.coroutines.launch

/**
 * The app shell: a bottom bar of the five [topLevelDestinations], the type-safe
 * [NavHost], and the Add [ModalBottomSheet]. Tab clicks single-top and restore
 * state so re-tapping a tab returns to where the user was.
 *
 * There is deliberately no global add button. The Transactions screen owns it
 * and asks this shell to open the sheet; Home and Transactions rows open the
 * same sheet to edit an entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TallyAppScaffold(
    navController: NavHostController = rememberNavController(),
    openEditTransactionId: String? = null,
    onEditConsumed: () -> Unit = {},
) {
    val colors = TallyTheme.colors
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    var sheetOpen by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reduceMotion = LocalReduceMotion.current

    val scope = rememberCoroutineScope()

    fun openSheet(transactionId: String?) {
        editingId = transactionId
        sheetOpen = true
    }

    // Slide the sheet away before removing it, so saving never snaps shut.
    fun closeSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { sheetOpen = false }
    }

    fun openTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // A capture-notification tap deep-links here to edit that transaction.
    LaunchedEffect(openEditTransactionId) {
        if (openEditTransactionId != null) {
            openSheet(openEditTransactionId)
            onEditConsumed()
        }
    }

    val selectedIndex =
        topLevelDestinations.indexOfFirst { destination ->
            currentDestination?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
        }

    Scaffold(
        containerColor = colors.bg0,
        bottomBar = {
            TBottomBar(
                items = topLevelDestinations.map { TBarItem(it.label, it.icon) },
                selectedIndex = selectedIndex,
                onSelect = { openTab(topLevelDestinations[it].route) },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Home,
            modifier = Modifier.padding(innerPadding),
            // Tabs cross-fade: the old screen leaves quickly, the new one eases in.
            enterTransition = {
                if (reduceMotion) EnterTransition.None else fadeIn(TallyMotion.standard())
            },
            exitTransition = {
                if (reduceMotion) ExitTransition.None else fadeOut(tween(90))
            },
            popEnterTransition = {
                if (reduceMotion) EnterTransition.None else fadeIn(TallyMotion.standard())
            },
            popExitTransition = {
                if (reduceMotion) ExitTransition.None else fadeOut(tween(90))
            },
        ) {
            composable<Home> {
                HomeScreen(
                    onTransactionClick = { openSheet(it) },
                    onOpenTransactions = { openTab(Transactions) },
                )
            }
            composable<Transactions> {
                TransactionsScreen(
                    onTransactionClick = { openSheet(it) },
                    onAddClick = { openSheet(null) },
                )
            }
            composable<Insights> { InsightsScreen() }
            composable<Reports> { ReportsScreen() }
            composable<Settings> { SettingsScreen() }
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { sheetOpen = false },
            sheetState = sheetState,
            containerColor = colors.bg0,
            shape = TallyTheme.shapes.sheet,
        ) {
            AddSheet(editingId = editingId, onDismiss = ::closeSheet)
        }
    }
}
