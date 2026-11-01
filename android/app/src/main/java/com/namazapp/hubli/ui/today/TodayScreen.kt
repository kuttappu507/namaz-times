package com.namazapp.hubli.ui.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.namazapp.hubli.R
import com.namazapp.hubli.domain.Prayer
import com.namazapp.hubli.domain.TimeFormat
import com.namazapp.hubli.ui.TodayUiState
import com.namazapp.hubli.ui.components.AmbientOrbs
import com.namazapp.hubli.ui.components.Pill
import com.namazapp.hubli.ui.components.PrayerIconBadge
import com.namazapp.hubli.ui.components.RollingText
import com.namazapp.hubli.ui.components.SectionHeader
import com.namazapp.hubli.ui.components.Starfield
import com.namazapp.hubli.ui.components.bounceOnChange
import com.namazapp.hubli.ui.components.breathe
import com.namazapp.hubli.ui.components.pressScale
import com.namazapp.hubli.ui.components.rememberPulse
import com.namazapp.hubli.ui.components.ShimmerOverlay
import com.namazapp.hubli.ui.components.scrollScrim
import com.namazapp.hubli.ui.components.staggeredEntrance
import com.namazapp.hubli.ui.theme.AppTheme
import com.namazapp.hubli.ui.theme.skyPaletteFor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Home screen. The greeting/date header and the hero card are **pinned**; only the
 * "Today's timings" list below them scrolls, dissolving under the hero via a fade edge.
 */
