package com.tally.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Tally's type scale. Text is Geist; every digit that represents money or a
 * count is Geist Mono with tabular figures (`tnum`) so columns of numbers line
 * up and never reflow as values change — see `TNum`, which is the only way the
 * constitution allows a number to reach the screen.
 *
 * Fonts are bundled as assets (fetched by `scripts/fetch-geist-fonts.*` and
 * git-ignored), so the families are built from the `AssetManager` at runtime
 * via [rememberTallyTypography] rather than from `res/font`.
 */
@Immutable
data class TallyTypography(
    // the hero balance on Today
    val display: TextStyle,
    // screen titles
    val headline: TextStyle,
    // section / card headers
    val title: TextStyle,
    // default running text
    val body: TextStyle,
    // emphasised body
    val bodyStrong: TextStyle,
    // buttons, tab labels, chips
    val label: TextStyle,
    // meta, timestamps, helper text
    val caption: TextStyle,
    // the small uppercase mono label above a figure or a section (use uppercase text)
    val kicker: TextStyle,
    // the one big figure on a screen (mono, tnum)
    val numericHero: TextStyle,
    // hero money (mono, tnum)
    val numericLarge: TextStyle,
    // stat-tile money (mono, tnum)
    val numericTitle: TextStyle,
    // row amounts (mono, tnum)
    val numericMedium: TextStyle,
    // dense figures (mono, tnum)
    val numericSmall: TextStyle,
)

private const val TABULAR = "tnum"

private fun buildTypography(
    geist: FontFamily,
    mono: FontFamily,
): TallyTypography =
    TallyTypography(
        display =
            TextStyle(
                fontFamily = geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                letterSpacing = (-0.02).em,
            ),
        headline =
            TextStyle(
                fontFamily = geist,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.015).em,
            ),
        title =
            TextStyle(fontFamily = geist, fontWeight = FontWeight.Medium, fontSize = 19.sp, lineHeight = 24.sp, letterSpacing = (-0.01).em),
        body =
            TextStyle(fontFamily = geist, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        bodyStrong =
            TextStyle(fontFamily = geist, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp),
        label =
            TextStyle(fontFamily = geist, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.005.em),
        caption =
            TextStyle(fontFamily = geist, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.01.em),
        kicker =
            TextStyle(
                fontFamily = mono,
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.14.em,
            ),
        numericHero =
            TextStyle(
                fontFamily = mono,
                fontWeight = FontWeight.Medium,
                fontSize = 52.sp,
                lineHeight = 56.sp,
                letterSpacing = (-0.04).em,
                fontFeatureSettings = TABULAR,
            ),
        numericLarge =
            TextStyle(
                fontFamily = mono,
                fontWeight = FontWeight.Medium,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                letterSpacing = (-0.01).em,
                fontFeatureSettings = TABULAR,
            ),
        numericTitle =
            TextStyle(
                fontFamily = mono,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.02).em,
                fontFeatureSettings = TABULAR,
            ),
        numericMedium =
            TextStyle(
                fontFamily = mono,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontFeatureSettings = TABULAR,
            ),
        numericSmall =
            TextStyle(
                fontFamily = mono,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontFeatureSettings = TABULAR,
            ),
    )

/**
 * Builds the Geist/Geist Mono typography from bundled assets. Remembered so the
 * `FontFamily` objects (and their backing typefaces) are created once per
 * composition root, not on every recomposition.
 */
@Composable
fun rememberTallyTypography(): TallyTypography {
    val assets = LocalContext.current.assets
    return remember(assets) {
        val geist =
            FontFamily(
                Font("fonts/Geist-Light.ttf", assets, FontWeight.Light),
                Font("fonts/Geist-Regular.ttf", assets, FontWeight.Normal),
                Font("fonts/Geist-Medium.ttf", assets, FontWeight.Medium),
                Font("fonts/Geist-SemiBold.ttf", assets, FontWeight.SemiBold),
                Font("fonts/Geist-Bold.ttf", assets, FontWeight.Bold),
            )
        val mono =
            FontFamily(
                Font("fonts/GeistMono-Regular.ttf", assets, FontWeight.Normal),
                Font("fonts/GeistMono-Medium.ttf", assets, FontWeight.Medium),
                Font("fonts/GeistMono-SemiBold.ttf", assets, FontWeight.SemiBold),
            )
        buildTypography(geist, mono)
    }
}

/**
 * Fallback typography for previews/tests that render without the bundled assets
 * (system sans + monospace). `TallyTheme` overrides this with the real Geist
 * families.
 */
val LocalTallyTypography =
    staticCompositionLocalOf {
        buildTypography(FontFamily.Default, FontFamily.Monospace)
    }
