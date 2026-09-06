package com.namazapp.hubli.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.namazapp.hubli.ui.theme.AppTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/*
 * Performance rules used throughout this file (they are what keep tab switching at 60/120 fps):
 *
 *  1. Animation values are only ever read inside `graphicsLayer { }` or draw lambdas – never in
 *     composition. A changing value therefore updates a RenderNode property or re-records one
 *     small layer instead of recomposing (and re-measuring) the screen.
 *  2. Brushes are created once in `drawWithCache`, not per frame.
 *  3. Continuous effects live on their own `graphicsLayer()` so their per-frame invalidation
 *     doesn't force the card that hosts them (text, icons, shadows…) to be re-recorded.
 *  4. Everything animated respects [LocalPageVisible]: pages parked off-screen in the pager
 *     pause their effects and hold their entrance animation until they are first shown.
 */

/**
 * `true` while the hosting page is (at least partly) on screen. The navigation pager provides
 * `false` for parked pages; the splash provides `false` until it has finished.
 */
val LocalPageVisible = compositionLocalOf { true }

/* --------------------------------------------------------------------------------------------- */
/* Entrance                                                                                       */
/* --------------------------------------------------------------------------------------------- */

/**
 * Fades, slides up and un-scales the content the first time its page becomes visible, delayed by
 * [baseDelayMs] + [index] × [staggerMs]. Plays once per composition – revisiting a kept-alive
 * page shows it already settled, which is what makes swiping between tabs feel solid.
 */
fun Modifier.staggeredEntrance(
    index: Int,
    staggerMs: Int = 75,
    durationMs: Int = 520,
    offsetY: Dp = 28.dp,
    baseDelayMs: Int = 90,
): Modifier = composed {
    val visible by rememberUpdatedState(LocalPageVisible.current)
    val alpha = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }   // 0 = offset & shrunk, 1 = settled
    LaunchedEffect(Unit) {
        // Wait (without being cancelled by later visibility changes) until first shown, then
        // always run to completion – a cancelled peek can never leave content half-faded.
        snapshotFlow { visible }.first { it }
        delay((baseDelayMs + index * staggerMs).toLong())
        coroutineScope {
            launch { alpha.animateTo(1f, tween(durationMs * 3 / 4, easing = EaseOutCubic)) }
            launch { progress.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow)) }
        }
    }
    graphicsLayer {
        this.alpha = alpha.value
        translationY = (1f - progress.value) * offsetY.toPx()
        val s = 0.97f + 0.03f * progress.value
        scaleX = s
        scaleY = s
    }
}

/* --------------------------------------------------------------------------------------------- */
/* Touch feedback                                                                                 */
/* --------------------------------------------------------------------------------------------- */

/** Scales the element down slightly while pressed – the "tactile card" feel. */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.97f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/** Bounces the element once each time [key] changes. */
fun Modifier.bounceOnChange(key: Any?): Modifier = composed {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(key) {
        scale.snapTo(1f)
        scale.animateTo(1.14f, tween(120, easing = FastOutSlowInEasing))
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
    }
    graphicsLayer { scaleX = scale.value; scaleY = scale.value }
}

/* --------------------------------------------------------------------------------------------- */
/* Continuous effects                                                                             */
/* --------------------------------------------------------------------------------------------- */

/**
 * Diagonal shine sweeping across the parent – place it inside a clipped Box with
 * `Modifier.matchParentSize()`. The band is drawn once; each frame only moves the layer.
 */
@Composable
fun ShimmerOverlay(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.17f),
    durationMs: Int = 3600,
) {
    if (!LocalPageVisible.current) return
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -0.6f, targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerProgress",
    )
    Box(
        modifier
            .graphicsLayer { translationX = size.width * progress }
            .drawWithCache {
                val w = size.width
                val brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, color, Color.Transparent),
                    start = Offset(-w * 0.22f, 0f),
                    end = Offset(w * 0.12f, size.height),
                )
                onDrawBehind { drawRect(brush) }
            },
    )
}

/** Gentle breathing scale for accent elements. */
fun Modifier.breathe(min: Float = 0.97f, max: Float = 1.03f, durationMs: Int = 2600): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "breathe")
    val scale by transition.animateFloat(
        initialValue = min, targetValue = max,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breatheScale",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * A slowly pulsing value between [min] and [max]. Returned as [State] on purpose: read `.value`
 * only inside a `graphicsLayer` / draw lambda so the pulse never recomposes anything.
 */
@Composable
fun rememberPulse(min: Float = 0f, max: Float = 1f, durationMs: Int = 2200): State<Float> {
    val transition = rememberInfiniteTransition(label = "pulse")
    return transition.animateFloat(
        initialValue = min, targetValue = max,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseValue",
    )
}

/** Slowly drifting soft light blobs – ambient motion for the hero card / splash. */
@Composable
fun AmbientOrbs(
    modifier: Modifier = Modifier,
    colors: List<Color>,
    alpha: Float = 0.45f,
    speedMs: Int = 9000,
) {
    if (!LocalPageVisible.current) return
    val transition = rememberInfiniteTransition(label = "orbs")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(speedMs, easing = LinearEasing), RepeatMode.Restart),
        label = "orbT",
    )
    Box(
        modifier
            .graphicsLayer() // own layer: per-frame redraw stays inside this node
            .drawWithCache {
                val w = size.width
                val h = size.height
                val baseRadius = size.maxDimension * 0.4f
                val brushes = colors.map { c ->
                    Brush.radialGradient(
                        colors = listOf(c.copy(alpha = alpha), c.copy(alpha = 0f)),
                        center = Offset.Zero, radius = baseRadius.coerceAtLeast(1f),
                    )
                }
                onDrawBehind {
                    if (w <= 0f || h <= 0f) return@onDrawBehind
                    val tt = t // draw-phase read
                    brushes.forEachIndexed { i, brush ->
                        val phase = tt * 2f * PI.toFloat() + i * 2.1f
                        val cx = w * (0.5f + 0.45f * cos(phase * (1 + i * 0.13f)))
                        val cy = h * (0.5f + 0.45f * sin(phase * (0.8f + i * 0.17f)))
                        val s = (0.34f + 0.08f * sin(phase)) * size.maxDimension / baseRadius
                        translate(cx, cy) {
                            scale(s, s, Offset.Zero) {
                                drawCircle(brush, radius = baseRadius, center = Offset.Zero)
                            }
                        }
                    }
                }
            },
    )
}

