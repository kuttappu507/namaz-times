package com.namazapp.hubli.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.namazapp.hubli.data.ThemeMode
import com.namazapp.hubli.data.UserSettings
import com.namazapp.hubli.domain.AsrMadhab
import com.namazapp.hubli.domain.HijriDate
import com.namazapp.hubli.domain.Prayer
import com.namazapp.hubli.ui.components.GlassCard
import com.namazapp.hubli.ui.components.PrayerIconBadge
import com.namazapp.hubli.ui.components.bounceOnChange
import com.namazapp.hubli.ui.components.staggeredEntrance
import com.namazapp.hubli.ui.theme.AppTheme
import com.namazapp.hubli.ui.theme.Emerald40
import com.namazapp.hubli.ui.theme.Emerald60
import java.time.LocalDate

@Composable
fun SettingsScreen(
    settings: UserSettings,
    contentPadding: PaddingValues,
    notificationPermissionGranted: Boolean,
    exactAlarmAllowed: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onMadhab: (AsrMadhab) -> Unit,
    onUse24Hour: (Boolean) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onNotificationsEnabled: (Boolean) -> Unit,
    onNotify: (Prayer, Boolean) -> Unit,
    onReminderMinutes: (Int) -> Unit,
    onVibrate: (Boolean) -> Unit,
    onHijriAdjustment: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 18.dp, end = 18.dp,
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "title") {
            Column(Modifier.staggeredEntrance(0).padding(start = 4.dp, bottom = 4.dp)) {
                Text("Settings", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Personalise alerts and display",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---------- Notifications ----------
        item(key = "notif") {
            Column(Modifier.staggeredEntrance(1)) {
                SectionTitle("Notifications")
                GlassCard {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        SwitchRow(
                            title = "Prayer-time notifications",
                            subtitle = "Alert when each prayer begins",
                            checked = settings.notificationsEnabled,
                            onCheckedChange = onNotificationsEnabled,
                        )
                        AnimatedVisibility(
                            visible = settings.notificationsEnabled,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Column {
                                if (!notificationPermissionGranted) {
                                    WarningRow("Notification permission is required", "Allow", onRequestNotificationPermission)
                                }
                                if (!exactAlarmAllowed) {
                                    WarningRow("Allow “Alarms & reminders” so alerts arrive exactly on time", "Open", onOpenExactAlarmSettings)
                                }
                                ThinDivider()
                                Text(
                                    "NOTIFY ME FOR",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 18.dp, top = 12.dp, bottom = 2.dp),
                                )
                                Prayer.entries.forEach { prayer ->
                                    val on = settings.notifyFor[prayer] ?: false
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable { onNotify(prayer, !on) }
                                            .padding(horizontal = 18.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        PrayerIconBadge(prayer, size = 34.dp, active = on, modifier = Modifier.bounceOnChange(on))
                                        Spacer(Modifier.width(14.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(prayer.displayName, style = MaterialTheme.typography.bodyLarge)
                                            Text(
                                                if (prayer.isSalah) prayer.arabicName else "Marks the end of Fajr",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        AppSwitch(checked = on, onCheckedChange = { onNotify(prayer, it) })
                                    }
                                }
                                ThinDivider(Modifier.padding(top = 6.dp))
                                ReminderRow(
                                    minutes = settings.reminderMinutes,
                                    onMinutes = onReminderMinutes,
                                )
                                ThinDivider()
                                SwitchRow(title = "Vibrate", subtitle = null, checked = settings.vibrate, onCheckedChange = onVibrate)
                            }
                        }
                    }
                }
            }
        }

        // ---------- Asr ----------
        item(key = "asr") {
            Column(Modifier.staggeredEntrance(2)) {
                SectionTitle("Asr calculation")
                GlassCard {
                    Column(Modifier.padding(18.dp)) {
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            AsrMadhab.entries.forEachIndexed { i, m ->
                                SegmentedButton(
                                    selected = settings.madhab == m,
                                    onClick = { onMadhab(m) },
                                    shape = SegmentedButtonDefaults.itemShape(i, AsrMadhab.entries.size),
                                    colors = segmentedColors(),
                                    label = { Text(m.label) },
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            settings.madhab.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // ---------- Display ----------
        item(key = "display") {
            Column(Modifier.staggeredEntrance(3)) {
                SectionTitle("Display")
                GlassCard {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        SwitchRow(title = "24-hour time", subtitle = null, checked = settings.use24Hour, onCheckedChange = onUse24Hour)
                        ThinDivider()
                        Column(Modifier.padding(18.dp)) {
                            Text("Theme", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(10.dp))
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                ThemeMode.entries.forEachIndexed { i, mode ->
                                    SegmentedButton(
                                        selected = settings.themeMode == mode,
                                        onClick = { onThemeMode(mode) },
                                        shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                                        colors = segmentedColors(),
                                        label = {
                                            Text(
                                                when (mode) {
                                                    ThemeMode.SYSTEM -> "System"
                                                    ThemeMode.LIGHT -> "Light"
                                                    ThemeMode.DARK -> "Dark"
                                                },
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        ThinDivider()
                        Row(
                            Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Hijri date adjustment", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Today: ${HijriDate.from(LocalDate.now(), settings.hijriAdjustment).formatted()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            FilledTonalIconButton(
                                onClick = { onHijriAdjustment(settings.hijriAdjustment - 1) },
                                enabled = settings.hijriAdjustment > -2,
                                modifier = Modifier.size(38.dp),
                            ) { Icon(Icons.Outlined.Remove, contentDescription = "Minus one day") }
                            Box(
                                Modifier
                                    .padding(horizontal = 8.dp)
                                    .size(width = 44.dp, height = 38.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = (if (settings.hijriAdjustment > 0) "+" else "") + settings.hijriAdjustment.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.bounceOnChange(settings.hijriAdjustment),
                                )
                            }
                            FilledTonalIconButton(
                                onClick = { onHijriAdjustment(settings.hijriAdjustment + 1) },
                                enabled = settings.hijriAdjustment < 2,
                                modifier = Modifier.size(38.dp),
                            ) { Icon(Icons.Outlined.Add, contentDescription = "Plus one day") }
                        }
                    }
                }
            }
        }

        // ---------- About ----------
        item(key = "about") {
            Column(Modifier.staggeredEntrance(4)) {
                SectionTitle("About")
                GlassCard {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .background(Brush.linearGradient(listOf(Emerald40, Emerald60)), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Outlined.Info, null, tint = Color.White) }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Namaz Times · Hubli", style = MaterialTheme.typography.titleMedium)
                            Text("Version 1.3.1", style = MaterialTheme.typography.labelSmall, color = AppTheme.extended.gold)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Timings are taken from the printed annual timetable for Hubli, Karnataka. " +
                                    "The sheet lists odd dates; a date that is not printed uses the previous printed " +
                                    "day's times exactly as on the sheet. Hijri dates follow the Umm al-Qura calendar and may " +
                                    "differ by a day from local moon sighting – use the adjustment above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------------------------------ */

/** "Remind me before" – label on the left, a compact segmented picker on the right. */
@Composable
private fun ReminderRow(minutes: Int, onMinutes: (Int) -> Unit) {
    val options = listOf(0, 5, 10, 15, 30)
    Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Remind me before", style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (minutes == 0) "Only at prayer time" else "$minutes minutes before each prayer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            options.forEach { m ->
                val selected = minutes == m
                val bg by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    tween(250), label = "segBg",
                )
                val fg by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(250), label = "segFg",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(bg)
                        .clickable { onMinutes(m) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (m == 0) "Off" else "${m}m",
                        style = MaterialTheme.typography.labelLarge,
                        color = fg,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun segmentedColors() = SegmentedButtonDefaults.colors(
    activeContainerColor = MaterialTheme.colorScheme.primary,
    activeContentColor = MaterialTheme.colorScheme.onPrimary,
    activeBorderColor = MaterialTheme.colorScheme.primary,
    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
)

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 8.dp),
    )
}

@Composable
private fun ThinDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier.padding(horizontal = 18.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun AppSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = MaterialTheme.colorScheme.primary,
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun WarningRow(text: String, action: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f), MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.WarningAmber, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        Text(action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}
