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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import android.graphics.Bitmap
import android.provider.Settings
import android.util.LruCache
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.player.SpatialFallbackColors
import chromahub.rhythm.app.shared.presentation.components.player.SpatialIcons
import chromahub.rhythm.app.shared.presentation.components.player.SpatialFontFamily
import chromahub.rhythm.app.shared.presentation.components.player.SpatialWavySlider
import chromahub.rhythm.app.shared.presentation.components.player.spatialAccentColor
import chromahub.rhythm.app.shared.presentation.components.player.spatialColorsFromBitmap
import chromahub.rhythm.app.shared.presentation.components.player.spatialMarquee
import chromahub.rhythm.app.shared.presentation.components.player.spatialSurfaceColor
import chromahub.rhythm.app.util.ImageUtils
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.material3.Icon as M3Icon

/** Estilos de texto: SpatialFlow muestra todo en peso regular con la fuente Google Sans Flex redondeada. */
private object SpatialStyles {
    val header = TextStyle(fontFamily = SpatialFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp)
    val title = TextStyle(fontFamily = SpatialFontFamily, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp)
    val artist = TextStyle(fontFamily = SpatialFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp)
    val chip = TextStyle(fontFamily = SpatialFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
    val time = TextStyle(fontFamily = SpatialFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp)
    val badge = TextStyle(fontFamily = SpatialFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
}


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
    swipeToDismissEnabled: Boolean = true,
    previousArtworkUri: Uri? = null,
    nextArtworkUri: Uri? = null,
    /** false cuando el reproductor esta replegado (solo se ve el mini reproductor): el fondo deja de animarse. */
    isVisible: Boolean = true
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
    val formatLabel = remember(song?.id) {
        song?.codec?.takeIf { it.isNotBlank() }?.uppercase()
            ?: song?.path?.substringAfterLast('.', "")
                ?.takeIf { it.length in 2..5 }
                ?.uppercase()
    }
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(background.value) }
            .then(swipeModifier)
    ) {
        // ---- Fondo: la misma portada, muy difuminada (se calcula una sola vez por cancion) ----
        SpatialBackdrop(artworkUri = artworkUri, isDark = isDark, isVisible = isVisible)

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
        val density = LocalDensity.current
        val statusBarTop = with(density) { WindowInsets.statusBars.getTop(this).toDp() }
        val navBarBottom = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }

        // Medidas EXACTAS de SpatialFlow (FullPlayer): portada = 90% del ancho; los demas elementos conservan sus
        // alturas y espacios. Igual que en SpatialFlow, si la pantalla es baja los botones de abajo se aprietan.
        val albumArtSize = maxWidth * 0.9f
        val coverPx = with(density) { albumArtSize.roundToPx() }
        // Precarga la portada de la cancion anterior y de la siguiente, para que al cambiar aparezca al instante.
        LaunchedEffect(previousArtworkUri, nextArtworkUri, coverPx, isDark) {
            delay(300)
            spatialPrefetchCover(context, previousArtworkUri, coverPx)
            spatialPrefetchCover(context, nextArtworkUri, coverPx)
            spatialPrefetchAurora(context, previousArtworkUri, isDark)
            spatialPrefetchAurora(context, nextArtworkUri, isDark)
        }
        val screenHeight = maxHeight + statusBarTop + navBarBottom
        val minTopOffset = statusBarTop + 68.dp
        val topOffset = ((screenHeight - albumArtSize) / 2f - 220.dp).coerceAtLeast(minTopOffset)
        val topGap = (topOffset - minTopOffset).coerceAtLeast(0.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    style = SpatialStyles.header,
                    color = contentSecondary
                )
                Spacer(modifier = Modifier.size(48.dp))
            }

            if (topGap > 0.dp) {
                Spacer(modifier = Modifier.height(topGap))
            }

            // ---- Portada ----
            val artShape = RoundedCornerShape(22.dp)
            Box(
                modifier = Modifier
                    .size(albumArtSize)
                    .shadow(16.dp, artShape)
                    .clip(artShape)
            ) {
                if (artworkUri != null) {
                    SpatialCover(
                        artworkUri = artworkUri,
                        contentDescription = song?.album,
                        placeholderColor = contentColor.copy(alpha = 0.10f),
                        iconTint = contentSecondary,
                        sizePx = coverPx,
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
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song?.title ?: stringResource(R.string.spatial_nothing_playing),
                        style = SpatialStyles.title,
                        color = contentColor,
                        maxLines = 1,
                        modifier = Modifier
                            .spatialMarquee()
                            .clickable { onSongInfoClick() }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song?.artist ?: "",
                        style = SpatialStyles.artist,
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
                Spacer(modifier = Modifier.width(8.dp))

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
                    label = stringResource(R.string.spatial_queue),
                    symbol = "queue_music",
                    onClick = onQueueClick,
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

                Spacer(modifier = Modifier.width(8.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ---- Barra ondulada + tiempos (unico lugar que lee el progreso) ----
            SpatialProgressSection(
                formatLabel = formatLabel,
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
}

// ---------------------------------------------------------------------------------------------
// Portada: se decodifica aqui (no con AsyncImage) y se guarda en un cache pequeno. Asi, al cambiar de cancion la
// portada nueva se pinta en el MISMO cuadro que el texto y nunca se ve la portada de la cancion anterior.
// ---------------------------------------------------------------------------------------------

/** Portadas ya decodificadas: la actual y las vecinas. */
private val spatialCoverCache = LruCache<String, ImageBitmap>(4)

private suspend fun spatialLoadCover(context: android.content.Context, uri: Uri, sizePx: Int): ImageBitmap? =
    withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(sizePx)
                .scale(Scale.FILL)
                .precision(Precision.INEXACT)
                .build()
            val result = coil.Coil.imageLoader(context).execute(request)
            (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap?.asImageBitmap()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

/** Deja lista en el cache la portada de [uri] (se usa para la cancion anterior y la siguiente). */
private suspend fun spatialPrefetchCover(context: android.content.Context, uri: Uri?, sizePx: Int) {
    if (uri == null) return
    val key = uri.toString()
    if (spatialCoverCache.get(key) != null) return
    val loaded = spatialLoadCover(context, uri, sizePx)
    if (loaded != null) spatialCoverCache.put(key, loaded)
}

@Composable
private fun SpatialCover(
    artworkUri: Uri,
    contentDescription: String?,
    placeholderColor: Color,
    iconTint: Color,
    sizePx: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val key = artworkUri.toString()
    // remember(key): al cambiar de cancion el estado se reinicia, asi nunca queda dibujada la portada anterior.
    var image by remember(key) { mutableStateOf<ImageBitmap?>(spatialCoverCache.get(key)) }
    var failed by remember(key) { mutableStateOf(false) }
    val coverAlpha = remember(key) { Animatable(if (image != null) 1f else 0f) }
    LaunchedEffect(key) {
        if (image == null) {
            val loaded = spatialLoadCover(context, artworkUri, sizePx)
            if (loaded != null) {
                spatialCoverCache.put(key, loaded)
                image = loaded
                coverAlpha.animateTo(1f, tween(140))
            } else {
                failed = true
            }
        }
    }
    Box(modifier = modifier.background(placeholderColor), contentAlignment = Alignment.Center) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = coverAlpha.value }
            )
        } else if (failed) {
            M3Icon(
                imageVector = SpatialIcons.MusicNote,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(96.dp)
            )
        }
    }
}

/** Un paso de desenfoque de caja (horizontal y vertical) sobre un canal; el resultado queda en [data]. */
private fun spatialBoxBlur(data: IntArray, tmp: IntArray, w: Int, h: Int, r: Int) {
    val div = 2 * r + 1
    for (y in 0 until h) {
        val row = y * w
        var sum = 0
        for (i in -r..r) sum += data[row + i.coerceIn(0, w - 1)]
        for (x in 0 until w) {
            tmp[row + x] = (sum + div / 2) / div
            sum += data[row + (x + r + 1).coerceAtMost(w - 1)] - data[row + (x - r).coerceAtLeast(0)]
        }
    }
    for (x in 0 until w) {
        var sum = 0
        for (i in -r..r) sum += tmp[i.coerceIn(0, h - 1) * w + x]
        for (y in 0 until h) {
            data[y * w + x] = (sum + div / 2) / div
            sum += tmp[(y + r + 1).coerceAtMost(h - 1) * w + x] - tmp[(y - r).coerceAtLeast(0) * w + x]
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Fondo "aurora" - FASE 1: extraccion de colores (sin movimiento, sin luces animadas).
// SOLO colores de la portada. Una vez por cancion (en segundo plano) se calcula:
//  - un campo de color de 64x112: cada zona toma UN matiz real de la portada (nunca se promedian matices opuestos,
//    asi no salen grises ni manchas sucias) con colores vivos y agradables;
//  - tres colores base (del matiz principal) y dos luces (para la siguiente fase; aun no se dibujan).
// ---------------------------------------------------------------------------------------------

// Tamanos del calculo
private const val AUR_SRC = 128
private const val AUR_GW = 32
private const val AUR_GH = 56
private const val AUR_OUT_W = 64
private const val AUR_OUT_H = 112

// Ajustes de color (tema oscuro). Aqui se retoca el aspecto sin tocar el resto del codigo.
private const val AUR_SAT_MIN = 0.70f        // saturacion minima de las zonas de color (mas alto = mas vivo)
private const val AUR_SAT_GAIN = 1.5f
private const val AUR_SAT_ADD = 0.18f
private const val AUR_VAL_BASE = 0.30f
private const val AUR_VAL_GAIN = 0.55f
private const val AUR_VAL_MIN = 0.42f        // brillo minimo / maximo de las zonas de color
private const val AUR_VAL_MAX = 0.88f
private const val AUR_LUM_CAP_FIELD = 0.27f  // luminancia maxima del campo (amarillos y verdes no quedan blancos y el texto se lee)
private const val AUR_LUM_CAP_LIGHT = 0.30f  // luminancia maxima de las luces
private const val AUR_GEXP = 0.2f            // cuanto pesa el matiz dominante (menor = mas variedad de matices)
private const val AUR_WIDE_MIX = 0.10f
private const val AUR_S0 = 0.05f             // los grises/blancos casi no cuentan: rampa de saturacion
private const val AUR_S1 = 0.25f
private const val AUR_NEIGHBORS = 0.4f
private const val AUR_SHADE_START = 0.35f    // desde esta altura (0..1) el campo se oscurece suavemente...
private const val AUR_SHADE_END = 0.62f      // ...hasta este factor abajo (mismo matiz, solo mas oscuro; no es negro)
private const val AUR_MAX_HUE_SPAN = 110f   // matices mas lejos que esto del principal (opuestos) se descartan: mezclados ensucian

private data class SpatialAurora(
    val field: ImageBitmap,
    val top: Color,
    val mid: Color,
    val deep: Color,
    val lightA: Color,
    val lightB: Color
)

private fun auroraRamp(x: Float, a: Float, b: Float): Float {
    val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun auroraLinear(c: Float): Float =
    if (c <= 0.04045f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()

private fun auroraLuminance(r: Float, g: Float, b: Float): Float =
    0.2126f * auroraLinear(r) + 0.7152f * auroraLinear(g) + 0.0722f * auroraLinear(b)

/** HSV (h 0..360, s y v 0..1) -> RGB 0..1 en out[0..2]. */
private fun auroraHsv(h: Float, s: Float, v: Float, out: FloatArray) {
    val hh = ((h % 360f) + 360f) % 360f
    val c = v * s
    val x = c * (1f - abs((hh / 60f) % 2f - 1f))
    val m = v - c
    var r = 0f
    var g = 0f
    var b = 0f
    when ((hh / 60f).toInt() % 6) {
        0 -> { r = c; g = x }
        1 -> { r = x; g = c }
        2 -> { g = c; b = x }
        3 -> { g = x; b = c }
        4 -> { r = x; b = c }
        else -> { r = c; b = x }
    }
    out[0] = r + m
    out[1] = g + m
    out[2] = b + m
}

/** Igual que [auroraHsv], pero baja el brillo hasta que la luminancia no pase de [cap] (amarillos y cianes no quedan blancos). */
private fun auroraHsvCapped(h: Float, s: Float, v: Float, cap: Float, out: FloatArray) {
    var value = v
    repeat(14) {
        auroraHsv(h, s, value, out)
        if (auroraLuminance(out[0], out[1], out[2]) <= cap) return
        value *= 0.93f
    }
    auroraHsv(h, s, value, out)
}

private fun auroraHueDistance(a: Float, b: Float): Float {
    val d = abs(a - b) % 360f
    return minOf(d, 360f - d)
}

private fun auroraMix(c: Color, gray: Float, k: Float): Color =
    Color(
        red = (gray * (1f - k) + c.red * k).coerceIn(0f, 1f),
        green = (gray * (1f - k) + c.green * k).coerceIn(0f, 1f),
        blue = (gray * (1f - k) + c.blue * k).coerceIn(0f, 1f)
    )

/** Desenfoque de caja (borde fijo), [passes] pasadas, sobre una rejilla w x h de decimales. */
private fun auroraBlur(data: FloatArray, tmp: FloatArray, w: Int, h: Int, r: Int, passes: Int) {
    val div = (2 * r + 1).toFloat()
    repeat(passes) {
        for (y in 0 until h) {
            val row = y * w
            var sum = 0f
            for (i in -r..r) sum += data[row + i.coerceIn(0, w - 1)]
            for (x in 0 until w) {
                tmp[row + x] = sum / div
                sum += data[row + (x + r + 1).coerceAtMost(w - 1)] - data[row + (x - r).coerceAtLeast(0)]
            }
        }
        for (x in 0 until w) {
            var sum = 0f
            for (i in -r..r) sum += tmp[i.coerceIn(0, h - 1) * w + x]
            for (y in 0 until h) {
                data[y * w + x] = sum / div
                sum += tmp[(y + r + 1).coerceAtMost(h - 1) * w + x] - tmp[(y - r).coerceAtLeast(0) * w + x]
            }
        }
    }
}

/** Convierte la portada en el fondo aurora (campo de color + colores base + luces). Todo con colores de la portada. */
private fun spatialAuroraFromBitmap(source: Bitmap, isDark: Boolean): SpatialAurora {
    val sw = AUR_SRC
    val sh = AUR_SRC
    val n = sw * sh

    // 1) Portada reducida a 128x128 (sirve tambien con bitmaps "hardware")
    val scaled = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
    run {
        val canvas = android.graphics.Canvas(scaled)
        canvas.drawColor(android.graphics.Color.BLACK)
        val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, null, android.graphics.Rect(0, 0, sw, sh), paint)
    }
    val px = IntArray(n)
    scaled.getPixels(px, 0, sw, 0, 0, sw, sh)

    // 2) Por pixel: matiz y peso. Blancos, grises y negros casi no pesan (evita fondos "sucios").
    val hue = FloatArray(n)
    val wgt = FloatArray(n)
    val rr = FloatArray(n)
    val gg = FloatArray(n)
    val bb = FloatArray(n)
    val hsv = FloatArray(3)
    var wSum = 0f
    var rSum = 0f
    var gSum = 0f
    var bSum = 0f
    for (i in 0 until n) {
        val c = px[i]
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        android.graphics.Color.RGBToHSV(r, g, b, hsv)
        rr[i] = r / 255f
        gg[i] = g / 255f
        bb[i] = b / 255f
        hue[i] = hsv[0]
        val w = auroraRamp(hsv[1], AUR_S0, AUR_S1) * auroraRamp(hsv[2], 0.04f, 0.20f)
        wgt[i] = w
        wSum += w
        rSum += rr[i]
        gSum += gg[i]
        bSum += bb[i]
    }
    // Que tan colorida es la portada (0 = casi gris, 1 = colorida)
    val vivid = (wSum / n / 0.12f).coerceIn(0f, 1f)

    // 2b) Matices principales (para luces y base): los mas presentes en la zona central de la portada (sin bordes, marcos ni logos)
    val tot = FloatArray(12)
    val sinSum = FloatArray(12)
    val cosSum = FloatArray(12)
    for (y in (0.06f * sh).toInt() until (0.78f * sh).toInt()) {
        for (x in (0.12f * sw).toInt() until (0.88f * sw).toInt()) {
            val i = y * sw + x
            val w = wgt[i]
            if (w <= 0f) continue
            val k = (hue[i] / 30f).toInt().coerceIn(0, 11)
            val ang = Math.toRadians(hue[i].toDouble())
            tot[k] += w
            sinSum[k] += w * sin(ang).toFloat()
            cosSum[k] += w * cos(ang).toFloat()
        }
    }
    val totalW = tot.sum()
    val order = (0 until 12).sortedByDescending { tot[it] }
    val hues = ArrayList<Float>(3)
    for (k in order) {
        if (hues.isNotEmpty() && tot[k] <= 0.02f * maxOf(totalW, 1e-9f)) continue
        val mean = if (tot[k] > 0f) {
            ((Math.toDegrees(atan2(sinSum[k].toDouble(), cosSum[k].toDouble())).toFloat() % 360f) + 360f) % 360f
        } else {
            k * 30f + 15f
        }
        // Distintos entre si (>= 28 grados) y no opuestos al matiz principal (los opuestos ensucian al mezclarse)
        val distinct = hues.all { auroraHueDistance(mean, it) >= 28f }
        val nearMain = hues.isEmpty() || auroraHueDistance(mean, hues[0]) <= AUR_MAX_HUE_SPAN
        if (distinct && nearMain) hues.add(mean)
        if (hues.size == 3) break
    }
    if (hues.isEmpty()) hues.add(0f)
    val main = hues[0]
    val pads = floatArrayOf((main + 20f) % 360f, (main - 20f + 360f) % 360f)
    while (hues.size < 3) hues.add(pads[hues.size - 1])

    // 3) Acumular peso y color por celda y por grupo de matiz (12 grupos de 30 grados)
    val cells = AUR_GW * AUR_GH
    val acc = Array(4) { FloatArray(12 * cells) }   // 0 = peso, 1..3 = color ponderado
    val avg = Array(3) { FloatArray(cells) }        // promedio simple por celda (para portadas casi grises)
    val cnt = FloatArray(cells)
    val sinAll = FloatArray(12)
    val cosAll = FloatArray(12)
    for (y in 0 until sh) {
        val cy = ((y * AUR_GH) / sh).coerceAtMost(AUR_GH - 1)
        for (x in 0 until sw) {
            val i = y * sw + x
            val cx = ((x * AUR_GW) / sw).coerceAtMost(AUR_GW - 1)
            val cell = cy * AUR_GW + cx
            avg[0][cell] += rr[i]
            avg[1][cell] += gg[i]
            avg[2][cell] += bb[i]
            cnt[cell] += 1f
            val w = wgt[i]
            if (w <= 0f) continue
            val bin = (hue[i] / 30f).toInt().coerceIn(0, 11)
            val ang = Math.toRadians(hue[i].toDouble())
            sinAll[bin] += w * sin(ang).toFloat()
            cosAll[bin] += w * cos(ang).toFloat()
            val idx = bin * cells + cell
            acc[0][idx] += w
            acc[1][idx] += w * rr[i]
            acc[2][idx] += w * gg[i]
            acc[3][idx] += w * bb[i]
        }
    }
    val glob = FloatArray(12)
    for (k in 0 until 12) {
        var s = 0f
        for (c in 0 until cells) s += acc[0][k * cells + c]
        glob[k] = s
    }
    val globMax = glob.maxOrNull() ?: 0f
    val gpow = FloatArray(12) {
        if (globMax > 0f) Math.pow((glob[it] / globMax).toDouble(), AUR_GEXP.toDouble()).toFloat() else 0f
    }
    // Los matices opuestos al principal no entran al campo de color (mezclados con el principal dan gris o verde sucio)
    for (k in 0 until 12) {
        if (glob[k] <= 0f) continue
        val binHue = ((Math.toDegrees(atan2(sinAll[k].toDouble(), cosAll[k].toDouble())).toFloat() % 360f) + 360f) % 360f
        if (auroraHueDistance(binHue, main) > AUR_MAX_HUE_SPAN) gpow[k] = 0f
    }

    // 4) Desenfoque local (r=3) y amplio (r=8) de cada grupo
    val tmp = FloatArray(cells)
    val local = Array(4) { FloatArray(12 * cells) }
    val wide = Array(4) { FloatArray(12 * cells) }
    val buf = FloatArray(cells)
    for (ch in 0 until 4) {
        for (k in 0 until 12) {
            System.arraycopy(acc[ch], k * cells, buf, 0, cells)
            auroraBlur(buf, tmp, AUR_GW, AUR_GH, 3, 3)
            System.arraycopy(buf, 0, local[ch], k * cells, cells)
            System.arraycopy(acc[ch], k * cells, buf, 0, cells)
            auroraBlur(buf, tmp, AUR_GW, AUR_GH, 8, 3)
            System.arraycopy(buf, 0, wide[ch], k * cells, cells)
        }
    }

    // 5) Colores de portadas casi grises: el promedio desenfocado de la propia portada
    for (ch in 0 until 3) {
        for (c in 0 until cells) avg[ch][c] = avg[ch][c] / maxOf(cnt[c], 1f)
        auroraBlur(avg[ch], tmp, AUR_GW, AUR_GH, 3, 3)
    }

    // 6) Cada celda "vota" por UN matiz (el suyo + la mitad de los vecinos, al cuadrado, por el peso global).
    //    Nunca se promedian colores de matices distintos.
    val field = Array(3) { FloatArray(cells) }
    val own = FloatArray(12)
    val rgb = FloatArray(3)
    val grayRgb = FloatArray(3)
    for (c in 0 until cells) {
        // Oscurecimiento suave hacia abajo (mismo matiz, solo mas oscuro) para que se lean los botones y los tiempos
        val rowT = ((c / AUR_GW) / (AUR_GH - 1f) - AUR_SHADE_START) / (1f - AUR_SHADE_START)
        val t = rowT.coerceIn(0f, 1f)
        val shade = 1f + (AUR_SHADE_END - 1f) * (t * t * (3f - 2f * t))
        for (k in 0 until 12) own[k] = local[0][k * cells + c] + AUR_WIDE_MIX * wide[0][k * cells + c]
        var best = 0
        var bestScore = -1f
        for (k in 0 until 12) {
            val a = own[k] + AUR_NEIGHBORS * (own[(k + 11) % 12] + own[(k + 1) % 12])
            val s = a * a * gpow[k]
            if (s > bestScore) {
                bestScore = s
                best = k
            }
        }
        val den = own[best] + 1e-9f
        val r = ((local[1][best * cells + c] + AUR_WIDE_MIX * wide[1][best * cells + c]) / den).coerceIn(0f, 1f)
        val g = ((local[2][best * cells + c] + AUR_WIDE_MIX * wide[2][best * cells + c]) / den).coerceIn(0f, 1f)
        val b = ((local[3][best * cells + c] + AUR_WIDE_MIX * wide[3][best * cells + c]) / den).coerceIn(0f, 1f)
        android.graphics.Color.RGBToHSV((r * 255f + 0.5f).toInt(), (g * 255f + 0.5f).toInt(), (b * 255f + 0.5f).toInt(), hsv)
        val boosted = (hsv[1] * AUR_SAT_GAIN + AUR_SAT_ADD).coerceIn(AUR_SAT_MIN, 1f)
        val sat = hsv[1] + (boosted - hsv[1]) * vivid
        if (isDark) {
            val v = (AUR_VAL_BASE + AUR_VAL_GAIN * hsv[2]).coerceIn(AUR_VAL_MIN, AUR_VAL_MAX) * shade
            auroraHsvCapped(hsv[0], sat, v, AUR_LUM_CAP_FIELD, rgb)
        } else {
            val v = (0.80f + 0.15f * hsv[2]).coerceIn(0.80f, 0.97f)
            auroraHsv(hsv[0], (sat * 0.55f).coerceIn(0f, 0.60f), v, rgb)
        }
        // Portada casi sin color: se mezcla hacia el promedio desenfocado de la propia portada
        if (vivid < 1f) {
            android.graphics.Color.RGBToHSV(
                (avg[0][c] * 255f + 0.5f).toInt(), (avg[1][c] * 255f + 0.5f).toInt(), (avg[2][c] * 255f + 0.5f).toInt(), hsv
            )
            if (isDark) {
                auroraHsvCapped(hsv[0], hsv[1], (hsv[2] * 0.55f).coerceIn(0.08f, 0.60f) * shade, AUR_LUM_CAP_FIELD, grayRgb)
            } else {
                auroraHsv(hsv[0], hsv[1] * 0.4f, (0.80f + 0.15f * hsv[2]).coerceIn(0.80f, 0.95f), grayRgb)
            }
            for (ch in 0 until 3) rgb[ch] = grayRgb[ch] + (rgb[ch] - grayRgb[ch]) * vivid
        }
        field[0][c] = rgb[0]
        field[1][c] = rgb[1]
        field[2][c] = rgb[2]
    }
    for (ch in 0 until 3) auroraBlur(field[ch], tmp, AUR_GW, AUR_GH, 2, 3)

    // 7) Bitmap final: se agranda a 64x112 y se suaviza otra vez, asi no se ven pixeles al estirarlo a pantalla
    val cellBitmap = Bitmap.createBitmap(AUR_GW, AUR_GH, Bitmap.Config.ARGB_8888)
    val cellPixels = IntArray(cells) {
        val r = (field[0][it] * 255f + 0.5f).toInt().coerceIn(0, 255)
        val g = (field[1][it] * 255f + 0.5f).toInt().coerceIn(0, 255)
        val b = (field[2][it] * 255f + 0.5f).toInt().coerceIn(0, 255)
        (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
    cellBitmap.setPixels(cellPixels, 0, AUR_GW, 0, 0, AUR_GW, AUR_GH)
    val out = Bitmap.createScaledBitmap(cellBitmap, AUR_OUT_W, AUR_OUT_H, true)
    val outN = AUR_OUT_W * AUR_OUT_H
    val outPixels = IntArray(outN)
    out.getPixels(outPixels, 0, AUR_OUT_W, 0, 0, AUR_OUT_W, AUR_OUT_H)
    val red = IntArray(outN) { (outPixels[it] shr 16) and 0xFF }
    val green = IntArray(outN) { (outPixels[it] shr 8) and 0xFF }
    val blue = IntArray(outN) { outPixels[it] and 0xFF }
    val outTmp = IntArray(outN)
    repeat(2) {
        spatialBoxBlur(red, outTmp, AUR_OUT_W, AUR_OUT_H, 3)
        spatialBoxBlur(green, outTmp, AUR_OUT_W, AUR_OUT_H, 3)
        spatialBoxBlur(blue, outTmp, AUR_OUT_W, AUR_OUT_H, 3)
    }
    for (i in 0 until outN) outPixels[i] = (0xFF shl 24) or (red[i] shl 16) or (green[i] shl 8) or blue[i]
    val finalBitmap = Bitmap.createBitmap(AUR_OUT_W, AUR_OUT_H, Bitmap.Config.ARGB_8888)
    finalBitmap.setPixels(outPixels, 0, AUR_OUT_W, 0, 0, AUR_OUT_W, AUR_OUT_H)

    // 9) Colores base (del matiz principal) y luces
    val tmpRgb = FloatArray(3)
    fun colorOf(h: Float, s: Float, v: Float, cap: Float): Color {
        if (isDark) auroraHsvCapped(h, s, v, cap, tmpRgb) else auroraHsv(h, s, v, tmpRgb)
        return Color(tmpRgb[0].coerceIn(0f, 1f), tmpRgb[1].coerceIn(0f, 1f), tmpRgb[2].coerceIn(0f, 1f))
    }
    var top = if (isDark) colorOf(main, 0.85f, 0.60f, 0.20f) else colorOf(main, 0.25f, 1.00f, 1f)
    var mid = if (isDark) colorOf(main, 0.85f, 0.42f, 0.20f) else colorOf(main, 0.30f, 0.96f, 1f)
    var deep = if (isDark) colorOf(main, 0.85f, 0.32f, 0.20f) else colorOf(main, 0.35f, 0.92f, 1f)
    var lightA = if (isDark) colorOf(hues[1], 0.75f, 1f, AUR_LUM_CAP_LIGHT) else colorOf(hues[1], 0.45f, 1f, 1f)
    var lightB = if (isDark) colorOf(hues[2], 0.75f, 1f, AUR_LUM_CAP_LIGHT) else colorOf(hues[2], 0.45f, 1f, 1f)
    if (vivid < 1f) {
        // Portada casi gris: base y luces en el gris de la propia portada
        val gray = (rSum + gSum + bSum) / (3f * n)
        val grayBase = if (isDark) (gray * 0.8f).coerceIn(0.10f, 0.30f) else (0.80f + 0.15f * gray).coerceIn(0.80f, 0.95f)
        top = auroraMix(top, grayBase, vivid)
        mid = auroraMix(mid, grayBase * 0.85f, vivid)
        deep = auroraMix(deep, grayBase * 0.70f, vivid)
        lightA = auroraMix(lightA, (grayBase * 1.6f).coerceAtMost(1f), vivid)
        lightB = auroraMix(lightB, (grayBase * 1.6f).coerceAtMost(1f), vivid)
    }

    return SpatialAurora(
        field = finalBitmap.asImageBitmap(),
        top = top,
        mid = mid,
        deep = deep,
        lightA = lightA,
        lightB = lightB
    )
}

/** Fondos ya calculados: la cancion actual y las vecinas. */
private val spatialAuroraCache = LruCache<String, SpatialAurora>(8)

private suspend fun spatialLoadAurora(context: android.content.Context, uri: Uri, isDark: Boolean): SpatialAurora? {
    val key = uri.toString() + if (isDark) "|d" else "|l"
    spatialAuroraCache.get(key)?.let { return it }
    return withContext(Dispatchers.Default) {
        try {
            val bitmap = withContext(Dispatchers.IO) { ImageUtils.loadArtworkBitmap(context, uri, AUR_SRC) }
            if (bitmap != null) {
                spatialAuroraFromBitmap(bitmap, isDark).also { spatialAuroraCache.put(key, it) }
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }
}

/** Deja listo el fondo de [uri] (cancion anterior y siguiente) para que al cambiar de cancion aparezca al instante. */
private suspend fun spatialPrefetchAurora(context: android.content.Context, uri: Uri?, isDark: Boolean) {
    if (uri == null) return
    spatialLoadAurora(context, uri, isDark)
}

// ---- Fase 2: movimiento y orbes de luz (ajustables aqui) ----
private const val AUR_PERIOD_MS = 48_000f        // vuelta completa del reloj; cada movimiento es un numero ENTERO de ciclos de esta vuelta, asi el bucle cierra sin saltos
private const val AUR_FRAME_NS = 30_000_000L     // ~30 fps: se ve igual de suave en un fondo difuso y gasta la mitad que 60
private const val AUR_FIELD_OVERSCAN = 1.20f     // el campo se dibuja 20 % mas grande que la pantalla para poder moverlo sin que asomen bordes
private const val AUR_DRIFT_X = 0.070f           // vaiven horizontal del campo (fraccion del ancho); 2 ciclos = 24 s por ciclo
private const val AUR_DRIFT_Y = 0.035f           // vaiven vertical del campo (fraccion del alto); 3 ciclos = 16 s por ciclo
private const val AUR_ORB_A_RADIUS = 0.75f       // radio de los orbes (fraccion del ancho)
private const val AUR_ORB_B_RADIUS = 0.60f
private const val AUR_ORB_A_ALPHA = 0.52f        // fuerza del orbe en su centro
private const val AUR_ORB_B_ALPHA = 0.42f
private const val AUR_TWO_PI = 6.2831855f

/** Seno de [n] ciclos enteros por vuelta del reloj (n entero = el movimiento cierra exacto al terminar la vuelta). */
private fun auroraCycle(phase: Float, n: Int, offset: Float = 0f): Float =
    sin(AUR_TWO_PI * (n * phase + offset))

/** Orbe de luz: el mismo color de la portada, fuerte al centro y desvaneciendose (misma tinta al desvanecer: no ensucia). */
private fun auroraOrbBrush(color: Color, strength: Float, radius: Float): Brush = Brush.radialGradient(
    colorStops = arrayOf(
        0.00f to color.copy(alpha = strength),
        0.35f to color.copy(alpha = strength * 0.62f),
        0.70f to color.copy(alpha = strength * 0.18f),
        1.00f to color.copy(alpha = 0f)
    ),
    center = Offset.Zero,
    radius = radius
)

/** true mientras la pantalla esta encendida y la app a la vista (ON_START..ON_STOP). */
@Composable
private fun rememberLifecycleStarted(): Boolean {
    val owner = LocalLifecycleOwner.current
    var started by remember(owner) {
        mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) started = true
            else if (event == Lifecycle.Event.ON_STOP) started = false
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return started
}

/**
 * Reloj unico del fondo (0..1 por vuelta). Solo corre mientras [running]; al parar se queda donde estaba y al volver sigue desde ahi.
 * Nadie lo lee en composicion: solo se lee al dibujar, asi cada tic redibuja unicamente el fondo (nada se recompone).
 */
@Composable
private fun rememberAuroraPhase(running: Boolean): FloatState {
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = -1L
        var elapsed = phase.floatValue * AUR_PERIOD_MS
        while (true) {
            withFrameNanos { now ->
                if (last < 0L) {
                    last = now
                } else if (now - last >= AUR_FRAME_NS) {
                    elapsed = (elapsed + (now - last) / 1_000_000f) % AUR_PERIOD_MS
                    last = now
                    phase.floatValue = elapsed / AUR_PERIOD_MS
                }
            }
        }
    }
    return phase
}

@Composable
private fun SpatialAuroraLayer(aurora: SpatialAurora, phase: FloatState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .graphicsLayer { }   // capa propia: al animar solo se redibuja el fondo, no todo el reproductor
            .drawWithCache {
                val w = size.width
                val h = size.height
                val baseBrush = Brush.verticalGradient(listOf(aurora.top, aurora.mid, aurora.deep))
                // campo mas grande que la pantalla y centrado (para poder moverlo sin mostrar bordes)
                val fieldW = (w * AUR_FIELD_OVERSCAN).roundToInt()
                val fieldH = (h * AUR_FIELD_OVERSCAN).roundToInt()
                val fieldSize = IntSize(fieldW, fieldH)
                val fieldOffset = IntOffset(-((fieldW - w.roundToInt()) / 2), -((fieldH - h.roundToInt()) / 2))
                val radiusA = w * AUR_ORB_A_RADIUS
                val radiusB = w * AUR_ORB_B_RADIUS
                val orbA = auroraOrbBrush(aurora.lightA, AUR_ORB_A_ALPHA, radiusA)
                val orbB = auroraOrbBrush(aurora.lightB, AUR_ORB_B_ALPHA, radiusB)
                onDrawBehind {
                    val p = phase.floatValue   // se lee aqui (fase de dibujo): el tic no recompone nada
                    // 1) degradado base del matiz principal
                    drawRect(brush = baseBrush)
                    // 2) campo de color de la portada con vaiven suave (seno: sin aceleraciones raras)
                    val dx = w * AUR_DRIFT_X * auroraCycle(p, 2)
                    val dy = h * AUR_DRIFT_Y * auroraCycle(p, 3, 0.25f)
                    withTransform({ translate(left = dx, top = dy) }) {
                        drawImage(
                            image = aurora.field,
                            dstOffset = fieldOffset,
                            dstSize = fieldSize,
                            alpha = 0.92f,
                            filterQuality = FilterQuality.Low
                        )
                    }
                    // 3) orbe A: franja superior (sobre la portada), recorre el ancho y "respira"
                    val breathe = 1f + 0.10f * auroraCycle(p, 3)
                    val ax = w * (0.5f + 0.34f * auroraCycle(p, 1))
                    val ay = h * (0.14f + 0.07f * auroraCycle(p, 2, 0.25f))
                    withTransform({
                        translate(left = ax, top = ay)
                        scale(scaleX = breathe, scaleY = breathe, pivot = Offset.Zero)
                    }) {
                        drawCircle(brush = orbA, radius = radiusA, center = Offset.Zero)
                    }
                    // 4) orbe B: zona bajo la portada, va al reves
                    val breatheB = 2f - breathe
                    val bx = w * (0.5f - 0.30f * auroraCycle(p, 2, 0.1f))
                    val by = h * (0.64f + 0.08f * auroraCycle(p, 1, 0.5f))
                    withTransform({
                        translate(left = bx, top = by)
                        scale(scaleX = breatheB, scaleY = breatheB, pivot = Offset.Zero)
                    }) {
                        drawCircle(brush = orbB, radius = radiusB, center = Offset.Zero)
                    }
                }
            }
    )
}

/**
 * Fondo del reproductor con los colores de la portada. Cambia con fundido cruzado al cambiar de cancion.
 * Se mueve solo si el reproductor se ve ([isVisible]), la app esta a la vista y el sistema no tiene las animaciones apagadas.
 */
@Composable
private fun SpatialBackdrop(artworkUri: Uri?, isDark: Boolean, isVisible: Boolean) {
    val context = LocalContext.current
    val aurora by produceState<SpatialAurora?>(initialValue = null, artworkUri, isDark) {
        value = if (artworkUri == null) null else spatialLoadAurora(context, artworkUri, isDark)
    }
    val animationsOn = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    val started = rememberLifecycleStarted()
    val phase = rememberAuroraPhase(running = isVisible && started && animationsOn)
    Crossfade(
        targetState = aurora,
        animationSpec = tween(700),
        label = "SpatialAurora"
    ) { current ->
        if (current != null) {
            SpatialAuroraLayer(aurora = current, phase = phase, modifier = Modifier.fillMaxSize())
        }
    }
}

/** Tiempo como en SpatialFlow: siempre mm:ss con ceros (00:05). */
private fun spatialTime(ms: Long): String {
    val total = (ms / 1000L).coerceAtLeast(0L)
    return String.format(java.util.Locale.getDefault(), "%02d:%02d", total / 60L, total % 60L)
}

/** Barra ondulada + tiempo actual + formato + tiempo total. Es lo unico que se recompone con el progreso. */
@Composable
private fun SpatialProgressSection(
    formatLabel: String?,
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
    val currentText = remember(currentSeconds) { spatialTime(currentSeconds * 1000L) }
    val totalText = remember(showRemainingTime, totalSeconds, remainingSeconds) {
        if (showRemainingTime) "-" + spatialTime(remainingSeconds * 1000L)
        else spatialTime(totalSeconds * 1000L)
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentText,
                style = SpatialStyles.time,
                color = contentSecondary
            )
            if (formatLabel != null) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(contentColor.copy(alpha = fillAlpha))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        icon = MaterialSymbolIcon("audio_file"),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = contentSecondary,
                        size = 14.dp
                    )
                    Text(
                        text = formatLabel,
                        style = SpatialStyles.badge,
                        color = contentSecondary,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = totalText,
                style = SpatialStyles.time,
                color = contentSecondary,
                modifier = Modifier.clickable { onTotalTimeClick() }
            )
        }
    }
}

/**
 * "Liquid glass" barato (sin desenfoque en vivo, ligero para el Helio G99): relleno translucido que aclara el fondo
 * (mas arriba, mas tenue en medio, un poco mas abajo, como el grosor de un cristal) y, aparte, un borde luminoso en diagonal.
 * Solo dibuja degradados: el fondo se sigue viendo y el boton se distingue en cualquier portada.
 */
private const val SPATIAL_GLASS_FILL_SCALE = 0.50f   // intensidad del relleno (1 = muy llamativo, 0 = invisible)
private const val SPATIAL_GLASS_RIM_SCALE = 0.55f    // intensidad del borde luminoso

private fun spatialGlassFill(isDark: Boolean, tint: Color, strength: Float = 1f): Brush {
    val k = strength * SPATIAL_GLASS_FILL_SCALE
    val top = ((if (isDark) 0.24f else 0.58f) * k).coerceAtMost(1f)
    val mid = ((if (isDark) 0.09f else 0.30f) * k).coerceAtMost(1f)
    val bottom = ((if (isDark) 0.15f else 0.44f) * k).coerceAtMost(1f)
    return Brush.verticalGradient(
        0.00f to tint.copy(alpha = top),
        0.55f to tint.copy(alpha = mid),
        1.00f to tint.copy(alpha = bottom)
    )
}

/** Borde de cristal: brillo fuerte arriba a la izquierda, casi nada en medio y un reflejo menor abajo a la derecha. */
private fun spatialGlassRim(isDark: Boolean, tint: Color): Brush = Brush.linearGradient(
    0.00f to tint.copy(alpha = (if (isDark) 0.75f else 0.95f) * SPATIAL_GLASS_RIM_SCALE),
    0.35f to tint.copy(alpha = (if (isDark) 0.14f else 0.30f) * SPATIAL_GLASS_RIM_SCALE),
    0.65f to tint.copy(alpha = (if (isDark) 0.08f else 0.20f) * SPATIAL_GLASS_RIM_SCALE),
    1.00f to tint.copy(alpha = (if (isDark) 0.45f else 0.70f) * SPATIAL_GLASS_RIM_SCALE)
)

/** Pastilla redondeada con icono y texto (cristal liquido); se resalta con el color de acento cuando esta activa. */
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
    // Cristal liquido: relleno translucido claro + borde luminoso; el fondo se sigue viendo detras
    val glassFill = remember(isDark, isSelected, accentColor) {
        if (isSelected) spatialGlassFill(isDark, accentColor, 1.25f) else spatialGlassFill(isDark, Color.White)
    }
    val glassRim = remember(isDark, isSelected, accentColor) {
        spatialGlassRim(isDark, if (isSelected) accentColor else Color.White)
    }
    val tintColor = if (isSelected) accentColor else contentColor.copy(alpha = 0.95f)

    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(glassFill, CircleShape)
            .border(1.dp, glassRim, CircleShape)
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
                style = SpatialStyles.chip,
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

    val playContent = if (isDark) Color(0xFF1C1B1F) else Color.White

    Layout(
        content = {
            SpatialTransportButton(
                onClick = onPrevious,
                interactionSource = source0,
                containerColor = Color.Transparent,
                contentColor = contentColor,
                icon = SpatialIcons.SkipPrevious,
                iconSize = 36.dp,
                contentDescription = previousLabel,
                glass = true,
                isDark = isDark
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
                containerColor = Color.Transparent,
                contentColor = contentColor,
                icon = SpatialIcons.SkipNext,
                iconSize = 36.dp,
                contentDescription = nextLabel,
                glass = true,
                isDark = isDark
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) { measurables, constraints ->
        val spacing = 12.dp.roundToPx()
        val buttonHeight = min(76.dp.roundToPx(), constraints.maxHeight).coerceAtLeast(0)
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
    contentDescription: String,
    glass: Boolean = false,
    isDark: Boolean = true
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
    val glassFill = remember(isDark) { spatialGlassFill(isDark, Color.White) }
    val glassRim = remember(isDark) { spatialGlassRim(isDark, Color.White) }
    val shape = RoundedCornerShape(cornerRadius)
    Button(
        onClick = onClick,
        modifier = if (glass) Modifier.fillMaxSize().background(glassFill, shape) else Modifier.fillMaxSize(),
        interactionSource = interactionSource,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = if (glass) BorderStroke(1.dp, glassRim) else null,
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
