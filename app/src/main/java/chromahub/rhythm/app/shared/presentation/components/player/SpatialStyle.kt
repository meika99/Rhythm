/*
 * SPDX-License-Identifier: GPL-3.0-or-later AND Apache-2.0
 *
 * Portions of this file are derived from SpatialFlow (PlayerSharedViewModel.kt, PlayerUiComponents.kt,
 * FullPlayer.kt, Type.kt and its vector drawables)
 * Copyright © 2026 Shubham Karande
 * https://github.com/MythicalSHUB/SpatialFlow
 * Licensed under the Apache License, Version 2.0 (see NOTICE_SPATIALFLOW.md).
 *
 * Changes: adapted to Rhythm's package; vector drawables are embedded as Compose ImageVectors; the
 * color extraction works on a Bitmap instead of a SpatialFlow SongItem.
 */

package chromahub.rhythm.app.shared.presentation.components.player

import android.graphics.Bitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import chromahub.rhythm.app.R
import kotlin.math.abs

// ---------------------------------------------------------------------------------------------
// Tipografia: Google Sans Flex con la redondez al maximo (igual que SpatialFlow)
// ---------------------------------------------------------------------------------------------

val SpatialFontFamily: FontFamily = FontFamily(
    listOf(400, 500, 600, 700).map { w ->
        Font(
            resId = R.font.spatial_google_sans_flex,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(w),
                FontVariation.Setting("ROND", 100f)
            )
        )
    }
)

/** Estilos de texto del reproductor (mismos tamanos y espaciados que SpatialFlow). */
object SpatialText {
    /** Titulo de la cancion: en SpatialFlow usa la fuente del sistema en negrita. */
    val title = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    )
    val artist = TextStyle(
        fontFamily = SpatialFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    )
    val header = TextStyle(
        fontFamily = SpatialFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    )
    val chip = TextStyle(
        fontFamily = SpatialFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    val time = TextStyle(
        fontFamily = SpatialFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
}

// ---------------------------------------------------------------------------------------------
// Texto que se desplaza con los bordes difuminados
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.spatialMarquee(edgeWidth: Dp = 12.dp): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val edgeWidthPx = edgeWidth.toPx()
        val leftBrush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.Black),
            startX = 0f,
            endX = edgeWidthPx
        )
        val rightBrush = Brush.horizontalGradient(
            colors = listOf(Color.Black, Color.Transparent),
            startX = size.width - edgeWidthPx,
            endX = size.width
        )
        onDrawWithContent {
            drawContent()
            drawRect(brush = leftBrush, blendMode = BlendMode.DstIn)
            drawRect(brush = rightBrush, blendMode = BlendMode.DstIn)
        }
    }
    .basicMarquee()
    .padding(horizontal = edgeWidth)

// ---------------------------------------------------------------------------------------------
// Colores sacados de la portada (misma receta que SpatialFlow)
// ---------------------------------------------------------------------------------------------

/** Acento y fondo por defecto (cuando no hay portada): violeta neon y carbon oscuro, como en SpatialFlow. */
val SpatialFallbackColors: Pair<Int, Int> = Pair(0xFF8338EC.toInt(), 0xFF0A0B0E.toInt())

/**
 * Devuelve (acento, fondo) calculados con Palette a partir de la portada.
 * Es la misma logica que SpatialFlow usa para el color del fondo y de los botones.
 * Debe llamarse fuera del hilo principal.
 */
