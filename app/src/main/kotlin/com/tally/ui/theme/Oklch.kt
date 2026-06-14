package com.tally.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Converts an OKLCH colour to an sRGB [Color].
 *
 * The Tally design canvas specifies every colour in OKLCH because it is
 * perceptually uniform — equal steps in `L` look like equal steps in
 * brightness, which is what makes the dark neutral ramp read evenly. Compose
 * has no native OKLCH, so this helper does the OKLCH → OKLab → linear-sRGB →
 * gamma-sRGB pipeline (Björn Ottosson's OKLab matrices) at theme-build time.
 *
 * @param lightness perceptual lightness, `0.0`(black)..`1.0`(white).
 * @param chroma colourfulness, `0.0` (grey) upward; jewel accents sit near 0.16.
 * @param hue hue angle in degrees, `0..360`.
 * @param alpha opacity, `0.0`..`1.0`.
 */
fun oklch(
    lightness: Double,
    chroma: Double,
    hue: Double,
    alpha: Float = 1f,
): Color {
    val hRad = hue * (Math.PI / 180.0)
    val a = chroma * cos(hRad)
    val b = chroma * sin(hRad)

    // OKLab → approximate (non-linear) LMS.
    val lPrime = lightness + 0.3963377774 * a + 0.2158037573 * b
    val mPrime = lightness - 0.1055613458 * a - 0.0638541728 * b
    val sPrime = lightness - 0.0894841775 * a - 1.2914855480 * b

    val l = lPrime * lPrime * lPrime
    val m = mPrime * mPrime * mPrime
    val s = sPrime * sPrime * sPrime

    // LMS → linear sRGB.
    val rLin = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
    val gLin = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
    val bLin = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s

    return Color(
        red = linearToSrgb(rLin),
        green = linearToSrgb(gLin),
        blue = linearToSrgb(bLin),
        alpha = alpha,
    )
}

/** Linear-light component → gamma-encoded sRGB, clamped to `[0, 1]`. */
private fun linearToSrgb(channel: Double): Float {
    val clamped = channel.coerceIn(0.0, 1.0)
    val encoded =
        if (clamped <= 0.0031308) {
            12.92 * clamped
        } else {
            1.055 * clamped.pow(1.0 / 2.4) - 0.055
        }
    return encoded.toFloat()
}
