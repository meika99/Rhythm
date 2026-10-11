/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * "Spatial" player style for Rhythm.
 *
 * The visual design of this screen (large rounded artwork, left-aligned title,
 * pill-shaped chips, wavy seek bar and big rounded transport buttons) and the
 * color-derivation helpers are adapted from SpatialFlow (FullPlayer.kt and
 * PlayerUiComponents.kt)
 * Copyright © 2026 Shubham Karande
 * https://github.com/MythicalSHUB/SpatialFlow
 * Licensed under the Apache License, Version 2.0 (see NOTICE_SPATIALFLOW.md).
 *
 * Changes: rewritten on top of Rhythm's own data model (Song, AppSettings-driven
 * callbacks, ColorExtractor, Material Symbols icons) instead of SpatialFlow's
 * view model. No SpatialFlow branding, name, logo or artwork is used.
 */

package chromahub.rhythm.app.shared.presentation.screens.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.shared.presentation.components.icons.Icon
import chromahub.rhythm.app.shared.presentation.components.icons.MaterialSymbolIcon
import chromahub.rhythm.app.shared.presentation.components.player.SpatialWavySlider
import chromahub.rhythm.app.util.ColorExtractor
import chromahub.rhythm.app.util.ImageUtils
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Third player style for Rhythm ("Spatial").
 *
 * It only needs a small set of callbacks; everything else (queue sheet, song info sheet,
 * full-screen lyrics, sleep timer, ...) stays owned by [PlayerScreen], which simply passes
 * the same lambdas it already prepares for the Expressive style.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpatialPlayerScreen(
    song: Song?,
    isPlaying: Boolean,
    isFavorite: Boolean,
    progress: () -> Float,
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
    currentTimeStr: String,
    totalTimeStr: String,
    modifier: Modifier = Modifier,
    onTotalTimeClick: () -> Unit = {},
    swipeToDismissEnabled: Boolean = true
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val contentColor = if (isDark) Color.White else Color(0xFF1C1B1F)
    val contentSecondary = contentColor.copy(alpha = 0.6f)
    val chipBase = contentColor.copy(alpha = if (isDark) 0.08f else 0.06f)

    // ---- Colors derived from the artwork (same recipe as SpatialFlow) ----
    val fallbackSeed = MaterialTheme.colorScheme.primary
    val artworkUri = song?.artworkUri
    val seedArgb by produceState<Int?>(initialValue = null, artworkUri) {
        value = if (artworkUri == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val bitmap = ImageUtils.loadArtworkBitmap(context, artworkUri, 128)
                    val extracted = ColorExtractor.extractColorsFromBitmap(bitmap)
                    extracted?.seedColor?.takeIf { it != 0 } ?: extracted?.primary
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
    val seed = seedArgb?.let { Color(it) } ?: fallbackSeed
    val targetSurface = remember(seed, isDark) { spatialSurfaceColor(seed, isDark) }
    val targetAccent = remember(seed, isDark) { spatialAccentColor(seed, isDark) }
    val surface by animateColorAsState(targetSurface, tween(700), label = "SpatialSurface")
    val accent by animateColorAsState(targetAccent, tween(700), label = "SpatialAccent")
    val backgroundBrush = remember(surface, accent) {
        Brush.verticalGradient(listOf(lerp(surface, accent, 0.22f), surface, surface))
    }
    val onAccent = if (accent.luminance() > 0.5f) Color(0xFF1C1B1F) else Color.White

    // Optional "swipe down to close" for the full-screen route (the bottom sheet host
    // already handles its own drag and passes swipeToDismissEnabled = false).
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
            .background(backgroundBrush)
            .then(swipeModifier)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val artSize = maxOf(minOf(maxWidth - 40.dp, maxHeight * 0.42f), 120.dp)

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ---- Header ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            icon = MaterialSymbolIcon("keyboard_arrow_down", defaultWeight = 500),
                            contentDescription = "Collapse player",
                            modifier = Modifier.size(28.dp),
                            tint = contentColor.copy(alpha = 0.8f),
                            size = 28.dp
                        )
                    }
                    Text(
                        text = "NOW PLAYING",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentSecondary
                    )
                    IconButton(onClick = onMoreClick) {
                        Icon(
                            icon = MaterialSymbolIcon("more_vert", defaultWeight = 500),
                            contentDescription = "More options",
                            modifier = Modifier.size(24.dp),
                            tint = contentColor.copy(alpha = 0.8f),
                            size = 24.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ---- Artwork ----
                val artworkScale by animateFloatAsState(
                    targetValue = if (isPlaying) 1f else 0.94f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "SpatialArtworkScale"
                )
                Box(
                    modifier = Modifier
                        .size(artSize)
                        .graphicsLayer {
                            scaleX = artworkScale
                            scaleY = artworkScale
                        }
                        .clip(RoundedCornerShape(32.dp))
                        .background(chipBase),
                    contentAlignment = Alignment.Center
                ) {
                    if (artworkUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(artworkUri)
                                .crossfade(300)
                                .memoryCacheKey(artworkUri.toString())
                                .diskCacheKey(artworkUri.toString())
                                .build(),
                            contentDescription = song?.album,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            icon = MaterialSymbolIcon("music_note", defaultWeight = 400),
                            contentDescription = null,
                            modifier = Modifier.size(96.dp),
                            tint = contentColor.copy(alpha = 0.35f),
                            size = 96.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ---- Title / artist ----
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = song?.title ?: "Nothing playing",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                            .clickable { onSongInfoClick() }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song?.artist ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentSecondary,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee()
                            .clickable { onShowArtist() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ---- Pill chips (horizontally scrollable) ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpatialPillChip(
                        icon = MaterialSymbolIcon("favorite", filled = isFavorite),
                        label = "Like",
                        isSelected = isFavorite,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleFavorite()
                        },
                        contentColor = contentColor,
                        accentColor = accent,
                        baseColor = chipBase,
                        isDark = isDark
                    )
                    SpatialPillChip(
                        icon = MaterialSymbolIcon("shuffle"),
                        label = "Shuffle",
                        isSelected = isShuffleEnabled,
                        onClick = onToggleShuffle,
                        contentColor = contentColor,
                        accentColor = accent,
                        baseColor = chipBase,
                        isDark = isDark
                    )
                    SpatialPillChip(
                        icon = MaterialSymbolIcon(if (repeatMode == 2) "repeat_one" else "repeat"),
                        label = "Repeat",
                        isSelected = repeatMode != 0,
                        onClick = onToggleRepeat,
                        contentColor = contentColor,
                        accentColor = accent,
                        baseColor = chipBase,
                        isDark = isDark
                    )
                    SpatialPillChip(
                        icon = MaterialSymbolIcon("lyrics"),
                        label = "Lyrics",
                        onClick = onOpenLyrics,
                        contentColor = contentColor,
                        accentColor = accent,
                        baseColor = chipBase,
                        isDark = isDark
                    )
                    SpatialPillChip(
                        icon = MaterialSymbolIcon("queue_music"),
                        label = "Queue",
                        onClick = onQueueClick,
                        contentColor = contentColor,
                        accentColor = accent,
                        baseColor = chipBase,
                        isDark = isDark
                    )
                    SpatialPillChip(
                        icon = MaterialSymbolIcon("bedtime"),
                        label = "Sleep timer",
                        onClick = onSleepTimer,
                        contentColor = contentColor,
                        accentColor = accent,
                        baseColor = chipBase,
                        isDark = isDark
                    )
                    SpatialPillChip(
                        icon = MaterialSymbolIcon("equalizer"),
                        label = "Equalizer",
                        onClick = onEqualizer,
                        contentColor = contentColor,
                        accentColor = accent,
                        baseColor = chipBase,
                        isDark = isDark
                    )
                    SpatialPillChip(
                        icon = MaterialSymbolIcon("share"),
                        label = "Share",
                        onClick = onShareFile,
                        contentColor = contentColor,
                        accentColor = accent,
                        baseColor = chipBase,
                        isDark = isDark
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ---- Wavy seek bar ----
                var isScrubbing by remember { mutableStateOf(false) }
                var scrubValue by remember { mutableFloatStateOf(0f) }
                val currentProgress = progress().coerceIn(0f, 1f)

                SpatialWavySlider(
                    value = if (isScrubbing) scrubValue else currentProgress,
                    onValueChange = {
                        isScrubbing = true
                        scrubValue = it
                    },
                    onValueChangeFinished = {
                        onSeek(scrubValue)
                        isScrubbing = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    activeTrackColor = accent,
                    inactiveTrackColor = contentColor.copy(alpha = if (isDark) 0.12f else 0.10f),
                    thumbColor = contentColor,
                    isPlaying = isPlaying,
                    trackHeight = 4.dp,
                    thumbRadius = 6.dp,
                    waveAmplitudeWhenPlaying = 6.dp,
                    waveLength = 48.dp
                )

                // ---- Times + format badge ----
                val formatLabel = remember(song?.id) {
                    song?.codec?.takeIf { it.isNotBlank() }?.uppercase()
                        ?: song?.path?.substringAfterLast('.', "")
                            ?.takeIf { it.length in 2..5 }
                            ?.uppercase()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentTimeStr,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentSecondary
                    )
                    if (formatLabel != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBase)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = formatLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = contentSecondary
                            )
                        }
                    }
                    Text(
                        text = totalTimeStr,
                        style = MaterialTheme.typography.labelMedium,
                        color = contentSecondary,
                        modifier = Modifier.clickable { onTotalTimeClick() }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ---- Transport buttons ----
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpatialControlButton(
                        iconName = "skip_previous",
                        contentDescription = "Previous song",
                        onClick = onSkipPrevious,
                        containerColor = chipBase,
                        contentColor = contentColor,
                        iconSize = 36.dp,
                        modifier = Modifier.weight(1f)
                    )
                    SpatialControlButton(
                        iconName = if (isPlaying) "pause" else "play_arrow",
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPlayPause()
                        },
                        containerColor = accent,
                        contentColor = onAccent,
                        iconSize = 42.dp,
                        modifier = Modifier.weight(1.2f)
                    )
                    SpatialControlButton(
                        iconName = "skip_next",
                        contentDescription = "Next song",
                        onClick = onSkipNext,
                        containerColor = chipBase,
                        contentColor = contentColor,
                        iconSize = 36.dp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // ---- Swipe up / tap to open the queue ----
                var queueGestureFired by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { queueGestureFired = false }
                            ) { _, dragAmount ->
                                if (dragAmount < -10f && !queueGestureFired) {
                                    queueGestureFired = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onQueueClick()
                                }
                            }
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onQueueClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon = MaterialSymbolIcon("keyboard_arrow_up", defaultWeight = 500),
                        contentDescription = "Open queue",
                        modifier = Modifier.size(32.dp),
                        tint = contentColor.copy(alpha = 0.5f),
                        size = 32.dp
                    )
                }
            }
        }
    }
}

