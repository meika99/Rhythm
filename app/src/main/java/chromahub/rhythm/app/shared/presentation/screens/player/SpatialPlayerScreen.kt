/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Estilo de reproductor "Spatial" para Rhythm.
 *
 * El diseno visual de esta pantalla (medidas, portada, chips, barra ondulada, grupo de botones, color de fondo
 * y de acento) esta adaptado de SpatialFlow (FullPlayer.kt, PlayerUiComponents.kt y PlayerSharedViewModel.kt)
 * Copyright © 2026 Shubham Karande
 * https://github.com/MythicalSHUB/SpatialFlow
 * Licenciado bajo Apache License, Version 2.0 (ver NOTICE_SPATIALFLOW.md).
 *
 * Cambios: reescrito sobre el modelo de datos de Rhythm (Song y los callbacks de PlayerScreen); el grupo de
 * botones imita ButtonGroup de Material 3 Expressive con un Layout propio; textos en espanol.
 */

package chromahub.rhythm.app.shared.presentation.screens.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.player.SpatialFallbackColors
import chromahub.rhythm.app.shared.presentation.components.player.SpatialIcons
import chromahub.rhythm.app.shared.presentation.components.player.SpatialText
import chromahub.rhythm.app.shared.presentation.components.player.SpatialWavySlider
import chromahub.rhythm.app.shared.presentation.components.player.formatDuration
import chromahub.rhythm.app.shared.presentation.components.player.spatialAccentColor
import chromahub.rhythm.app.shared.presentation.components.player.spatialColorsFromBitmap
import chromahub.rhythm.app.shared.presentation.components.player.spatialMarquee
import chromahub.rhythm.app.shared.presentation.components.player.spatialSurfaceColor
import chromahub.rhythm.app.util.ImageUtils
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.material3.Icon as M3Icon

/**
 * Reproductor "Spatial" (copia del Now Playing de SpatialFlow).
 *
 * Solo necesita unos pocos callbacks; todo lo demas (cola, info de la cancion, letras a pantalla completa,
 * temporizador...) lo sigue manejando [PlayerScreen].
 *
 * Rendimiento: esta pantalla NO lee el progreso en su cuerpo; solo lo lee [SpatialProgressSection]
 * (barra y tiempos), asi el resto no se recompone cada 100 ms.
 */
