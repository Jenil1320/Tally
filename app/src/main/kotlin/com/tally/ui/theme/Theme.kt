package com.tally.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember

/**
 * The Tally theme root. Wrap the whole app (and every Compose preview / screen-
 * shot test) in this. It:
 *
 *  - resolves the [TallyColors] for the chosen [jewel];
 *  - builds the Geist/Geist Mono [TallyTypography] from bundled assets;
 *  - publishes Tally's tokens via composition locals (read through [TallyTheme]);
 *  - bridges to a Material 3 [darkColorScheme] so any stock M3 component that
 *    slips in still lands on Tally's palette rather than purple.
 *
 * There is no light scheme — Tally is dark-only by constitution (N1).
 */
@Composable
fun TallyTheme(
    jewel: Jewel = Jewel.Default,
    content: @Composable () -> Unit,
) {
    val colors = remember(jewel) { tallyColors(jewel) }
    val typography = rememberTallyTypography()
    val shapes = remember { TallyShapes() }
    val spacing = remember { TallySpacing() }
    val materialScheme = remember(colors) { colors.toMaterialColorScheme() }
    val materialTypography = remember(typography) { typography.toMaterialTypography() }

    CompositionLocalProvider(
        LocalTallyColors provides colors,
        LocalTallyTypography provides typography,
        LocalTallyShapes provides shapes,
        LocalTallySpacing provides spacing,
        LocalContentColor provides colors.textHi,
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = materialTypography,
            content = content,
        )
    }
}

/**
 * Ergonomic accessor: `TallyTheme.colors`, `TallyTheme.typography`, etc. inside
 * any composable, mirroring `MaterialTheme.*`.
 */
object TallyTheme {
    val colors: TallyColors
        @Composable @ReadOnlyComposable
        get() = LocalTallyColors.current
    val typography: TallyTypography
        @Composable @ReadOnlyComposable
        get() = LocalTallyTypography.current
    val shapes: TallyShapes
        @Composable @ReadOnlyComposable
        get() = LocalTallyShapes.current
    val spacing: TallySpacing
        @Composable @ReadOnlyComposable
        get() = LocalTallySpacing.current
}

private fun TallyColors.toMaterialColorScheme() =
    darkColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentDim,
        onPrimaryContainer = textHi,
        secondary = accentDim,
        onSecondary = onAccent,
        background = bg0,
        onBackground = textHi,
        surface = bg1,
        onSurface = textHi,
        surfaceVariant = bg2,
        onSurfaceVariant = textMid,
        surfaceContainer = bg2,
        surfaceContainerHigh = bg3,
        error = danger,
        onError = onAccent,
        outline = line,
        outlineVariant = lineSoft,
    )

private fun TallyTypography.toMaterialTypography() =
    Typography(
        displaySmall = display,
        headlineSmall = headline,
        titleLarge = title,
        titleMedium = title,
        bodyLarge = body,
        bodyMedium = body,
        bodySmall = caption,
        labelLarge = label,
        labelMedium = label,
        labelSmall = caption,
    )
