package com.tally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.tally.core.money.Pence
import com.tally.domain.insight.CategorySpend
import com.tally.domain.insight.EatingOutSummary
import com.tally.ui.theme.TallyTheme
import org.junit.Rule
import org.junit.Test

/**
 * Goldens for the design-system atoms added with the Tally.html redesign: the
 * hero figure, kicker, pill, category chip, bottom bar, add button and the shared
 * breakdown and eating-out cards. Look at the recorded PNGs once before
 * trusting them.
 */
class DesignSystemScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    @Test
    fun designSystemGallery() {
        paparazzi.snapshot { Gallery { AtomsAndBreakdown() } }
    }

    @Test
    fun designSystemGalleryCards() {
        paparazzi.snapshot { Gallery { SummaryAndNavigation() } }
    }
}

private val sampleBreakdown =
    listOf(
        CategorySpend("rent", "Rent", 9, Pence(145_000), 0.55f),
        CategorySpend("gro", "Groceries", 0, Pence(31_200), 0.20f),
        CategorySpend("eat", "Eating out", 1, Pence(26_800), 0.15f),
        CategorySpend("trn", "Transport", 2, Pence(14_200), 0.10f),
    )

@Composable
private fun Gallery(content: @Composable () -> Unit) {
    TallyTheme(reduceMotion = true) {
        Column(
            modifier = Modifier.fillMaxSize().background(TallyTheme.colors.bg0).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) { content() }
    }
}

@Composable
private fun AtomsAndBreakdown() {
    val colors = TallyTheme.colors
    TKicker("Spent this month")
    TNum(amount = Pence(260_046), size = TNumSize.Hero)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TPill(text = "-11% vs last month", color = colors.positive, showDot = true)
        TPill(text = "+8% so far", color = colors.warning)
        TPill(text = "Plain", filled = false)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("Groceries", "Eating out", "Transport", "Shopping", "Bills", "Other").forEachIndexed { index, name ->
            TCatChip(name = name, color = colors.category(index))
        }
        TCatChip(name = null, color = colors.textDim)
    }
    TCard {
        TKicker("Today")
        TNum(amount = Pence(5_705), size = TNumSize.Large)
    }
    CategoryBreakdownCard(breakdown = sampleBreakdown, total = Pence(217_200))
}

@Composable
private fun SummaryAndNavigation() {
    CategorySummaryCard(breakdown = sampleBreakdown + sampleBreakdown.take(2), maxRows = 4)
    EatingOutCard(summary = EatingOutSummary(paid = Pence(26_800), mine = Pence(11_400), count = 6, sharedCount = 4))
    EatingOutNote(summary = EatingOutSummary(paid = Pence(26_800), mine = Pence(11_400), count = 6, sharedCount = 4))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TFab(onClick = {}, contentDescription = "Add transaction")
    }
    TBottomBar(
        items =
            listOf(
                TBarItem("Home", Icons.Outlined.Home),
                TBarItem("Transactions", Icons.AutoMirrored.Outlined.ReceiptLong),
                TBarItem("Insights", Icons.Outlined.Insights),
                TBarItem("Reports", Icons.Outlined.CalendarMonth),
                TBarItem("Settings", Icons.Outlined.Settings),
            ),
        selectedIndex = 0,
        onSelect = {},
    )
}