@Composable
fun SpatialPlayerScreen(
    song: Song?,
    isPlaying: Boolean,
    isFavorite: Boolean,
    progress: () -> Float,
    durationMs: Long,
    showRemainingTime: Boolean,
    useHoursFormat: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onOpenLyrics: () -> Unit,
    onQueueClick: () -> Unit,
    onSongInfoClick: () -> Unit,
    onShowArtist: () -> Unit,
    onMoreClick: () -> Unit,
    onSleepTimer: () -> Unit,
    onEqualizer: () -> Unit,
    onShareFile: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onTotalTimeClick: () -> Unit = {},
    swipeToDismissEnabled: Boolean = true
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // Igual que SpatialFlow: tema oscuro -> fondo oscuro con tinte y texto blanco; tema claro -> pastel y texto oscuro.
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val contentColor = if (isDark) Color.White else Color(0xFF1C1B1F)
    val contentSecondary = contentColor.copy(alpha = 0.6f)
    val fillAlpha = if (isDark) 0.08f else 0.06f

    // ---- Colores sacados de la portada (misma formula que SpatialFlow) ----
    val artworkUri = song?.artworkUri
    val colors by produceState(initialValue = SpatialFallbackColors, artworkUri) {
        value = if (artworkUri == null) {
            SpatialFallbackColors
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val bitmap = ImageUtils.loadArtworkBitmap(context, artworkUri, 128)
                    if (bitmap != null) spatialColorsFromBitmap(bitmap) else SpatialFallbackColors
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    SpatialFallbackColors
                }
            }
        }
    }
    val accent = remember(colors, isDark) { spatialAccentColor(colors.first, isDark) }
    val targetBackground = remember(colors, isDark) { spatialSurfaceColor(colors.second, isDark) }
    val background = animateColorAsState(targetBackground, tween(500), label = "SpatialBackground")

    // "Deslizar hacia abajo para cerrar" cuando la pantalla es completa (la hoja ya maneja su propio arrastre).
    val swipeModifier = if (swipeToDismissEnabled) {
        Modifier.pointerInput(Unit) {
            var total = 0f
            detectVerticalDragGestures(
                onDragStart = { total = 0f },
                onDragEnd = { if (total > 240f) onBack() },
                onDragCancel = { total = 0f }
            ) { _, dragAmount -> total += dragAmount }
        }
    } else {
        Modifier
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(background.value) }
            .then(swipeModifier)
    ) {
        val density = LocalDensity.current
        val statusBarTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
        val navBarBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }

        // Medidas de SpatialFlow: portada = 90% del ancho; en pantallas muy bajitas se reduce lo justo para no cortar los botones.
        val albumArtSize = minOf(
            maxWidth * 0.9f,
            (maxHeight - statusBarTop - navBarBottom - 448.dp).coerceAtLeast(140.dp)
        )
        val topGap = ((maxHeight - albumArtSize) / 2f - 220.dp - (statusBarTop + 68.dp)).coerceAtLeast(0.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---- Encabezado ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    M3Icon(
                        imageVector = SpatialIcons.ArrowDown,
                        contentDescription = stringResource(R.string.spatial_collapse),
                        tint = contentColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.spatial_now_playing),
                    style = SpatialText.header,
                    color = contentSecondary
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            if (topGap > 0.dp) {
                Spacer(modifier = Modifier.height(topGap))
            }

            // ---- Portada ----
            val artShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .size(albumArtSize)
                    .shadow(16.dp, artShape)
                    .clip(artShape)
            ) {
                if (artworkUri != null) {
                    val request = remember(artworkUri) {
                        ImageRequest.Builder(context)
                            .data(artworkUri)
                            .crossfade(true)
                            .memoryCacheKey(artworkUri.toString())
                            .diskCacheKey(artworkUri.toString())
                            .build()
                    }
                    AsyncImage(
                        model = request,
                        contentDescription = song?.album,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        M3Icon(
                            imageVector = SpatialIcons.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(96.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ---- Titulo / artista ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song?.title ?: stringResource(R.string.spatial_nothing_playing),
                        style = SpatialText.title,
                        color = contentColor,
                        maxLines = 1,
                        modifier = Modifier
                            .spatialMarquee()
                            .clickable { onSongInfoClick() }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song?.artist ?: "",
                        style = SpatialText.artist,
                        color = contentSecondary,
                        maxLines = 1,
                        modifier = Modifier
                            .spatialMarquee()
                            .clickable { onShowArtist() }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Chips en pastilla (se desplazan horizontalmente, de borde a borde) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val pad = 20.dp.roundToPx()
                        val placeable = measurable.measure(
                            constraints.copy(maxWidth = constraints.maxWidth + 2 * pad)
                        )
                        layout(placeable.width - 2 * pad, placeable.height) {
                            placeable.place(-pad, 0)
                        }
                    }
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(12.dp))

                SpatialPillChip(
                    label = stringResource(R.string.spatial_like),
                    imageVector = if (isFavorite) SpatialIcons.ThumbUp else SpatialIcons.ThumbUpOutline,
                    isSelected = isFavorite,
                    onClick = onToggleFavorite,
                    contentColor = contentColor,
                    accentColor = accent,
                    isDark = isDark
                )
                SpatialPillChip(
                    label = stringResource(R.string.spatial_lyrics),
                    imageVector = SpatialIcons.Lyrics,
                    onClick = onOpenLyrics,
                    contentColor = contentColor,
                    accentColor = accent,
                    isDark = isDark
                )
                SpatialPillChip(
                    label = stringResource(R.string.spatial_shuffle),
                    symbol = "shuffle",
                    isSelected = isShuffleEnabled,
                    onClick = onToggleShuffle,
                    contentColor = contentColor,
                    accentColor = accent,
                    isDark = isDark
                )
                SpatialPillChip(
                    label = stringResource(R.string.spatial_repeat),
                    symbol = if (repeatMode == 2) "repeat_one" else "repeat",
                    isSelected = repeatMode != 0,
                    onClick = onToggleRepeat,
                    contentColor = contentColor,
                    accentColor = accent,
                    isDark = isDark
                )
                SpatialPillChip(
                    label = stringResource(R.string.spatial_sleep_timer),
                    symbol = "bedtime",
                    onClick = onSleepTimer,
                    contentColor = contentColor,
                    accentColor = accent,
                    isDark = isDark
                )
                SpatialPillChip(
                    label = stringResource(R.string.spatial_equalizer),
                    symbol = "equalizer",
                    onClick = onEqualizer,
                    contentColor = contentColor,
                    accentColor = accent,
                    isDark = isDark
                )
                SpatialPillChip(
                    label = stringResource(R.string.spatial_share),
                    imageVector = SpatialIcons.Share,
                    onClick = onShareFile,
                    contentColor = contentColor,
                    accentColor = accent,
                    isDark = isDark
                )
                SpatialPillChip(
                    label = stringResource(R.string.spatial_more),
                    symbol = "more_horiz",
                    onClick = onMoreClick,
                    contentColor = contentColor,
                    accentColor = accent,
                    isDark = isDark
                )

                Spacer(modifier = Modifier.width(12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---- Barra ondulada + tiempos (unico lugar que lee el progreso) ----
            SpatialProgressSection(
                progress = progress,
                durationMs = durationMs,
                isPlaying = isPlaying,
                onSeek = onSeek,
                showRemainingTime = showRemainingTime,
                useHoursFormat = useHoursFormat,
                onTotalTimeClick = onTotalTimeClick,
                accent = accent,
                contentColor = contentColor,
                contentSecondary = contentSecondary,
                fillAlpha = fillAlpha
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Botones: anterior / reproducir-pausar / siguiente ----
            SpatialTransportRow(
                isPlaying = isPlaying,
                onPrevious = onSkipPrevious,
                onPlayPause = onPlayPause,
                onNext = onSkipNext,
                contentColor = contentColor,
                accent = accent,
                isDark = isDark,
                fillAlpha = fillAlpha,
                previousLabel = stringResource(R.string.spatial_previous),
                nextLabel = stringResource(R.string.spatial_next),
                playLabel = stringResource(R.string.spatial_play),
                pauseLabel = stringResource(R.string.spatial_pause)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Deslizar hacia arriba o tocar la flecha para abrir la cola ----
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .pointerInput(Unit) {
                        var fired = false
                        detectVerticalDragGestures(
                            onDragStart = { fired = false }
                        ) { _, dragAmount ->
                            if (dragAmount < -10f && !fired) {
                                fired = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onQueueClick()
                            }
                        }
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onQueueClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                M3Icon(
                    imageVector = SpatialIcons.ArrowDown,
                    contentDescription = stringResource(R.string.spatial_open_queue),
                    tint = contentColor.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer { rotationZ = 180f }
                )
            }
        }
    }
}

/** Barra ondulada + tiempo actual + tiempo total. Es lo unico que se recompone con el progreso. */
@Composable
private fun SpatialProgressSection(
    progress: () -> Float,
    durationMs: Long,
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    showRemainingTime: Boolean,
    useHoursFormat: Boolean,
    onTotalTimeClick: () -> Unit,
    accent: Color,
    contentColor: Color,
    contentSecondary: Color,
    fillAlpha: Float
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }
    val currentProgress = progress().coerceIn(0f, 1f)
    val shownProgress = if (isScrubbing) scrubValue else currentProgress

    val currentSeconds = (shownProgress * durationMs).toLong() / 1000L
    val totalSeconds = durationMs / 1000L
    val remainingSeconds = (totalSeconds - currentSeconds).coerceAtLeast(0L)
    val currentText = remember(currentSeconds, useHoursFormat) {
        formatDuration(currentSeconds * 1000L, useHoursFormat)
    }
    val totalText = remember(showRemainingTime, totalSeconds, remainingSeconds, useHoursFormat) {
        if (showRemainingTime) "-" + formatDuration(remainingSeconds * 1000L, useHoursFormat)
        else formatDuration(totalSeconds * 1000L, useHoursFormat)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        SpatialWavySlider(
            value = shownProgress,
            onValueChange = {
                isScrubbing = true
                scrubValue = it
            },
            onValueChangeFinished = {
                onSeek(scrubValue)
                isScrubbing = false
            },
            modifier = Modifier.fillMaxWidth(),
            activeTrackColor = accent,
            inactiveTrackColor = contentColor.copy(alpha = fillAlpha),
            thumbColor = contentColor,
            isPlaying = isPlaying,
            trackHeight = 4.dp,
            thumbRadius = 6.dp,
            waveAmplitudeWhenPlaying = 6.dp,
            waveLength = 48.dp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = currentText,
                style = SpatialText.time,
                color = contentSecondary
            )
            Text(
                text = totalText,
                style = SpatialText.time,
                color = contentSecondary,
                modifier = Modifier.clickable { onTotalTimeClick() }
            )
        }
    }
}

/** Pastilla redondeada con icono y texto; se resalta con el color de acento cuando esta activa. */
@Composable
private fun SpatialPillChip(
    label: String,
    onClick: () -> Unit,
    contentColor: Color,
    accentColor: Color,
    isDark: Boolean,
    isSelected: Boolean = false,
    imageVector: ImageVector? = null,
    symbol: String? = null
) {
    val backgroundColor = if (isSelected) {
        accentColor.copy(alpha = if (isDark) 0.25f else 0.18f)
    } else {
        contentColor.copy(alpha = if (isDark) 0.08f else 0.06f)
    }
    val tintColor = if (isSelected) accentColor else contentColor.copy(alpha = 0.8f)

    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (imageVector != null) {
                M3Icon(
                    imageVector = imageVector,
                    contentDescription = label,
                    tint = tintColor,
                    modifier = Modifier.size(18.dp)
                )
            } else if (symbol != null) {
                Icon(
                    icon = MaterialSymbolIcon(symbol),
                    contentDescription = label,
                    modifier = Modifier.size(18.dp),
                    tint = tintColor,
                    size = 18.dp
                )
            }
            Text(
                text = label,
                style = SpatialText.chip,
                color = tintColor,
                maxLines = 1
            )
        }
    }
}