/** Rounded pill with an icon and a label; highlighted with the accent color when selected. */
@Composable
private fun SpatialPillChip(
    icon: MaterialSymbolIcon,
    label: String,
    onClick: () -> Unit,
    contentColor: Color,
    accentColor: Color,
    baseColor: Color,
    isDark: Boolean,
    isSelected: Boolean = false
) {
    val background = if (isSelected) {
        accentColor.copy(alpha = if (isDark) 0.25f else 0.18f)
    } else {
        baseColor
    }
    val tint = if (isSelected) accentColor else contentColor.copy(alpha = 0.8f)

    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = tint,
                size = 18.dp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = tint,
                maxLines = 1
            )
        }
    }
}

/** Big transport button whose corners tighten while pressed (bouncy spring). */
@Composable
private fun SpatialControlButton(
    iconName: String,
    contentDescription: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    iconSize: Dp,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed) 12.dp else 28.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "SpatialControlCorner"
    )
    Button(
        onClick = onClick,
        modifier = modifier.height(76.dp),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            icon = MaterialSymbolIcon(iconName, filled = true, defaultWeight = 500),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = contentColor,
            size = iconSize
        )
    }
}

/** Dark/light surface tinted with the artwork hue (same recipe SpatialFlow uses). */
private fun spatialSurfaceColor(source: Color, isDark: Boolean): Color {
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(source.toArgb(), hsl)
    val isMonochrome = hsl[1] < 0.06f
    hsl[2] = if (isDark) 0.155f else 0.835f
    hsl[1] = when {
        isMonochrome -> 0f
        isDark -> hsl[1].coerceIn(0.32f, 0.54f)
        else -> hsl[1].coerceIn(0.30f, 0.48f)
    }
    return Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
}

/** Accent color (play button, active chips, seek bar) readable on top of the surface. */
private fun spatialAccentColor(source: Color, isDark: Boolean): Color {
    val hsl = FloatArray(3)
    androidx.core.graphics.ColorUtils.colorToHSL(source.toArgb(), hsl)
    return if (hsl[1] < 0.08f) {
        if (isDark) Color.White else Color(0xFF1C1B1F)
    } else if (isDark) {
        source
    } else {
        hsl[2] = hsl[2].coerceAtMost(0.45f)
        hsl[1] = hsl[1].coerceAtLeast(0.6f)
        Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
    }
}