fun spatialColorsFromBitmap(bitmap: Bitmap): Pair<Int, Int> {
    return try {
        val palette = Palette.from(bitmap).maximumColorCount(24).generate()

        val allSwatches = palette.swatches
        val totalPopulation = allSwatches.sumOf { it.population }.coerceAtLeast(1)
        val candidateSwatches = listOfNotNull(
            palette.vibrantSwatch,
            palette.darkVibrantSwatch,
            palette.lightVibrantSwatch,
            palette.mutedSwatch,
            palette.darkMutedSwatch,
            palette.lightMutedSwatch,
            palette.dominantSwatch
        )
        val bestAccentSwatch = candidateSwatches.maxByOrNull { swatch ->
            val sat = swatch.hsl[1]
            val pop = swatch.population.toFloat() / totalPopulation.toFloat()
            sat * (0.25f + pop)
        }
        val accentColor = bestAccentSwatch?.rgb
            ?: palette.getVibrantColor(palette.getDominantColor(0xFF8338EC.toInt()))

        val baseBgColor = palette.getDominantColor(0xFF0F0F0F.toInt())

        val accentHsl = FloatArray(3)
        val bgHsl = FloatArray(3)
        ColorUtils.colorToHSL(accentColor, accentHsl)
        ColorUtils.colorToHSL(baseBgColor, bgHsl)

        val maxChannelDelta = maxOf(
            abs(android.graphics.Color.red(baseBgColor) - android.graphics.Color.green(baseBgColor)),
            abs(android.graphics.Color.green(baseBgColor) - android.graphics.Color.blue(baseBgColor)),
            abs(android.graphics.Color.blue(baseBgColor) - android.graphics.Color.red(baseBgColor))
        ) / 255f
        val colorfulPopulation = allSwatches
            .filter { it.hsl[1] >= 0.16f }
            .sumOf { it.population }
        val colorfulRatio = colorfulPopulation.toFloat() / totalPopulation.toFloat()
        val isMonochromatic = (
            colorfulRatio < 0.08f &&
                bgHsl[1] < 0.14f &&
                accentHsl[1] < 0.18f &&
                maxChannelDelta < 0.09f
            ) || (bgHsl[1] < 0.06f && accentHsl[1] < 0.08f)

        val finalAccent = if (isMonochromatic) {
            accentHsl[0] = 0f
            accentHsl[1] = 0f
            accentHsl[2] = 1f
            ColorUtils.HSLToColor(accentHsl)
        } else {
            if (accentHsl[2] < 0.60f) {
                accentHsl[2] = 0.65f
            }
            if (accentHsl[1] < 0.30f) {
                accentHsl[1] = 0.60f
            }
            ColorUtils.HSLToColor(accentHsl)
        }

        val finalBg = if (isMonochromatic) {
            bgHsl[0] = 0f
            bgHsl[1] = 0f
            bgHsl[2] = 0.30f
            ColorUtils.HSLToColor(bgHsl)
        } else {
            if (bgHsl[1] < 0.16f) {
                bgHsl[0] = accentHsl[0]
                bgHsl[1] = (accentHsl[1] * 0.36f).coerceIn(0.18f, 0.40f)
            } else {
                bgHsl[1] = bgHsl[1].coerceIn(0.18f, 0.60f)
            }
            bgHsl[2] = bgHsl[2].coerceIn(0.22f, 0.48f)
            ColorUtils.HSLToColor(bgHsl)
        }

        Pair(finalAccent, finalBg)
    } catch (e: Exception) {
        SpatialFallbackColors
    }
}

/** Color solido del fondo: el color de la portada llevado a un tono muy oscuro (tema oscuro) o pastel (claro). */
fun spatialSurfaceColor(source: Int, isDark: Boolean): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(source, hsl)
    val isMonochrome = hsl[1] < 0.06f
    if (isDark) {
        hsl[2] = 0.155f
        hsl[1] = if (isMonochrome) 0f else hsl[1].coerceIn(0.32f, 0.54f)
    } else {
        hsl[2] = 0.835f
        hsl[1] = if (isMonochrome) 0f else hsl[1].coerceIn(0.30f, 0.48f)
    }
    return Color(ColorUtils.HSLToColor(hsl))
}

/** Color de acento final (boton de reproducir, barra, chips activos). */
fun spatialAccentColor(accent: Int, isDark: Boolean): Color {
    val raw = if (accent != 0) Color(accent) else Color(0xFF8338EC)
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(raw.toArgb(), hsl)
    if (hsl[1] < 0.08f) {
        return if (isDark) Color.White else Color(0xFF1C1B1F)
    }
    return if (isDark) {
        raw
    } else {
        hsl[2] = hsl[2].coerceAtMost(0.45f)
        hsl[1] = hsl[1].coerceAtLeast(0.6f)
        Color(ColorUtils.HSLToColor(hsl))
    }
}

// ---------------------------------------------------------------------------------------------
// Iconos vectoriales (los mismos trazos que usa SpatialFlow)
// ---------------------------------------------------------------------------------------------