/**
 * Grupo de tres botones como el ButtonGroup de Material 3 Expressive que usa SpatialFlow:
 * anchos 1 : 1,2 : 1, separacion de 12 dp, y al presionar uno se ensancha un 30% mientras
 * los vecinos se comprimen (con resorte). Cada boton ademas cierra sus esquinas al presionarlo.
 */
@Composable
private fun SpatialTransportRow(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    contentColor: Color,
    accent: Color,
    isDark: Boolean,
    fillAlpha: Float,
    previousLabel: String,
    nextLabel: String,
    playLabel: String,
    pauseLabel: String
) {
    val source0 = remember { MutableInteractionSource() }
    val source1 = remember { MutableInteractionSource() }
    val source2 = remember { MutableInteractionSource() }
    val pressed0 by source0.collectIsPressedAsState()
    val pressed1 by source1.collectIsPressedAsState()
    val pressed2 by source2.collectIsPressedAsState()
    val growSpec = spring<Float>(dampingRatio = 0.6f, stiffness = 800f)
    val grow0 = animateFloatAsState(if (pressed0) 1f else 0f, growSpec, label = "SpatialGrowPrev")
    val grow1 = animateFloatAsState(if (pressed1) 1f else 0f, growSpec, label = "SpatialGrowPlay")
    val grow2 = animateFloatAsState(if (pressed2) 1f else 0f, growSpec, label = "SpatialGrowNext")

    val neutral = contentColor.copy(alpha = fillAlpha)
    val playContent = if (isDark) Color(0xFF1C1B1F) else Color.White

    Layout(
        content = {
            SpatialTransportButton(
                onClick = onPrevious,
                interactionSource = source0,
                containerColor = neutral,
                contentColor = contentColor,
                icon = SpatialIcons.SkipPrevious,
                iconSize = 36.dp,
                contentDescription = previousLabel
            )
            SpatialTransportButton(
                onClick = onPlayPause,
                interactionSource = source1,
                containerColor = accent,
                contentColor = playContent,
                icon = if (isPlaying) SpatialIcons.Pause else SpatialIcons.Play,
                iconSize = 42.dp,
                contentDescription = if (isPlaying) pauseLabel else playLabel
            )
            SpatialTransportButton(
                onClick = onNext,
                interactionSource = source2,
                containerColor = neutral,
                contentColor = contentColor,
                icon = SpatialIcons.SkipNext,
                iconSize = 36.dp,
                contentDescription = nextLabel
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) { measurables, constraints ->
        val spacing = 12.dp.roundToPx()
        val buttonHeight = 76.dp.roundToPx()
        val total = constraints.maxWidth
        val available = (total - spacing * 2).coerceAtLeast(0)
        val unit = available / 3.2f
        val widths = intArrayOf(
            (unit * 1f).roundToInt(),
            (unit * 1.2f).roundToInt(),
            (unit * 1f).roundToInt()
        )
        val base = widths.copyOf()
        val ratio = 0.3f
        val pressProgress = floatArrayOf(grow0.value, grow1.value, grow2.value)
        for (i in 0..2) {
            val p = pressProgress[i]
            if (p == 0f) continue
            when (i) {
                0 -> {
                    val g = min((p * ratio * base[0]).roundToInt(), widths[1])
                    widths[1] -= g
                    widths[0] += g
                }
                1 -> {
                    val g = (p * ratio * base[1] / 2f).roundToInt()
                    val left = min(g, widths[0])
                    val right = min(g, widths[2])
                    widths[0] -= left
                    widths[2] -= right
                    widths[1] += left + right
                }
                else -> {
                    val g = min((p * ratio * base[2]).roundToInt(), widths[1])
                    widths[1] -= g
                    widths[2] += g
                }
            }
        }
        val placeables = measurables.mapIndexed { index, measurable ->
            measurable.measure(Constraints.fixed(widths[index].coerceAtLeast(0), buttonHeight))
        }
        layout(total, buttonHeight) {
            var x = 0
            placeables.forEachIndexed { index, placeable ->
                placeable.placeRelative(x, 0)
                x += widths[index] + spacing
            }
        }
    }
}

/** Boton grande de Material 3: relleno suave, esquinas 28 dp que se cierran a 12 dp al presionar (resorte con rebote). */
@Composable
private fun SpatialTransportButton(
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector,
    iconSize: Dp,
    contentDescription: String
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 12.dp else 28.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "SpatialCorner"
    )
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxSize(),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            M3Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
