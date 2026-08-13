package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MetronomeBordeaux
import com.example.ui.theme.MetronomeBordeauxDark
import com.example.ui.theme.MetronomeCream
import com.example.ui.theme.MetronomeCreamDarker
import com.example.ui.theme.MetronomeGreen
import com.example.ui.theme.MetronomeOrange
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

enum class VisualizerLayoutMode {
    ROW,
    RING
}

@Composable
fun MetronomeScreen(
    bpm: Int,
    beatsPerMeasure: Int,
    beatValue: Int,
    isPlaying: Boolean,
    currentBeatIndex: Int,
    isCurrentAccent: Boolean,
    lastBeatTriggerTime: Long,
    onBpmChange: (Int) -> Unit,
    onBeatsPerMeasureChange: (Int) -> Unit,
    onBeatValueChange: (Int) -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Auto-select ring layout if beats > 7, but allow manual toggle
    var layoutModeOverride by remember { mutableStateOf<VisualizerLayoutMode?>(null) }
    val effectiveLayoutMode = layoutModeOverride ?: if (beatsPerMeasure > 7) VisualizerLayoutMode.RING else VisualizerLayoutMode.ROW

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MetronomeCream),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. App Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "METRONOME",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    color = MetronomeBordeaux,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "PRECISION ANALOG TIMING",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = MetronomeGreen,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Large Dynamic Measure Visualizer (Edge-to-edge adaptive Row / Ring layout)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("visual_indicator_container")
            ) {
                MeasureBeatVisualizer(
                    beatsPerMeasure = beatsPerMeasure,
                    currentBeatIndex = currentBeatIndex,
                    isCurrentAccent = isCurrentAccent,
                    isPlaying = isPlaying,
                    lastBeatTriggerTime = lastBeatTriggerTime,
                    layoutMode = effectiveLayoutMode,
                    onToggleLayoutMode = {
                        layoutModeOverride = if (effectiveLayoutMode == VisualizerLayoutMode.ROW) {
                            VisualizerLayoutMode.RING
                        } else {
                            VisualizerLayoutMode.ROW
                        }
                    }
                )

                // Beat Status Text
                Text(
                    text = if (isPlaying) {
                        if (isCurrentAccent) "BEAT ${currentBeatIndex + 1} / $beatsPerMeasure  (ACCENT)"
                        else "BEAT ${currentBeatIndex + 1} / $beatsPerMeasure"
                    } else {
                        "READY ($beatsPerMeasure / $beatValue)"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = if (isPlaying && isCurrentAccent) MetronomeOrange else MetronomeBordeaux,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Central BPM Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Large − Button
                    HoldRepeatButton(
                        onTrigger = { onBpmChange(bpm - 1) },
                        modifier = Modifier
                            .size(60.dp)
                            .testTag("bpm_minus")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease BPM",
                            tint = MetronomeCream,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // BPM Number Display
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = bpm.toString(),
                            fontSize = 64.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 64.sp,
                            color = MetronomeBordeaux,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.testTag("bpm_display")
                        )
                        Text(
                            text = "BPM",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 3.sp,
                            color = MetronomeOrange
                        )
                    }

                    // Large + Button
                    HoldRepeatButton(
                        onTrigger = { onBpmChange(bpm + 1) },
                        modifier = Modifier
                            .size(60.dp)
                            .testTag("bpm_plus")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase BPM",
                            tint = MetronomeCream,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tempo Slider (10 to 260)
                Slider(
                    value = bpm.toFloat(),
                    onValueChange = { onBpmChange(it.toInt()) },
                    valueRange = 10f..260f,
                    steps = 249, // 260 - 10 - 1 = 249 steps
                    colors = SliderDefaults.colors(
                        thumbColor = MetronomeBordeaux,
                        activeTrackColor = MetronomeOrange,
                        inactiveTrackColor = MetronomeCreamDarker
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                        .testTag("bpm_slider")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("10", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MetronomeBordeaux)
                    Text("120", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MetronomeBordeaux)
                    Text("260", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MetronomeBordeaux)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Measure Selector (A / B) - Balanced 50/50 Layout with equal button sizing
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MetronomeCreamDarker)
                    .border(2.dp, MetronomeBordeaux, RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "TIME SIGNATURE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = MetronomeBordeaux,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // A Section (50% proportional column)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "BEATS (A)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MetronomeGreen,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HoldRepeatButton(
                                onTrigger = { onBeatsPerMeasureChange(beatsPerMeasure - 1) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("measure_a_minus"),
                                backgroundColor = MetronomeBordeaux
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease beats",
                                    tint = MetronomeCream,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Text(
                                text = beatsPerMeasure.toString(),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = MetronomeBordeaux,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .width(38.dp)
                                    .testTag("measure_a_display")
                            )

                            HoldRepeatButton(
                                onTrigger = { onBeatsPerMeasureChange(beatsPerMeasure + 1) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("measure_a_plus"),
                                backgroundColor = MetronomeBordeaux
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase beats",
                                    tint = MetronomeCream,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Divider /
                    Text(
                        text = "/",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        color = MetronomeBordeaux,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )

                    // B Section (50% proportional column)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "VALUE (B)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MetronomeGreen,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HoldRepeatButton(
                                onTrigger = { onBeatValueChange(beatValue - 1) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("measure_b_minus"),
                                backgroundColor = MetronomeBordeaux
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease beat value",
                                    tint = MetronomeCream,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Text(
                                text = beatValue.toString(),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = MetronomeBordeaux,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .width(38.dp)
                                    .testTag("measure_b_display")
                            )

                            HoldRepeatButton(
                                onTrigger = { onBeatValueChange(beatValue + 1) },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("measure_b_plus"),
                                backgroundColor = MetronomeBordeaux
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase beat value",
                                    tint = MetronomeCream,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Large Start / Stop Button
            Surface(
                onClick = onTogglePlay,
                shape = RoundedCornerShape(20.dp),
                color = if (isPlaying) MetronomeBordeaux else MetronomeGreen,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .border(
                        width = 3.dp,
                        color = if (isPlaying) MetronomeBordeauxDark else MetronomeOrange,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .testTag("start_stop_button")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Stop Metronome" else "Start Metronome",
                        tint = MetronomeCream,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isPlaying) "STOP" else "START",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp,
                        color = MetronomeCream
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

/**
 * Dynamic Measure Visualizer supporting both Row and Ring (Halka) layouts.
 * Maximizes screen width usage and visual impact.
 */
@Composable
fun MeasureBeatVisualizer(
    beatsPerMeasure: Int,
    currentBeatIndex: Int,
    isCurrentAccent: Boolean,
    isPlaying: Boolean,
    lastBeatTriggerTime: Long,
    layoutMode: VisualizerLayoutMode,
    onToggleLayoutMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecentlyFlashed = (System.currentTimeMillis() - lastBeatTriggerTime) < 140

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        val availableWidth = maxWidth

        if (layoutMode == VisualizerLayoutMode.ROW) {
            // Adaptive horizontal Row Layout maximizing available width
            val totalSpacing = 8.dp * (beatsPerMeasure - 1)
            val maxCalculatedSize = ((availableWidth - totalSpacing - 16.dp).value / beatsPerMeasure).dp
            val circleSize = maxCalculatedSize.coerceIn(24.dp, 64.dp)
            val spacing = if (beatsPerMeasure <= 5) 10.dp else if (beatsPerMeasure <= 8) 6.dp else 4.dp

            val fontSize = when {
                circleSize >= 54.dp -> 22.sp
                circleSize >= 44.dp -> 18.sp
                circleSize >= 36.dp -> 14.sp
                circleSize >= 28.dp -> 11.sp
                else -> 9.sp
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until beatsPerMeasure) {
                    val isActive = isPlaying && isRecentlyFlashed && (currentBeatIndex == i)
                    val isAccent = (i == 0)
                    val isAccentActive = isActive && isAccent

                    val dotColor by animateColorAsState(
                        targetValue = when {
                            isAccentActive -> MetronomeOrange
                            isActive -> MetronomeGreen
                            else -> MetronomeCreamDarker
                        },
                        animationSpec = tween(durationMillis = if (isActive) 15 else 90),
                        label = "dot_color_$i"
                    )

                    val textColor = when {
                        isActive -> MetronomeCream
                        else -> MetronomeBordeaux
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = spacing / 2)
                            .size(circleSize)
                            .scale(if (isAccentActive) 1.15f else if (isActive) 1.08f else 1.0f)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(
                                width = if (isAccent) 2.5.dp else 1.5.dp,
                                color = if (isAccentActive) MetronomeOrange else MetronomeBordeaux,
                                shape = CircleShape
                            )
                            .testTag("beat_dot_$i"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (i + 1).toString(),
                            fontSize = fontSize,
                            fontWeight = if (isAccent || isActive) FontWeight.Black else FontWeight.Bold,
                            color = textColor,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        } else {
            // Large Circular Ring Layout (Halka Şeklinde)
            val containerSize = min(availableWidth.value, 200f).dp
            val ringRadiusDp = (containerSize.value * 0.38f).dp
            
            val dotSize = when {
                beatsPerMeasure <= 6 -> 42.dp
                beatsPerMeasure <= 10 -> 34.dp
                beatsPerMeasure <= 14 -> 28.dp
                beatsPerMeasure <= 18 -> 24.dp
                else -> 20.dp
            }
            val fontSize = when {
                dotSize >= 38.dp -> 16.sp
                dotSize >= 30.dp -> 13.sp
                dotSize >= 26.dp -> 11.sp
                else -> 8.sp
            }

            Box(
                modifier = Modifier
                    .size(containerSize)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                // Central dial background circle
                Box(
                    modifier = Modifier
                        .size(containerSize * 0.9f)
                        .clip(CircleShape)
                        .background(MetronomeCreamDarker.copy(alpha = 0.5f))
                        .border(1.5.dp, MetronomeBordeaux.copy(alpha = 0.3f), CircleShape)
                )

                // Center Beat Number indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isPlaying) "${currentBeatIndex + 1}" else "$beatsPerMeasure",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isPlaying && isCurrentAccent) MetronomeOrange else MetronomeBordeaux,
                        lineHeight = 32.sp
                    )
                    Text(
                        text = if (isPlaying && isCurrentAccent) "ACCENT" else "BEAT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = if (isPlaying && isCurrentAccent) MetronomeOrange else MetronomeGreen
                    )
                }

                // Place dots around the perimeter (top = Beat 1 / Accent)
                for (i in 0 until beatsPerMeasure) {
                    val angle = -PI / 2.0 + (i.toDouble() * (2.0 * PI / beatsPerMeasure.toDouble()))
                    val radiusPx = ringRadiusDp.value
                    val xOffset = (radiusPx * cos(angle)).roundToInt()
                    val yOffset = (radiusPx * sin(angle)).roundToInt()

                    val isActive = isPlaying && isRecentlyFlashed && (currentBeatIndex == i)
                    val isAccent = (i == 0)
                    val isAccentActive = isActive && isAccent

                    val dotColor by animateColorAsState(
                        targetValue = when {
                            isAccentActive -> MetronomeOrange
                            isActive -> MetronomeGreen
                            else -> MetronomeCreamDarker
                        },
                        animationSpec = tween(durationMillis = if (isActive) 15 else 90),
                        label = "ring_dot_color_$i"
                    )

                    val textColor = when {
                        isActive -> MetronomeCream
                        else -> MetronomeBordeaux
                    }

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(xOffset.dp.roundToPx(), yOffset.dp.roundToPx()) }
                            .size(dotSize)
                            .scale(if (isAccentActive) 1.2f else if (isActive) 1.1f else 1.0f)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(
                                width = if (isAccent) 2.5.dp else 1.5.dp,
                                color = if (isAccentActive) MetronomeOrange else MetronomeBordeaux,
                                shape = CircleShape
                            )
                            .testTag("beat_dot_$i"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (i + 1).toString(),
                            fontSize = fontSize,
                            fontWeight = if (isAccent || isActive) FontWeight.Black else FontWeight.Bold,
                            color = textColor,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }
        }

        // Layout switcher icon button at top-right
        IconButton(
            onClick = onToggleLayoutMode,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
                .testTag("toggle_layout_mode_button")
        ) {
            Icon(
                imageVector = if (layoutMode == VisualizerLayoutMode.ROW) Icons.Outlined.Circle else Icons.Outlined.ViewWeek,
                contentDescription = if (layoutMode == VisualizerLayoutMode.ROW) "Switch to Ring Layout" else "Switch to Row Layout",
                tint = MetronomeBordeaux.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * High-performance button supporting single-click and fast repeat on long-press.
 */
@Composable
fun HoldRepeatButton(
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MetronomeBordeaux,
    content: @Composable () -> Unit
) {
    val currentOnTrigger by rememberUpdatedState(onTrigger)
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            currentOnTrigger()
            // Initial delay before repeating starts
            delay(350)
            while (isPressed && isActive) {
                currentOnTrigger()
                delay(60) // Fast repeat step
            }
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPressed) MetronomeOrange else backgroundColor)
            .border(2.dp, MetronomeCreamDarker, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        try {
                            // Wait until all pointers are up or cancelled
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.changes.all { !it.pressed }) {
                                    break
                                }
                            }
                        } finally {
                            isPressed = false
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
