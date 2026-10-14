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
import android.util.LruCache
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
    nextArtworkUri: Uri? = null
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
        SpatialBackdrop(artworkUri = artworkUri, isDark = isDark)

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
        LaunchedEffect(previousArtworkUri, nextArtworkUri, coverPx) {
            delay(300)
            spatialPrefetchCover(context, previousArtworkUri, coverPx)
            spatialPrefetchCover(context, nextArtworkUri, coverPx)
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

// Fondo difuminado: la portada se reduce a un bitmap diminuto (proporcion de pantalla), se desenfoca de verdad
// (3 pasadas de "box blur" = casi gaussiano) y se estira suave a toda la pantalla. Resultado: manchas de color
// grandes y SIN pixeles. Se calcula una sola vez por cancion y en segundo plano, asi que no afecta al rendimiento.
private const val BACKDROP_W = 108
private const val BACKDROP_H = 240
private const val BACKDROP_BLUR_RADIUS = 18
private const val BACKDROP_SATURATION = 1.2f

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

/** Convierte la portada en el bitmap pequeno y muy desenfocado que se usa de fondo. */
private fun spatialBlurredBackdrop(source: Bitmap): Bitmap {
    val w = BACKDROP_W
    val h = BACKDROP_H
    val small = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(small)
    canvas.drawColor(android.graphics.Color.BLACK)
    val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(source, null, android.graphics.Rect(0, 0, w, h), paint)

    val pixels = IntArray(w * h)
    small.getPixels(pixels, 0, w, 0, 0, w, h)
    val red = IntArray(w * h) { (pixels[it] shr 16) and 0xFF }
    val green = IntArray(w * h) { (pixels[it] shr 8) and 0xFF }
    val blue = IntArray(w * h) { pixels[it] and 0xFF }
    val tmp = IntArray(w * h)
    repeat(3) {
        spatialBoxBlur(red, tmp, w, h, BACKDROP_BLUR_RADIUS)
        spatialBoxBlur(green, tmp, w, h, BACKDROP_BLUR_RADIUS)
        spatialBoxBlur(blue, tmp, w, h, BACKDROP_BLUR_RADIUS)
    }
    for (i in 0 until w * h) {
        val r = red[i].toFloat()
        val g = green[i].toFloat()
        val b = blue[i].toFloat()
        val luma = 0.299f * r + 0.587f * g + 0.114f * b
        val rr = (luma + (r - luma) * BACKDROP_SATURATION).roundToInt().coerceIn(0, 255)
        val gg = (luma + (g - luma) * BACKDROP_SATURATION).roundToInt().coerceIn(0, 255)
        val bb = (luma + (b - luma) * BACKDROP_SATURATION).roundToInt().coerceIn(0, 255)
        pixels[i] = (0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb
    }
    small.setPixels(pixels, 0, w, 0, 0, w, h)
    return small
}

/**
 * Fondo de SpatialFlow adaptado: la misma portada pero MUY difuminada (manchas suaves de sus colores, sin pixeles)
 * y un velo ligero para que el texto se lea.
 */
@Composable
private fun SpatialBackdrop(artworkUri: Uri?, isDark: Boolean) {
    val context = LocalContext.current
    val backdrop by produceState<ImageBitmap?>(initialValue = null, artworkUri) {
        value = if (artworkUri == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val bitmap = ImageUtils.loadArtworkBitmap(context, artworkUri, 128)
                    if (bitmap != null) spatialBlurredBackdrop(bitmap).asImageBitmap() else null
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    Crossfade(
        targetState = backdrop,
        animationSpec = tween(600),
        label = "SpatialBackdrop"
    ) { image ->
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                filterQuality = FilterQuality.Low,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    val veil = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.04f),
                0.55f to Color.Black.copy(alpha = 0.10f),
                1f to Color.Black.copy(alpha = 0.20f)
            )
        } else {
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.25f),
                0.55f to Color.White.copy(alpha = 0.30f),
                1f to Color.White.copy(alpha = 0.35f)
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(veil)
    )
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
