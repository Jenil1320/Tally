package com.tally.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tally's semantic colour roles, resolved from the design-canvas OKLCH tokens.
 *
 * Compose components read colour from here via [LocalTallyColors] (and through
 * the `TallyTheme.colors` accessor) rather than from `MaterialTheme`, because
 * Tally's roles (`bg0..bg3`, `textHi/Mid/Lo`, jewel accent, category dots) do
 * not map cleanly onto Material 3's scheme. The neutral ramp is hue 250 at very
 * low chroma; accents come from the chosen [Jewel].
 *
 * Three anchors are contractually fixed and covered by `OklchTest` — `bg0`
 * (#14181B), `textHi` (#F9FAFA), and the default emerald accent (#33C798). Their
 * OKLCH inputs are calibrated to render to those exact committed hex values, so
 * the pre-Compose splash (`res/values/colors.xml`) and the Compose surface are
 * the same colour. See ADR-0003.
 */
@Immutable
data class TallyColors(
    // Background / surface ramp (darkest → lightest).
    val bg0: Color,
    val bg1: Color,
    val bg2: Color,
    val bg3: Color,
    val line: Color,
    val lineSoft: Color,
    // Text ramp (brightest → faintest).
    val textHi: Color,
    val textMid: Color,
    val textLo: Color,
    val textDim: Color,
    // Accent, from the active jewel.
    val accent: Color,
    val accentDim: Color,
    val accentWash: Color,
    val onAccent: Color,
    // Semantic (fixed regardless of jewel).
    val positive: Color,
    val warning: Color,
    val danger: Color,
    // Ordered palette for category dots / chart series.
    val categories: List<Color>,
) {
    /** Deterministic category colour for an index (wraps the palette). */
    fun category(index: Int): Color = categories[((index % categories.size) + categories.size) % categories.size]
}

/**
 * The neutral + semantic tokens shared by every jewel.
 *
 * `bg0` and `textHi` are *calibrated* OKLCH — the L/C/H that round-trip to the
 * committed splash hex in `colors.xml` exactly (see ADR-0003). The intermediate
 * surfaces and text tones are then derived by walking lightness up/down the same
 * hue (240) at low chroma, which keeps the ramp perceptually even. The rendered
 * hex of each derived token is noted for reference.
 */
private val neutral =
    object {
        val bg0 = oklch(0.2061, 0.0086, 240.33) // #14181B — page (canvas anchor)
        val bg1 = oklch(0.250, 0.009, 240.0) // #1E2226 — card / list surface
        val bg2 = oklch(0.290, 0.010, 240.0) // #272C30 — elevated sheet / dialog
        val bg3 = oklch(0.335, 0.011, 240.0) // #32383C — input field / pressed
        val line = oklch(0.400, 0.012, 240.0) // #42494E — visible border
        val lineSoft = oklch(0.300, 0.010, 240.0) // #292F32 — hairline divider
        val textHi = oklch(0.9843, 0.0011, 197.14) // #F9FAFA — primary text (anchor)
        val textMid = oklch(0.760, 0.006, 240.0) // #AEB2B4 — secondary text
        val textLo = oklch(0.580, 0.008, 240.0) // #767B7F — captions / meta
        val textDim = oklch(0.440, 0.009, 240.0) // #4E5357 — disabled / placeholder
        val positive = oklch(0.76, 0.15, 158.0) // credits, refunds, money in
        val warning = oklch(0.78, 0.15, 75.0) // approaching a limit
        val danger = oklch(0.64, 0.20, 25.0) // destructive actions
    }

/**
 * The category-dot palette: evenly spread hues at a shared lightness/chroma so
 * no single category shouts. Used by `TCatDot` and the hand-rolled charts.
 */
private val categoryPalette: List<Color> =
    // Hue order: red, orange, gold, green, teal, cyan, blue, indigo, violet, pink.
    listOf(
        oklch(0.72, 0.13, 25.0),
        oklch(0.74, 0.13, 60.0),
        oklch(0.80, 0.12, 95.0),
        oklch(0.76, 0.13, 145.0),
        oklch(0.74, 0.12, 178.0),
        oklch(0.72, 0.12, 212.0),
        oklch(0.70, 0.13, 250.0),
        oklch(0.68, 0.14, 288.0),
        oklch(0.70, 0.14, 320.0),
        oklch(0.74, 0.13, 350.0),
    )

/** Builds the full colour set for a chosen [jewel]. */
fun tallyColors(jewel: Jewel = Jewel.Default): TallyColors =
    TallyColors(
        bg0 = neutral.bg0,
        bg1 = neutral.bg1,
        bg2 = neutral.bg2,
        bg3 = neutral.bg3,
        line = neutral.line,
        lineSoft = neutral.lineSoft,
        textHi = neutral.textHi,
        textMid = neutral.textMid,
        textLo = neutral.textLo,
        textDim = neutral.textDim,
        accent = jewel.accent,
        accentDim = jewel.accentDim,
        accentWash = jewel.accentWash,
        onAccent = jewel.onAccent,
        positive = neutral.positive,
        warning = neutral.warning,
        danger = neutral.danger,
        categories = categoryPalette,
    )

/**
 * Colour access for composables. Default is the emerald build; the real value
 * is provided by `TallyTheme`. `staticCompositionLocalOf` because the palette
 * changes rarely (only when the user switches jewel), so we don't want every
 * colour read to subscribe to recomposition.
 */
val LocalTallyColors = staticCompositionLocalOf { tallyColors() }
