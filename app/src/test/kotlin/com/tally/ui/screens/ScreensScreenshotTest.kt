package com.tally.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.tally.core.money.Pence
import com.tally.domain.insight.CategorySpend
import com.tally.domain.insight.EatingOutSummary
import com.tally.ui.components.TBarItem
import com.tally.ui.components.TBottomBar
import com.tally.ui.components.TransactionRowUi
import com.tally.ui.screens.home.HomeContent
import com.tally.ui.screens.home.HomeUiState
import com.tally.ui.screens.transactions.TransactionDay
import com.tally.ui.screens.transactions.TransactionsContent
import com.tally.ui.screens.transactions.TransactionsUiState
import com.tally.ui.theme.TallyTheme
import org.junit.Rule
import org.junit.Test

/**
 * Whole-screen goldens on a Pixel 6, with the bottom bar so the layout reads as
 * the user sees it. They render the stateless content composables with sample
 * data, so no ViewModel or database is needed. Look at the PNGs when the design
 * changes; a diff here is a visible change to a screen.
 */
class ScreensScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    @Test
    fun home() {
        paparazzi.snapshot { Frame(selected = 0) { HomeContent(state = sampleHome, onTransactionClick = {}, onOpenTransactions = {}) } }
    }

    @Test
    fun homeEmpty() {
        paparazzi.snapshot {
            Frame(selected = 0) {
                HomeContent(state = HomeUiState(loaded = true), onTransactionClick = {}, onOpenTransactions = {})
            }
        }
    }

    @Test
    fun transactions() {
        paparazzi.snapshot {
            Frame(selected = 1) {
                TransactionsContent(state = sampleTransactions, onTransactionClick = {}, onAddClick = {})
            }
        }
    }

    @Test
    fun transactionsEmpty() {
        paparazzi.snapshot {
            Frame(selected = 1) {
                TransactionsContent(state = TransactionsUiState(loaded = true), onTransactionClick = {}, onAddClick = {})
            }
        }
    }
}

@Composable
internal fun Frame(
    selected: Int,
    content: @Composable () -> Unit,
) {
    TallyTheme(reduceMotion = true) {
        Column(modifier = Modifier.fillMaxSize().background(TallyTheme.colors.bg0).statusBarsPadding()) {
            Box(modifier = Modifier.weight(1f)) { content() }
            TBottomBar(
                items =
                    listOf(
                        TBarItem("Home", Icons.Outlined.Home),
                        TBarItem("Transactions", Icons.AutoMirrored.Outlined.ReceiptLong),
                        TBarItem("Insights", Icons.Outlined.Insights),
                        TBarItem("Reports", Icons.Outlined.CalendarMonth),
                        TBarItem("Settings", Icons.Outlined.Settings),
                    ),
                selectedIndex = selected,
                onSelect = {},
            )
        }
    }
}

private val sampleBreakdown =
    listOf(
        CategorySpend("rent", "Rent", 9, Pence(145_000), 0.55f),
        CategorySpend("gro", "Groceries", 0, Pence(31_200), 0.20f),
        CategorySpend("eat", "Eating out", 1, Pence(26_800), 0.15f),
        CategorySpend("trn", "Transport", 2, Pence(14_200), 0.07f),
        CategorySpend("ent", "Entertainment", 6, Pence(9_600), 0.03f),
    )

private val sampleRecent =
    listOf(
        TransactionRowUi("1", "Dishoom Shoreditch", Pence(3_840), "Eating out", 1, "Eating out · Today, 21:08", share = Pence(1_920)),
        TransactionRowUi("2", "Sainsbury's Local", Pence(1_265), "Groceries", 0, "Groceries · Today, 18:42"),
        TransactionRowUi("3", "TfL contactless", Pence(640), "Transport", 2, "Transport · Today, 08:51"),
        TransactionRowUi("4", "Fabric", Pence(2_750), "Entertainment", 6, "Entertainment · Yesterday, 23:10"),
    )

private val sampleHome =
    HomeUiState(
        dateKicker = "Tuesday · 6 October",
        monthName = "October",
        lastMonthName = "September",
        monthTotal = Pence(260_046),
        deltaPercent = -11,
        daysLeft = 25,
        breakdown = sampleBreakdown,
        spentToday = Pence(5_745),
        todayCount = 3,
        forecast = Pence(214_000),
        forecastGap = Pence(-46_000),
        eatingOut = EatingOutSummary(paid = Pence(26_800), mine = Pence(11_400), count = 6, sharedCount = 4),
        recent = sampleRecent,
        hasAnyTransactions = true,
        loaded = true,
    )

private val sampleTransactions =
    TransactionsUiState(
        days =
            listOf(
                TransactionDay("2026-10-06", "Today", Pence(7_745), sampleRecent.take(3)),
                TransactionDay("2026-10-05", "Yesterday", Pence(2_750), sampleRecent.drop(3)),
                TransactionDay("2026-10-04", "4 Oct", Pence(900), listOf(sampleRecent[1].copy(id = "5"))),
            ),
        loaded = true,
    )