private fun spatialVector(name: String, viewport: Float, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = viewport,
        viewportHeight = viewport
    ).addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        fill = SolidColor(Color.Black)
    ).build()

object SpatialIcons {
    val SkipPrevious: ImageVector by lazy {
        spatialVector(
            "SpatialSkipPrevious", 48f,
            "M40.782,12.183C41.445,11.725 42.35,12.2 42.35,13.006V35.094C42.35,35.9 41.445,36.375 40.782,35.917L24.791,24.873C24.216,24.475 24.216,23.625 24.791,23.227L40.782,12.183ZM23.182,12.183C23.845,11.725 24.75,12.2 24.75,13.006L24.75,35.094C24.75,35.9 23.845,36.375 23.182,35.917L7.191,24.873C6.616,24.475 6.616,23.625 7.191,23.227L23.182,12.183Z"
        )
    }
    val SkipNext: ImageVector by lazy {
        spatialVector(
            "SpatialSkipNext", 48f,
            "M7.568,35.867C6.905,36.325 6,35.85 6,35.044V12.956C6,12.15 6.905,11.675 7.568,12.133L23.559,23.177C24.134,23.575 24.134,24.425 23.559,24.823L7.568,35.867ZM25.168,35.867C24.505,36.325 23.6,35.85 23.6,35.044V12.956C23.6,12.15 24.505,11.675 25.168,12.133L41.159,23.177C41.734,23.575 41.734,24.425 41.159,24.823L25.168,35.867Z"
        )
    }
    val Play: ImageVector by lazy {
        spatialVector(
            "SpatialPlay", 48f,
            "M17.537,37.022C16.871,37.446 16,36.967 16,36.178V11.822C16,11.033 16.871,10.554 17.537,10.978L36.674,23.156C37.292,23.549 37.292,24.451 36.674,24.844L17.537,37.022Z"
        )
    }
    val Pause: ImageVector by lazy {
        spatialVector(
            "SpatialPause", 48f,
            "M28.75,38C28.198,38 27.75,37.552 27.75,37V11C27.75,10.448 28.198,10 28.75,10H35.5C36.052,10 36.5,10.448 36.5,11V37C36.5,37.552 36.052,38 35.5,38H28.75ZM12.5,38C11.948,38 11.5,37.552 11.5,37V11C11.5,10.448 11.948,10 12.5,10H19.25C19.802,10 20.25,10.448 20.25,11V37C20.25,37.552 19.802,38 19.25,38H12.5Z"
        )
    }
    val ArrowDown: ImageVector by lazy {
        spatialVector(
            "SpatialArrowDown", 960f,
            "M466.69,594.04q-6.31,-2.5 -10.92,-8.12L263.85,394q-8.7,-7.69 -8.58,-20.12 0.12,-12.42 9.19,-20.99 9.08,-8.58 20.81,-8.58 11.73,0 20.81,8.08L481,527.92l174.54,-175.53q8.69,-7.7 20.31,-7.58 11.61,0.11 20.69,8.69 9.07,8.58 9.07,20.81 0,12.23 -9.07,20.3L505.23,585.92q-4.61,5.62 -10.92,8.12 -6.31,2.5 -13.81,2.5 -7.5,0 -13.81,-2.5Z"
        )
    }
    val ThumbUp: ImageVector by lazy {
        spatialVector(
            "SpatialThumbUp", 960f,
            "M860,328q23,0 41.5,18.5T920,388v82q0,11 -2.5,25.5T910,521L794,789q-9,21 -29.5,36T721,840L314,840q-25,0 -42.5,-17.5T254,780v-428q0,-11 4.5,-22t12.5,-19l207,-218q14,-14 33.5,-17t36.5,7q17,10 25.5,28t4.5,37l-38,180h320ZM137,840q-23,0 -40,-17t-17,-40v-398q0,-23 17,-40t40,-17q23,0 40,17t17,40v398q0,23 -17,40t-40,17Z"
        )
    }
    val ThumbUpOutline: ImageVector by lazy {
        spatialVector(
            "SpatialThumbUpOutline", 960f,
            "M855,328q24,0 42,18t18,42v81.84q0,7.16 1.5,14.66T915,499L789,789q-8.88,21.25 -29.59,36.12Q738.69,840 716,840L272,840v-512l225,-238q13.6,-14 32.19,-16.5Q547.77,71 565,81q17,10 25.5,27.5t4.2,36.5L556,328h299ZM332,353v427h397l126,-299v-93L482,388l53,-249 -203,214ZM139,840q-24.75,0 -42.37,-17.63Q79,804.75 79,780v-392q0,-24.75 17.63,-42.38Q114.25,328 139,328h133v60L139,388v392h133v60L139,840ZM332,780v-427,427Z"
        )
    }
    val Lyrics: ImageVector by lazy {
        spatialVector(
            "SpatialLyrics", 960f,
            "M302,545h90q6.7,0 11.85,-5.21 5.15,-5.21 5.15,-12t-5.15,-12.29Q398.7,510 392,510h-90q-7.68,0 -12.84,5.73T284,528.25q0,6.79 5.16,11.77Q294.32,545 302,545ZM742.59,472Q702,472 674,444t-28,-69q0,-41 28.12,-69.5 28.11,-28.5 69.13,-28.5 19.52,0 33.14,6.5Q790,290 806,305v-197q0,-13.92 9.75,-23.96Q825.5,74 839,74h67q7.13,0 12.56,5.73 5.44,5.73 5.44,12.52 0,6.79 -5.44,11.77Q913.13,109 906,109h-65v266q0,41 -28.41,69t-70,28ZM302,417h210q6.7,0 11.85,-5.21 5.15,-5.21 5.15,-12t-5.15,-12.29Q518.7,382 512,382L302,382q-7.68,0 -12.84,5.73T284,400.25q0,6.79 5.16,11.77Q294.32,417 302,417ZM302,289h210q6.7,0 11.85,-5.21 5.15,-5.21 5.15,-12t-5.15,-12.29Q518.7,254 512,254L302,254q-7.68,0 -12.84,5.73T284,272.25q0,6.79 5.16,11.77Q294.32,289 302,289ZM247.24,674L209,712q-5,5 -11.05,7.5 -6.04,2.5 -12.62,2.5 -12.33,0 -22.83,-8.32 -10.5,-8.31 -10.5,-24.83L152,192q0,-26.5 19.75,-46.75T219,125h375q28.05,0 48.03,18.19Q662,161.38 662,187.05q0,10.95 -5.58,21.85 -5.59,10.89 -16.42,17.1 -37.06,25.08 -57.03,64.42Q563,329.77 563,374.39q0,45.25 21.5,85.23Q606,499.6 644,525q16.54,11.93 25.77,29.46t9.23,36.05q0,34.78 -25.37,59.13Q628.25,674 594,674L247.24,674Z"
        )
    }
    val Share: ImageVector by lazy {
        spatialVector(
            "SpatialShare", 960f,
            "M686,880q-47.5,0 -80.75,-33.25T572,766q0,-8 5,-34L278,557q-16.28,17.34 -37.64,27.17Q219,594 194,594q-47.5,0 -80.75,-33T80,480q0,-48 33.25,-81T194,366q24,0 45,9.3 21,9.29 37,25.7l301,-173q-2,-8 -3.5,-16.5T572,194q0,-47.5 33.25,-80.75T686,80q47.5,0 80.75,33.25T800,194q0,47.5 -33.25,80.75T686,308q-23.27,0 -43.64,-9Q622,290 606,275L302,444q3,8 4.5,17.5t1.5,18q0,8.5 -1,16t-3,15.5l303,173q16,-15 36.09,-23.5 20.1,-8.5 43.07,-8.5Q734,652 767,685.25T800,766q0,47.5 -33.25,80.75T686,880Z"
        )
    }
    val MusicNote: ImageVector by lazy {
        spatialVector(
            "SpatialMusicNote", 960f,
            "M130.5,761.5Q96,727 96,677t34.5,-84.5Q165,558 215,558q25,0 47.5,8t36.5,26v-320q0,-12 8.5,-21t21.5,-11l421,-70q14,-2 25.5,7.5T787,203v394q0,50 -34.5,84.5T668,716q-50,0 -84.5,-34.5T549,597q0,-50 34.5,-84.5T668,478q24,0 46.5,8t37.5,26v-172l-418,63v274q0,50 -34,84.5T215.5,796q-50.5,0 -85,-34.5Z"
        )
    }
}
