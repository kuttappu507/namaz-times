package com.namazapp.hubli.ui.monthly

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.namazapp.hubli.domain.AsrMadhab
import com.namazapp.hubli.domain.HijriDate
import com.namazapp.hubli.domain.Prayer
import com.namazapp.hubli.domain.PrayerDay
import com.namazapp.hubli.domain.TimeFormat
import com.namazapp.hubli.ui.MonthlyUiState
import com.namazapp.hubli.ui.components.scrollScrim
import com.namazapp.hubli.ui.components.staggeredEntrance
import com.namazapp.hubli.ui.theme.AppTheme
import com.namazapp.hubli.ui.theme.Emerald40
import com.namazapp.hubli.ui.theme.Emerald60
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MonthlyScreen(
    state: MonthlyUiState,
    contentPadding: PaddingValues,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    val use24 = state.settings.use24Hour
    val madhab = state.settings.madhab
    val isCurrentMonth = state.yearMonth == YearMonth.from(state.today)

    // Track navigation direction so the month slides the right way.
    var lastMonth by remember { mutableIntStateOf(state.yearMonth.year * 12 + state.yearMonth.monthValue) }
    val thisMonthIdx = state.yearMonth.year * 12 + state.yearMonth.monthValue
    val forward = thisMonthIdx >= lastMonth
    LaunchedEffect(thisMonthIdx) { lastMonth = thisMonthIdx }

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        MonthHeader(
            yearMonth = state.yearMonth,
            hijriRange = hijriRangeLabel(state),
            isCurrentMonth = isCurrentMonth,
            forward = forward,
            onPrevious = onPrevious,
            onNext = onNext,
            onToday = onToday,
            modifier = Modifier.staggeredEntrance(0),
        )
        ColumnHeaders(madhabLabel = madhab.label, modifier = Modifier.staggeredEntrance(1))

        AnimatedContent(
            targetState = state.yearMonth,
            transitionSpec = {
                val dir = if (forward) 1 else -1
                (slideInHorizontally(tween(380, easing = EaseOutCubic)) { dir * it / 3 } + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(300)) { -dir * it / 3 } + fadeOut(tween(180)))
            },
            label = "month",
            modifier = Modifier.fillMaxSize(),
        ) { ym ->
            val days = if (ym == state.yearMonth) state.days else emptyList()
            val listState = rememberLazyListState()
            LaunchedEffect(ym, days.size) {
                if (days.isNotEmpty() && ym == YearMonth.from(state.today)) {
                    listState.scrollToItem((state.today.dayOfMonth - 3).coerceAtLeast(0))
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().scrollScrim(listState, height = 12.dp),
                contentPadding = PaddingValues(
                    start = 14.dp, end = 14.dp, top = 8.dp,
                    bottom = contentPadding.calculateBottomPadding() + 20.dp,
                ),
            ) {
                itemsIndexed(days, key = { _, d -> d.date.toEpochDay() }) { index, day ->
                    DayRow(
                        day = day,
                        isToday = day.date == state.today,
                        use24 = use24,
                        madhab = madhab,
                        hijriAdjustment = state.settings.hijriAdjustment,
                        striped = index % 2 == 1,
                        modifier = if (index < 14) Modifier.staggeredEntrance(index, staggerMs = 26, durationMs = 380, offsetY = 16.dp, baseDelayMs = 60) else Modifier,
                    )
                }
            }
        }
    }
}

private fun hijriRangeLabel(state: MonthlyUiState): String {
    if (state.days.isEmpty()) return ""
    val first = HijriDate.from(state.days.first().date, state.settings.hijriAdjustment)
    val last = HijriDate.from(state.days.last().date, state.settings.hijriAdjustment)
    return when {
        first.month == last.month && first.year == last.year -> "${first.monthName} ${first.year} AH"
        first.year == last.year -> "${first.monthName} – ${last.monthName} ${first.year} AH"
        else -> "${first.monthName} ${first.year} – ${last.monthName} ${last.year} AH"
    }
}

