package com.tally.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The accent palette. Tally is otherwise a desaturated dark neutral; the user
 * picks one "jewel" tone (Settings → Appearance, stored in DataStore) that
 * colours the FAB, selected states, focus rings, and the active-tab marker.
 *
 * Each jewel is a single OKLCH anchor at a shared lightness/chroma band so they
 * read as siblings. [Emerald] is the default the constitution ships with; its
 * OKLCH is calibrated to render to the committed accent `#33C798` (the seed in
 * `colors.xml`) rather than the looser `oklch(0.76 0.16 165)` label, which would
 * clip to a harsher `#00D097` at the sRGB gamut edge. See ADR-0003.
 *
 * Rendered hex, for reference: Emerald #33C798, Sapphire #5AA3EC,
 * Amethyst #B386E4, Ruby #E36364, Topaz #E7B643, Aquamarine #3ACED6.
 *
 * @property displayName user-facing label for the picker.
 */
enum class Jewel(
    val displayName: String,
    private val l: Double,
    private val c: Double,
    private val h: Double,
) {
    Emerald("Emerald", 0.7422, 0.1399, 166.58),
    Sapphire("Sapphire", 0.70, 0.13, 250.0),
    Amethyst("Amethyst", 0.70, 0.14, 305.0),
    Ruby("Ruby", 0.66, 0.16, 22.0),
    Topaz("Topaz", 0.80, 0.14, 85.0),
    Aquamarine("Aquamarine", 0.78, 0.12, 200.0),
    ;

    /** The full-strength accent. */
    val accent: Color get() = oklch(l, c, h)

    /** A dimmer accent for hairlines and de-emphasised marks. */
    val accentDim: Color get() = oklch(l - 0.10, c * 0.85, h)

    /** A translucent accent for hover/selected fills over a dark surface. */
    val accentWash: Color get() = oklch(l, c, h, alpha = 0.14f)

    /** Foreground colour for text/icons sitting *on* the accent (it is light). */
    val onAccent: Color get() = oklch(0.16, 0.012, h)

    companion object {
        val Default = Emerald
    }
}
