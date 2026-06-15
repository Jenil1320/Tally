package com.tally.ui.components.chart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.tally.core.money.Pence
import com.tally.ui.components.TNum
import com.tally.ui.components.TNumSize
import com.tally.ui.components.TText
import com.tally.ui.theme.TallyTheme
import org.junit.Rule
import org.junit.Test

/** Golden for the hand-rolled donut chart. Eyeball once before trusting. */
class ChartsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6_PRO)

    @Test
    fun categoryDonut() {
        paparazzi.snapshot {
            TallyTheme {
                val colors = TallyTheme.colors
                Box(
                    modifier = Modifier.background(colors.bg0).padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TDonutChart(
                        slices =
                            listOf(
                                DonutSlice(0.45f, colors.category(6)),
                                DonutSlice(0.30f, colors.category(3)),
                                DonutSlice(0.15f, colors.category(1)),
                                DonutSlice(0.10f, colors.category(8)),
                            ),
                        modifier = Modifier.size(220.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TText("Spent", style = TallyTheme.typography.caption, color = colors.textLo)
                            TNum(amount = Pence(123_456), size = TNumSize.Medium)
                        }
                    }
                }
            }
        }
    }
}
