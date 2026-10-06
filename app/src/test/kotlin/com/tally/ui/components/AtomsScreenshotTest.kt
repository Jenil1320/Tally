package com.tally.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.tally.core.money.Pence
import com.tally.ui.theme.TallyTheme
import org.junit.Rule
import org.junit.Test

/**
 * Paparazzi goldens for the PR2 atoms. Renders an atom gallery on two device
 * sizes (Pixel 4a and a large phone proxy for the Pixel 7 Pro) so a future
 * change that shifts colour, type, or spacing fails CI with a visible diff.
 *
 * NOTE: the first recording captures whatever renders as the source of truth —
 * the generated PNGs in `src/test/snapshots/` should be eyeballed once before
 * they're trusted (especially that Geist loaded from assets, not a fallback).
 */
class AtomsScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_4A)

    @Test
    fun atomsGalleryCompact() {
        paparazzi.snapshot { AtomsGallery() }
    }

    @Test
    fun atomsGalleryLarge() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_6_PRO)
        paparazzi.snapshot { AtomsGallery() }
    }
}

@Composable
private fun AtomsGallery() {
    TallyTheme(reduceMotion = true) {
        val colors = TallyTheme.colors
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(colors.bg0)
                    .padding(TallyTheme.spacing.xl),
            verticalArrangement = Arrangement.spacedBy(TallyTheme.spacing.lg),
        ) {
            TText(text = "Tally atoms", style = TallyTheme.typography.headline)
            TText(text = "Geist body — secondary tone", color = colors.textMid)
            TText(text = "Caption tone", style = TallyTheme.typography.caption, color = colors.textLo)

            TCard(border = true) {
                TText(text = "Spent today", style = TallyTheme.typography.caption, color = colors.textLo)
                TNum(amount = Pence(123_456), size = TNumSize.Large)
            }

            TCard {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TCatDot(color = colors.category(0))
                    TText(text = "Pret a Manger", modifier = Modifier.weight(1f))
                    TNum(amount = Pence(485))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.sm)) {
                repeat(6) { index -> TCatDot(color = colors.category(index), size = 16.dp) }
            }

            TNum(amount = Pence(1299), color = colors.positive, signed = true)

            Row(horizontalArrangement = Arrangement.spacedBy(TallyTheme.spacing.md)) {
                TButton(text = "Save", onClick = {})
                TButton(text = "Disabled", onClick = {}, enabled = false)
            }
        }
    }
}