@Composable
private fun MonthHeader(
    yearMonth: YearMonth,
    hijriRange: String,
    isCurrentMonth: Boolean,
    forward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthFmt = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = onPrevious, modifier = Modifier.size(44.dp)) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
        }
        AnimatedContent(
            targetState = yearMonth,
            transitionSpec = {
                val dir = if (forward) 1 else -1
                (slideInHorizontally(tween(320, easing = EaseOutCubic)) { dir * it / 2 } + fadeIn()) togetherWith
                    (slideOutHorizontally(tween(240)) { -dir * it / 2 } + fadeOut())
            },
            label = "monthTitle",
            modifier = Modifier.weight(1f),
        ) { ym ->
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(ym.format(monthFmt), style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        ym.year.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = AppTheme.extended.gold,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
                Text(
                    hijriRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        FilledTonalIconButton(onClick = onNext, modifier = Modifier.size(44.dp)) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
        }
        Spacer(Modifier.width(6.dp))
        FilledIconButton(
            onClick = onToday,
            enabled = !isCurrentMonth,
            modifier = Modifier.size(44.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Icon(Icons.Rounded.Today, contentDescription = "Jump to current month")
        }
    }
}

private val columnWeights = floatArrayOf(1.4f, 1f, 1f, 1f, 1f, 1f, 1f)

@Composable
private fun ColumnHeaders(madhabLabel: String, modifier: Modifier = Modifier) {
    val labels = listOf("Date", "Fajr", "Sunrise", "Zuhr", "Asr", "Maghrib", "Isha")
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp), spotColor = Emerald40.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Emerald40, Emerald60)))
            .padding(horizontal = 8.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { i, label ->
            Column(
                Modifier.weight(columnWeights[i]),
                horizontalAlignment = if (i == 0) Alignment.Start else Alignment.CenterHorizontally,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    textAlign = if (i == 0) TextAlign.Start else TextAlign.Center,
                    maxLines = 1,
                )
                if (label == "Asr") {
                    Text(
                        madhabLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayRow(
    day: PrayerDay,
    isToday: Boolean,
    use24: Boolean,
    madhab: AsrMadhab,
    hijriAdjustment: Int,
    striped: Boolean,
    modifier: Modifier = Modifier,
) {
    val isFriday = day.date.dayOfWeek == DayOfWeek.FRIDAY
    val ext = AppTheme.extended
    val shape = RoundedCornerShape(14.dp)
    val bgModifier = when {
        isToday -> Modifier
            .shadow(10.dp, shape, spotColor = Emerald40.copy(alpha = 0.45f))
            .background(Brush.horizontalGradient(listOf(Emerald40, Emerald60)), shape)
        isFriday -> Modifier
            .background(ext.goldContainer.copy(alpha = if (ext.isDark) 0.35f else 0.6f), shape)
            .border(1.dp, ext.gold.copy(alpha = 0.35f), shape)
        striped -> Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = if (ext.isDark) 0.9f else 1f), shape)
        else -> Modifier
    }
    val fg = if (isToday) Color.White else MaterialTheme.colorScheme.onSurface
    val fgMuted = if (isToday) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    val hijri = HijriDate.from(day.date, hijriAdjustment)

    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(shape)
            .then(bgModifier)
            .padding(horizontal = 8.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(columnWeights[0])) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(24.dp)
                        .background(
                            when {
                                isToday -> Color.White.copy(alpha = 0.22f)
                                isFriday -> ext.gold.copy(alpha = 0.25f)
                                else -> MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        day.date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp),
                        color = if (isFriday && !isToday) ext.onGoldContainer else fg,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(5.dp))
                Text(
                    day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFriday && !isToday) ext.onGoldContainer else fgMuted,
                )
            }
            Text(
                "${hijri.day} ${hijri.monthShortName}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.2.sp),
                color = fgMuted,
                maxLines = 1,
                modifier = Modifier.padding(start = 2.dp, top = 1.dp),
            )
        }
        listOf(Prayer.FAJR, Prayer.SUNRISE, Prayer.ZUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA)
            .forEachIndexed { i, prayer ->
                Text(
                    TimeFormat.timeNumber(day.timeOf(prayer, madhab), use24),
                    modifier = Modifier.weight(columnWeights[i + 1]),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (prayer == Prayer.SUNRISE) fgMuted else fg,
                    fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
    }
}
