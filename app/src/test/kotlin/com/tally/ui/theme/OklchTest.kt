package com.tally.ui.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

/**
 * Two guarantees:
 *
 *  1. The OKLCH → sRGB maths is correct (the extremes and alpha behave).
 *  2. The three calibrated theme anchors render to the exact committed splash
 *     hex in `res/values/colors.xml` — so the pre-Compose splash and the first
 *     Compose frame are the same colour and there is no cold-start flash. If a
 *     token or the maths drifts, these break. See ADR-0003.
 *
 * Tolerance is ±1 per 8-bit channel (pure float-rounding slack).
 */
class OklchTest {
    private fun Color.rgb255(): Triple<Int, Int, Int> =
        Triple((red * 255).roundToInt(), (green * 255).roundToInt(), (blue * 255).roundToInt())

    private fun assertHex(
        color: Color,
        r: Int,
        g: Int,
        b: Int,
        tolerance: Int = 1,
    ) {
        val (ar, ag, ab) = color.rgb255()
        assertThat(ar).isIn((r - tolerance)..(r + tolerance))
        assertThat(ag).isIn((g - tolerance)..(g + tolerance))
        assertThat(ab).isIn((b - tolerance)..(b + tolerance))
    }

    @Test fun `page background anchor renders to committed 14181B`() {
        assertHex(tallyColors().bg0, 0x14, 0x18, 0x1B)
    }

    @Test fun `high-emphasis text anchor renders to committed F9FAFA`() {
        assertHex(tallyColors().textHi, 0xF9, 0xFA, 0xFA)
    }

    @Test fun `default emerald accent renders to committed 33C798`() {
        assertHex(Jewel.Emerald.accent, 0x33, 0xC7, 0x98)
        assertHex(tallyColors().accent, 0x33, 0xC7, 0x98)
    }

    @Test fun `pure black and white map to the extremes`() {
        assertHex(oklch(0.0, 0.0, 0.0), 0, 0, 0)
        assertHex(oklch(1.0, 0.0, 0.0), 255, 255, 255)
    }

    @Test fun `alpha is carried through`() {
        assertThat(oklch(0.5, 0.0, 0.0, alpha = 0.5f).alpha).isWithin(0.01f).of(0.5f)
    }
}