@Composable
fun TodayScreen(
    state: TodayUiState?,
    contentPadding: PaddingValues,
    notificationsBlocked: Boolean,
    exactAlarmsBlocked: Boolean,
    onFixNotifications: () -> Unit,
    onFixExactAlarms: () -> Unit,
) {
    if (state == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val use24 = state.settings.use24Hour
    val madhab = state.settings.madhab

    val warning: Pair<String, () -> Unit>? = when {
        !state.settings.notificationsEnabled -> null
        notificationsBlocked -> "Notifications are blocked · tap to allow" to onFixNotifications
        exactAlarmsBlocked -> "Allow “Alarms & reminders” for exact alerts" to onFixExactAlarms
        else -> null
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        // ---------- Pinned part ----------
        Column(Modifier.padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(10.dp))
            Header(state, Modifier.staggeredEntrance(0))
            Spacer(Modifier.height(14.dp))
            HeroCard(state, Modifier.staggeredEntrance(1))
            if (warning != null) {
                Spacer(Modifier.height(12.dp))
                WarningCard(text = warning.first, onClick = warning.second, modifier = Modifier.staggeredEntrance(2))
            }
            Spacer(Modifier.height(18.dp))
            SectionHeader(
                "Today's timings",
                modifier = Modifier.staggeredEntrance(2),
                trailing = {
                    Pill(
                        text = madhab.label.uppercase(Locale.ENGLISH),
                        container = AppTheme.extended.goldContainer,
                        content = AppTheme.extended.onGoldContainer,
                    )
                },
            )
            Spacer(Modifier.height(6.dp))
        }

        // ---------- Scrolling part ----------
        // The list opens on the *ongoing* prayer, so "NOW" and "NEXT" are on screen without any
        // scrolling; earlier prayers sit above (scroll up to see them). When the prayer changes
        // the list glides to the new one – unless the user is scrolling at that moment.
        val currentIndex = Prayer.entries.indexOf(state.current.prayer)
            .takeIf { state.current.at.toLocalDate() == state.today.date } ?: -1
        val nextIndex = Prayer.entries.indexOf(state.next.prayer)
            .takeIf { state.next.at.toLocalDate() == state.today.date } ?: -1
        val anchor = when {
            currentIndex >= 0 -> currentIndex
            nextIndex >= 0 -> nextIndex
            else -> 0
        }
        val nextIsTomorrow = state.next.at.toLocalDate() != state.today.date
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = anchor)
        LaunchedEffect(anchor) {
            if (!listState.isScrollInProgress) listState.animateScrollToItem(anchor)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .scrollScrim(listState, height = 14.dp),
            contentPadding = PaddingValues(
                start = 18.dp, end = 18.dp, top = 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(Prayer.entries, key = { _, p -> p.name }) { index, prayer ->
                val time = state.today.timeOf(prayer, madhab)
                val at = LocalDateTime.of(state.today.date, time)
                // Remembered so the card's parameters stay equal between the once-a-second
                // clock ticks and Compose can skip it entirely. Stagger counts from the anchor,
                // so the cards that are actually on screen appear first.
                val entrance = remember { Modifier.staggeredEntrance(3 + (index - anchor).coerceAtLeast(0), staggerMs = 55) }
                PrayerCard(
                    prayer = prayer,
                    name = if (prayer.isJumuah(state.today.date)) "Jumu'ah" else prayer.displayName,
                    time = TimeFormat.timeNumber(time, use24),
                    amPm = TimeFormat.amPm(time, use24),
                    isCurrent = index == currentIndex,
                    isNext = index == nextIndex,
                    isPast = !state.now.isBefore(at),
                    notifyOn = state.settings.isNotifyOn(prayer),
                    modifier = entrance,
                )
            }
            if (nextIsTomorrow) {
                // After Isha the upcoming prayer is tomorrow's Fajr – show it so "next" is
                // always in the list too.
                item(key = "tomorrow") {
                    val next = state.next
                    val time = next.at.toLocalTime()
                    PrayerCard(
                        prayer = next.prayer,
                        name = if (next.prayer.isJumuah(next.at.toLocalDate())) "Jumu'ah" else next.prayer.displayName,
                        note = "Tomorrow · ${next.prayer.arabicName}",
                        time = TimeFormat.timeNumber(time, use24),
                        amPm = TimeFormat.amPm(time, use24),
                        isCurrent = false,
                        isNext = true,
                        isPast = false,
                        notifyOn = state.settings.isNotifyOn(next.prayer),
                        modifier = remember { Modifier.staggeredEntrance(4, staggerMs = 55) },
                    )
                }
            }
            item(key = "footer") {
                Text(
                    "Asr per ${madhab.label} school · change in Settings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .staggeredEntrance(10, staggerMs = 55)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/* ------------------------------------------------------------------------------------------ */

@Composable
private fun Header(state: TodayUiState, modifier: Modifier = Modifier) {
    val dateFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
    val greeting = when (state.now.hour) {
        in 4..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
    Row(modifier.padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                "${stringResource(R.string.greeting)} · $greeting",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(2.dp))
            Text(state.now.toLocalDate().format(dateFmt), style = MaterialTheme.typography.headlineMedium)
            Text(
                "${state.hijri.formatted()}  ·  ${state.hijri.monthNameArabic}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.LocationOn, contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                stringResource(R.string.city_name),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/* ------------------------------------------------------------------------------------------ */

/** Sky-gradient card with drifting orbs, shimmer, and the odometer countdown ring. */
@Composable
private fun HeroCard(state: TodayUiState, modifier: Modifier = Modifier) {
    val use24 = state.settings.use24Hour
    val palette = skyPaletteFor(state.now.hour)
    val top by animateColorAsState(palette.top, tween(1500), label = "top")
    val mid by animateColorAsState(palette.mid, tween(1500), label = "mid")
    val bottom by animateColorAsState(palette.bottom, tween(1500), label = "bottom")
    val glow by animateColorAsState(palette.glow, tween(1500), label = "glow")
    val progress by animateFloatAsState(state.progress, tween(900, easing = EaseOutCubic), label = "progress")

    val nextName = if (state.next.prayer.isJumuah(state.next.at.toLocalDate())) "Jumu'ah" else state.next.prayer.displayName
    val currentLabel = when {
        state.current.prayer == Prayer.SUNRISE -> "Fajr has ended"
        state.current.prayer.isJumuah(state.current.at.toLocalDate()) -> "Now · Jumu'ah"
        else -> "Now · ${state.current.prayer.displayName}"
    }
    val nextIsTomorrow = state.next.at.toLocalDate() != state.now.toLocalDate()
    val isNight = state.now.hour >= 19 || state.now.hour < 5
    val shape = RoundedCornerShape(30.dp)

    Box(
        modifier
            .fillMaxWidth()
            .shadow(24.dp, shape, ambientColor = mid.copy(alpha = 0.45f), spotColor = mid.copy(alpha = 0.6f))
            .clip(shape)
            .background(Brush.linearGradient(listOf(top, mid, bottom), start = Offset.Zero, end = Offset.Infinite)),
    ) {
        ShimmerOverlay(Modifier.matchParentSize())
        AmbientOrbs(
            modifier = Modifier.matchParentSize(),
            colors = listOf(glow, palette.accent, Color.White),
            alpha = 0.40f,
            speedMs = 11000,
        )
        if (isNight) Starfield(Modifier.matchParentSize(), count = 28, seed = 3)

        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.07f), radius = size.minDimension * 0.9f, center = Offset(size.width * 1.02f, -size.height * 0.15f))
            drawCircle(Color.White.copy(alpha = 0.05f), radius = size.minDimension * 0.6f, center = Offset(-size.width * 0.05f, size.height * 1.1f))
        }

        Row(Modifier.padding(horizontal = 22.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Pill(
                    text = if (nextIsTomorrow) "NEXT · TOMORROW" else "UP NEXT",
                    container = Color.White.copy(alpha = 0.18f),
                    content = Color.White,
                )
                Spacer(Modifier.height(10.dp))
                AnimatedContent(
                    targetState = nextName,
                    transitionSpec = {
                        (fadeIn(tween(400)) + slideInVertically { it / 3 }) togetherWith (fadeOut(tween(200)) + slideOutVertically { -it / 3 })
                    },
                    label = "nextName",
                ) { name ->
                    Column {
                        Text(
                            name,
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            state.next.prayer.arabicName,
                            style = MaterialTheme.typography.titleMedium,
                            color = palette.accent,
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        TimeFormat.timeNumber(state.next.at.toLocalTime(), use24),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (!use24) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            TimeFormat.amPm(state.next.at.toLocalTime(), use24),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 7.dp),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .breathe(0.8f, 1.25f, 1400)
                            .background(palette.accent, CircleShape),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        currentLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            CountdownRing(
                progress = progress,
                label = TimeFormat.countdown(state.timeToNext),
                accent = palette.accent,
                modifier = Modifier.breathe(0.985f, 1.015f, 3200),
            )
        }
    }
}

@Composable
private fun CountdownRing(progress: Float, label: String, accent: Color, modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(132.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2 + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawCircle(
                brush = Brush.radialGradient(listOf(Color.White.copy(alpha = 0.14f), Color.Transparent)),
                radius = size.minDimension / 2,
            )
            drawArc(
                color = Color.White.copy(alpha = 0.20f),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            val sweep = 360f * progress.coerceIn(0f, 1f)
            drawArc(
                brush = Brush.sweepGradient(listOf(accent, Color.White, accent)),
                startAngle = -90f, sweepAngle = sweep, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (sweep > 2f) {
                val angle = Math.toRadians((sweep - 90f).toDouble())
                val r = arcSize.width / 2
                val head = Offset(center.x + (r * Math.cos(angle)).toFloat(), center.y + (r * Math.sin(angle)).toFloat())
                drawCircle(Color.White.copy(alpha = 0.55f), radius = stroke * 0.9f, center = head)
                drawCircle(Color.White, radius = stroke * 0.42f, center = head)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RollingText(
                text = label,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, letterSpacing = 0.5.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "remaining",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/* ------------------------------------------------------------------------------------------ */

/**
 * One prayer as its own card. The NEXT card gets a gradient tint, a glowing coloured border
 * and a slow pulse so it stands out; past prayers dim.
 */
@Composable
private fun PrayerCard(
    prayer: Prayer,
    name: String,
    time: String,
    note: String? = null,
    amPm: String,
    isCurrent: Boolean,
    isNext: Boolean,
    isPast: Boolean,
    notifyOn: Boolean,
    modifier: Modifier = Modifier,
) {
    val ext = AppTheme.extended
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(22.dp)

    val tintTarget = when {
        isNext -> accent.copy(alpha = if (ext.isDark) 0.22f else 0.14f)
        isCurrent && prayer.isSalah -> ext.gold.copy(alpha = if (ext.isDark) 0.16f else 0.12f)
        else -> Color.Transparent
    }
    val tint by animateColorAsState(tintTarget, tween(500), label = "tint")
    val borderTarget = when {
        isNext -> accent.copy(alpha = 0.55f)
        isCurrent && prayer.isSalah -> ext.gold.copy(alpha = 0.5f)
        ext.isDark -> Color.White.copy(alpha = 0.08f)
        else -> Color.White.copy(alpha = 0.9f)
    }
    val border by animateColorAsState(borderTarget, tween(300), label = "border")
    val alpha by animateFloatAsState(if (isPast && !isCurrent) 0.62f else 1f, tween(400), label = "rowAlpha")
    val cardStart = ext.cardGradientStart
    val cardEnd = ext.cardGradientEnd

    val textColor = if (prayer.isSalah) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isNext) 14.dp else if (ext.isDark) 6.dp else 10.dp,
                shape = shape,
                ambientColor = if (isNext) accent.copy(alpha = 0.35f) else Color.Black.copy(alpha = if (ext.isDark) 0.4f else 0.08f),
                spotColor = if (isNext) accent.copy(alpha = 0.55f) else Color.Black.copy(alpha = if (ext.isDark) 0.5f else 0.14f),
            )
            .graphicsLayer { this.alpha = alpha }
            .clip(shape)
            .drawWithCache {
                // Brushes are built once per size/colour change – not once per frame.
                val base = Brush.verticalGradient(listOf(cardStart, cardEnd))
                val wash = Brush.horizontalGradient(listOf(tint, tint.copy(alpha = tint.alpha * 0.25f)))
                onDrawBehind {
                    drawRect(base)
                    if (tint.alpha > 0.002f) drawRect(wash)
                }
            }
            .border(1.dp, border, shape),
    ) {
        if (isNext) NextGlow(shape = shape, color = accent, modifier = Modifier.matchParentSize())
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrayerIconBadge(
                prayer = prayer,
                active = isNext || isCurrent,
                modifier = Modifier.bounceOnChange(isNext),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleMedium, color = textColor)
                    Spacer(Modifier.width(8.dp))
                    when {
                        isCurrent && prayer.isSalah -> Pill("NOW", container = ext.goldContainer, content = ext.onGoldContainer)
                        isNext -> Pill("NEXT", container = accent, content = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                Text(
                    note ?: prayer.arabicName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = if (notifyOn) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                contentDescription = if (notifyOn) "Notification on" else "Notification off",
                tint = if (notifyOn) accent else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(14.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(time, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textColor)
                if (amPm.isNotEmpty()) {
                    Spacer(Modifier.width(3.dp))
                    Text(
                        amPm,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
            }
        }
    }
}

/**
 * Pulsing highlight border for the NEXT card. The pulse is read only inside `graphicsLayer`
 * (alpha) so each frame updates one layer property – nothing recomposes or re-lays out.
 */
@Composable
private fun NextGlow(shape: RoundedCornerShape, color: Color, modifier: Modifier = Modifier) {
    val pulse = rememberPulse(0.15f, 1f, 1800)
    Box(
        modifier
            .graphicsLayer { alpha = pulse.value }
            .border(1.5.dp, color.copy(alpha = 0.85f), shape),
    )
}

@Composable
private fun WarningCard(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interaction,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = modifier.pressScale(interaction),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