/**
 * Static (cached, zero-cost per frame) colour wash behind the whole app: a primary glow at the
 * top-left, a secondary glow at the right and a faint primary glow at the bottom.
 */
@Composable
fun BackgroundWash(
    modifier: Modifier = Modifier,
    primary: Color,
    secondary: Color,
    primaryAlpha: Float,
    secondaryAlpha: Float,
) {
    Box(
        modifier.drawWithCache {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) {
                onDrawBehind { }
            } else {
                val b1 = Brush.radialGradient(
                    listOf(primary.copy(alpha = primaryAlpha), primary.copy(alpha = 0f)),
                    center = Offset(w * 0.12f, h * 0.02f), radius = w * 0.95f,
                )
                val b2 = Brush.radialGradient(
                    listOf(secondary.copy(alpha = secondaryAlpha), secondary.copy(alpha = 0f)),
                    center = Offset(w * 1.0f, h * 0.48f), radius = w * 0.8f,
                )
                val b3 = Brush.radialGradient(
                    listOf(primary.copy(alpha = primaryAlpha * 0.7f), primary.copy(alpha = 0f)),
                    center = Offset(w * 0.25f, h * 1.02f), radius = w * 0.85f,
                )
                onDrawBehind {
                    drawRect(b1)
                    drawRect(b2)
                    drawRect(b3)
                }
            }
        },
    )
}

/** Twinkling stars for night-time gradients and the splash screen. */
@Composable
fun Starfield(modifier: Modifier = Modifier, count: Int = 40, color: Color = Color.White, seed: Int = 7) {
    if (!LocalPageVisible.current) return
    val stars = remember(count, seed) {
        val rnd = Random(seed)
        List(count) { floatArrayOf(rnd.nextFloat(), rnd.nextFloat(), 0.6f + rnd.nextFloat() * 1.6f, rnd.nextFloat()) }
    }
    val transition = rememberInfiniteTransition(label = "stars")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkle",
    )
    Box(
        modifier
            .graphicsLayer()
            .drawWithCache {
                val radii = FloatArray(stars.size) { stars[it][2].dp.toPx() * 0.5f }
                onDrawBehind {
                    val tt = t
                    stars.forEachIndexed { i, s ->
                        val twinkle = 0.35f + 0.65f * ((sin((tt + s[3]) * 2f * PI.toFloat()) + 1f) / 2f)
                        drawCircle(
                            color.copy(alpha = twinkle * 0.9f),
                            radius = radii[i],
                            center = Offset(s[0] * size.width, s[1] * size.height),
                        )
                    }
                }
            },
    )
}

/* --------------------------------------------------------------------------------------------- */
/* Edges                                                                                          */
/* --------------------------------------------------------------------------------------------- */

/**
 * A soft shadow along the top edge of a scrolling list that appears once the list has been
 * scrolled, so rows visibly slide *under* the pinned content above. Unlike an alpha-mask fade
 * this needs no off-screen compositing – it is a single cached gradient rectangle.
 */
fun Modifier.scrollScrim(
    listState: LazyListState,
    height: Dp = 16.dp,
    color: Color = Color.Unspecified,
): Modifier = composed {
    val scrim = if (color.isSpecified) color else {
        if (AppTheme.extended.isDark) Color.Black.copy(alpha = 0.38f) else Color(0xFF0B7A5B).copy(alpha = 0.10f)
    }
    val shown by animateFloatAsState(
        targetValue = if (listState.canScrollBackward) 1f else 0f,
        animationSpec = tween(220),
        label = "scrollScrim",
    )
    drawWithCache {
        val h = height.toPx()
        val brush = Brush.verticalGradient(listOf(scrim, scrim.copy(alpha = 0f)), startY = 0f, endY = h)
        onDrawWithContent {
            drawContent()
            val a = shown
            if (a > 0.01f) drawRect(brush, size = Size(size.width, h), alpha = a)
        }
    }
}

/* --------------------------------------------------------------------------------------------- */
/* Text                                                                                           */
/* --------------------------------------------------------------------------------------------- */

/**
 * Countdown text where each digit rolls vertically when it changes (odometer style),
 * instead of the whole string re-rendering.
 */
@Composable
fun RollingText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
) {
    Row(modifier) {
        text.forEachIndexed { i, ch ->
            if (ch.isDigit()) {
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        (slideInVertically(tween(260, easing = EaseOutCubic)) { -it } + fadeIn(tween(200))) togetherWith
                            (slideOutVertically(tween(260, easing = EaseOutCubic)) { it } + fadeOut(tween(160)))
                    },
                    label = "digit$i",
                ) { digit ->
                    Text(digit.toString(), style = style, color = color, fontWeight = fontWeight)
                }
            } else {
                Text(ch.toString(), style = style, color = color, fontWeight = fontWeight)
            }
        }
    }
}
