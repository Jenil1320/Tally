package com.tally.ui.screens

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.tally.core.money.Pence
import com.tally.domain.insight.CategorySpend
import com.tally.domain.insight.EatingOutMonth
import com.tally.domain.insight.EatingOutSummary
import com.tally.domain.insight.LimitState
import com.tally.domain.insight.LimitStatus
import com.tally.domain.insight.MerchantTotal
import com.tally.domain.insight.MonthTotal
import com.tally.domain.insight.UnusualSpend
import com.tally.domain.insight.weekRhythm
import com.tally.domain.report.DaySpend
import com.tally.domain.report.MerchantSpend
import com.tally.domain.report.MonthDelta
import com.tally.domain.report.MonthlyReport
import com.tally.ui.screens.insights.CategoryChip
import com.tally.ui.screens.insights.InsightsContent
import com.tally.ui.screens.insights.InsightsUiState
import com.tally.ui.screens.reports.ReportsContent
import com.tally.ui.screens.reports.ReportsUiState
import com.tally.ui.screens.settings.CategoryLimitRow
import com.tally.ui.screens.settings.SettingsContent
import com.tally.ui.screens.settings.SettingsUiState
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test

/**
 * Goldens for Insights, Reports and Settings on a tall Pixel 6, so a whole
 * scroll of each screen is visible in one frame. They use sample data and the
 * stateless content composables, so no ViewModel is involved.
 */
class InsightsReportsSettingsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6.copy(screenHeight = 3400))

    private val breakdown =
        listOf(
            CategorySpend("rent", "Rent", 9, Pence(145_000), 0.55f),
            CategorySpend("gro", "Groceries", 0, Pence(31_200), 0.20f),
            CategorySpend("eat", "Eating out", 1, Pence(26_800), 0.15f),
            CategorySpend("trn", "Transport", 2, Pence(14_200), 0.07f),
            CategorySpend("ent", "Entertainment", 6, Pence(9_600), 0.03f),
        )

    private val eatingOut = EatingOutSummary(paid = Pence(26_800), mine = Pence(11_400), count = 6, sharedCount = 4)

    @Test
    fun insights() {
        val daily = listOf(42, 18, 6, 88, 24, 0, 12, 56, 31, 22, 18, 9, 0, 64, 102, 28, 14, 8, 36, 71, 19, 0, 12, 48, 22, 17, 33, 56, 38, 0)
        val state = sampleInsights()
        paparazzi.snapshot { Frame(selected = 2) { InsightsContent(state = state, onToggleCategory = {}) } }
    }

    @Test
    fun reports() {
        val report =
            MonthlyReport(
                monthLabel = "September 2026",
                total = Pence(260_046),
                transactionCount = 112,
                byCategory = breakdown,
                topMerchant = MerchantSpend("Sainsbury's", Pence(28_600), 11),
                priciestDay = DaySpend(LocalDate(2026, 9, 19), Pence(15_600)),
                avgPerDay = Pence(8_668),
                autoCapturedPercent = 62,
                vsPrevious = MonthDelta(Pence(292_000), -11),
                eatingOut = eatingOut,
            )
        val state =
            ReportsUiState(
                loaded = true,
                report = report,
                previousMonthLabel = "August 2026",
                observation = "Rent took the biggest share. 62% of entries logged themselves.",
                canGoForward = true,
            )
        paparazzi.snapshot { Frame(selected = 3) { ReportsContent(state = state, onPrev = {}, onNext = {}) } }
    }

    @Test
    fun settings() {
        val state =
            SettingsUiState(
                reminderEnabled = true,
                reminderTime = "22:00",
                captureEnabled = true,
                limitRows =
                    listOf(
                        CategoryLimitRow("gro", "Groceries", 0, "250"),
                        CategoryLimitRow("eat", "Eating out", 1, "300"),
                        CategoryLimitRow("trn", "Transport", 2, ""),
                        CategoryLimitRow("shp", "Shopping", 3, ""),
                    ),
                loaded = true,
            )
        paparazzi.snapshot {
            Frame(selected = 4) {
                SettingsContent(
                    state = state,
                    accessGranted = true,
                    onReminderToggle = {},
                    onCaptureToggle = {},
                    onOpenAccessSettings = {},
                    onLimitChange = { _, _ -> },
                )
            }
        }
    }
}

private fun sampleInsights(): InsightsUiState {
    val daily = listOf(42, 18, 6, 88, 24, 0, 12, 56, 31, 22, 18, 9, 0, 64, 102, 28, 14, 8, 36, 71, 19, 0, 12, 48, 22, 17, 33, 56, 38, 0)
    val months =
        listOf("May" to 2_430_00L, "Jun" to 2_610_00L, "Jul" to 2_280_00L, "Aug" to 2_950_00L, "Sep" to 2_600_46L, "Oct" to 1_130_00L)
    val averages = listOf(42, 28, 24, 38, 52, 142, 96).map { Pence(it * 100L) }
    return InsightsUiState(
        monthLabel = "October 2026",
        total = Pence(113_000),
        forecast = Pence(214_000),
        usualMonth = Pence(257_409),
        transactionCount = 61,
        trend =
            months.mapIndexed { index, (label, total) ->
                MonthTotal(LocalDate(2026, 5 + index, 1), label, "$label 2026", Pence(total))
            },
        dailyTotals = daily.map { Pence(it * 100L) },
        todayIndex = 5,
        weekdayAverages = averages,
        peakWeekdayIndex = 5,
        rhythm = weekRhythm(averages),
        chips =
            listOf(
                CategoryChip("rent", "Rent", 9, true),
                CategoryChip("gro", "Groceries", 0, true),
                CategoryChip("eat", "Eating out", 1, true),
                CategoryChip("trn", "Transport", 2, false),
            ),
        limits =
            listOf(
                LimitState(
                    categoryId = "eat",
                    name = "Eating out",
                    colorIndex = 1,
                    limit = Pence(30_000),
                    spent = Pence(26_800),
                    remaining = Pence(3_200),
                    dailyAllowance = Pence(128),
                    projected = Pence(41_000),
                    fillFraction = 0.89f,
                    paceFraction = 0.2f,
                    status = LimitStatus.NEAR,
                ),
            ),
        topMerchants =
            listOf(
                MerchantTotal("sains", "Sainsbury's", Pence(28_600), 11),
                MerchantTotal("tfl", "TfL", Pence(10_800), 24),
                MerchantTotal("dish", "Dishoom", Pence(9_200), 3),
            ),
        unusual =
            listOf(
                UnusualSpend("u1", "Fabric", "Entertainment", Instant.parse("2026-10-02T21:00:00Z"), Pence(5_500), Pence(1_040), 5.3f),
                UnusualSpend("u2", "Amazon", "Shopping", Instant.parse("2026-10-04T15:00:00Z"), Pence(8_000), Pence(3_200), 2.5f),
            ),
        eatingOutMonths =
            listOf(
                EatingOutMonth(LocalDate(2026, 10, 1), "October 2026", Pence(26_800), Pence(11_400), hasShared = true),
                EatingOutMonth(LocalDate(2026, 9, 1), "September 2026", Pence(31_000), Pence(31_000), hasShared = false),
            ),
        hasData = true,
        loaded = true,
    )
}
