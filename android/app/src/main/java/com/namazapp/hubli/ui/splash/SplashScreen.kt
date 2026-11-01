package com.namazapp.hubli.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.namazapp.hubli.ui.components.AmbientOrbs
import com.namazapp.hubli.ui.components.Starfield
import com.namazapp.hubli.ui.theme.Emerald20
import com.namazapp.hubli.ui.theme.Emerald40
import com.namazapp.hubli.ui.theme.Emerald60
import com.namazapp.hubli.ui.theme.Gold80
import com.namazapp.hubli.ui.theme.Ink
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated intro shown once per cold start, after the system splash icon hands over.
 *
 * Timeline (ms):
 *  0     – background orbs + stars are already moving
 *  100   – crescent stroke draws itself (0 → 100 %)
 *  650   – crescent fills; a soft glow blooms behind it
 *  800   – star pops in with an overshoot
 *  950   – title letters cascade in; tagline fades
 *  1750  – whole screen scales up slightly and fades → [onFinished]
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val crescentDraw = remember { Animatable(0f) }
    val crescentFill = remember { Animatable(0f) }
    val glow = remember { Animatable(0f) }
    val star = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val tagline = remember { Animatable(0f) }
    val exit = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(100)
        crescentDraw.animateTo(1f, tween(600, easing = EaseInOutCubic))
        coroutineScope {
            launch { crescentFill.animateTo(1f, tween(350)) }
            launch { glow.animateTo(1f, tween(900, easing = EaseOutCubic)) }
            launch {
                delay(150)
                star.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium))
            }
            launch {
                delay(300)
                title.animateTo(1f, tween(700, easing = EaseOutCubic))
            }
            launch {
                delay(650)
                tagline.animateTo(1f, tween(500))
            }
        }
        delay(380)
        exit.animateTo(1f, tween(380, easing = EaseInOutCubic))
        onFinished()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Ink, Color(0xFF0A1F1A), Emerald20)))
            .graphicsLayer {
                alpha = 1f - exit.value
                val s = 1f + 0.08f * exit.value
                scaleX = s; scaleY = s
            },
    ) {
        AmbientOrbs(
            modifier = Modifier.fillMaxSize(),
            colors = listOf(Emerald40, Emerald60, Gold80.copy(alpha = 0.6f)),
            alpha = 0.28f,
            speedMs = 12000,
        )
        Starfield(Modifier.fillMaxSize(), count = 60, seed = 11)

        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                // Glow bloom
                Canvas(Modifier.fillMaxSize()) {
                    val r = size.minDimension * (0.35f + 0.25f * glow.value)
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Emerald60.copy(alpha = 0.55f * glow.value), Color.Transparent),
                            center = center, radius = r,
                        ),
                        radius = r,
                    )
                }
                CrescentAndStar(
                    drawProgress = crescentDraw.value,
                    fillProgress = crescentFill.value,
                    starScale = star.value,
                    modifier = Modifier.size(150.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            CascadingTitle(text = "Namaz Times", progress = title.value)

            Spacer(Modifier.height(8.dp))

            Text(
                "Hubli · Karnataka",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 3.sp),
                color = Gold80.copy(alpha = 0.9f * tagline.value),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { translationY = (1f - tagline.value) * 10f },
            )
        }

        Text(
            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.55f * tagline.value),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp),
        )
    }
}

@Composable
private fun CrescentAndStar(
    drawProgress: Float,
    fillProgress: Float,
    starScale: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val outerR = w * 0.36f
        val outerC = Offset(w * 0.46f, h * 0.52f)
        val innerR = outerR * 0.86f
        val innerC = Offset(outerC.x + outerR * 0.34f, outerC.y - outerR * 0.18f)

        // Crescent = outer disc minus the offset inner disc
        val crescentOnly = Path().apply {
            op(
                Path().apply { addOval(Rect(outerC, outerR)) },
                Path().apply { addOval(Rect(innerC, innerR)) },
                PathOperation.Difference,
            )
        }

        // 1) Stroke reveal along the crescent outline
        if (drawProgress > 0f) {
            val measure = PathMeasure().apply { setPath(crescentOnly, forceClosed = true) }
            val segment = Path()
            measure.getSegment(0f, measure.length * drawProgress, segment, startWithMoveTo = true)
            drawPath(
                segment,
                brush = Brush.linearGradient(listOf(Color.White, Emerald60)),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        // 2) Fill fades in
        if (fillProgress > 0f) {
            drawPath(
                crescentOnly,
                brush = Brush.linearGradient(
                    listOf(Color.White, Color(0xFFDDF5EC)),
                    start = Offset(0f, 0f), end = Offset(w, h),
                ),
                alpha = fillProgress,
            )
        }

        // 3) Five-point star pops in
        if (starScale > 0f) {
            val sc = Offset(w * 0.74f, h * 0.30f)
            val r = w * 0.085f * starScale
            val starPath = Path()
            for (i in 0 until 10) {
                val angle = -PI / 2 + i * PI / 5
                val rr = if (i % 2 == 0) r else r * 0.45f
                val p = Offset(sc.x + (rr * cos(angle)).toFloat(), sc.y + (rr * sin(angle)).toFloat())
                if (i == 0) starPath.moveTo(p.x, p.y) else starPath.lineTo(p.x, p.y)
            }
            starPath.close()
            drawPath(starPath, color = Gold80)
        }
    }
}

/** Title whose letters rise into place one after another. */
@Composable
private fun CascadingTitle(text: String, progress: Float) {
    val n = text.length
    Row {
        text.forEachIndexed { i, ch ->
            // Each letter gets its own window of the overall progress.
            val start = i / (n * 1.6f)
            val local = ((progress - start) / (1f - start)).coerceIn(0f, 1f)
            val eased = EaseOutBack.transform(local)
            Text(
                ch.toString(),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp, letterSpacing = 0.5.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    alpha = local
                    translationY = (1f - eased) * 26f
                },
            )
        }
    }
}
