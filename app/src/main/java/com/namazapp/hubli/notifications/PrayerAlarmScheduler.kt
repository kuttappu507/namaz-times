package com.namazapp.hubli.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.namazapp.hubli.data.PrayerRepository
import com.namazapp.hubli.data.UserSettings
import com.namazapp.hubli.domain.Prayer
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules exact alarms for upcoming prayers.
 *
 * Strategy: only the *next* occurrence of each enabled prayer (plus its optional pre-reminder)
 * is scheduled – at most 12 alarms. When any alarm fires, [PrayerAlarmReceiver] shows the
 * notification and calls [scheduleAll] again, which rolls that prayer forward to the next day.
 * [RescheduleReceiver] does the same after reboots / time changes, so the chain never breaks.
 */
class PrayerAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val zone: ZoneId get() = ZoneId.systemDefault()

    /** Whether we are allowed to use exact alarms (always true below Android 12). */
    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * Cancel everything, then (re)schedule according to [settings]. Safe to call repeatedly.
     *
     * @param notBefore when called from an alarm that just fired, the time that alarm was set
     *   for – guards against re-arming the same moment if the system delivered it a little early.
     */
    fun scheduleAll(
        repository: PrayerRepository,
        settings: UserSettings,
        notBefore: LocalDateTime? = null,
    ) {
        cancelAll()
        if (!settings.notificationsEnabled) return
        val realNow = LocalDateTime.now(zone)
        val now = if (notBefore != null && notBefore.isAfter(realNow)) notBefore else realNow
        val exact = canScheduleExact()

        for (prayer in Prayer.entries) {
            if (!settings.isNotifyOn(prayer)) continue
            val next = nextOccurrence(repository, settings, prayer, now)

            setAlarm(prayer, isReminder = false, at = next, exact = exact)

            if (settings.reminderMinutes > 0 && prayer.isSalah) {
                var reminderAt = next.minusMinutes(settings.reminderMinutes.toLong())
                if (!reminderAt.isAfter(now)) {
                    // Reminder window for the upcoming prayer already passed – use the next day's.
                    val after = nextOccurrence(repository, settings, prayer, next)
                    reminderAt = after.minusMinutes(settings.reminderMinutes.toLong())
                }
                setAlarm(prayer, isReminder = true, at = reminderAt, exact = exact)
            }
        }
        Log.d(TAG, "Scheduled alarms (exact=$exact) at ${LocalDateTime.now()}")
    }

    fun cancelAll() {
        for (prayer in Prayer.entries) {
            pendingIntent(prayer, isReminder = false, create = false)?.let {
                alarmManager.cancel(it); it.cancel()
            }
            pendingIntent(prayer, isReminder = true, create = false)?.let {
                alarmManager.cancel(it); it.cancel()
            }
        }
    }

    private fun nextOccurrence(
        repo: PrayerRepository,
        settings: UserSettings,
        prayer: Prayer,
        after: LocalDateTime,
    ): LocalDateTime {
        var date = after.toLocalDate()
        repeat(3) {
            val at = repo.day(date).dateTimeOf(prayer, settings.madhab)
            if (at.isAfter(after)) return at
            date = date.plusDays(1)
        }
        return repo.day(date).dateTimeOf(prayer, settings.madhab)
    }

    private fun setAlarm(prayer: Prayer, isReminder: Boolean, at: LocalDateTime, exact: Boolean) {
        val pi = pendingIntent(prayer, isReminder, create = true, at = at) ?: return
        val triggerMillis = at.atZone(zone).toInstant().toEpochMilli()
        try {
            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
            } else {
                // Falls back to an inexact window (may be delayed by up to ~10 min in Doze).
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm denied, falling back to inexact", e)
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        }
    }

    private fun pendingIntent(
        prayer: Prayer,
        isReminder: Boolean,
        create: Boolean,
        at: LocalDateTime? = null,
    ): PendingIntent? {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_PRAYER_ALARM
            putExtra(EXTRA_PRAYER, prayer.name)
            putExtra(EXTRA_IS_REMINDER, isReminder)
            if (at != null) {
                putExtra(EXTRA_DATE, at.toLocalDate().toString())
                putExtra(EXTRA_TIME, at.toLocalTime().toString())
            }
        }
        val requestCode = NotificationHelper.notificationId(prayer, isReminder)
        var flags = PendingIntent.FLAG_IMMUTABLE
        flags = flags or if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }

    companion object {
        private const val TAG = "PrayerAlarmScheduler"
        const val ACTION_PRAYER_ALARM = "com.namazapp.hubli.action.PRAYER_ALARM"
        const val EXTRA_PRAYER = "prayer"
        const val EXTRA_IS_REMINDER = "is_reminder"
        const val EXTRA_DATE = "date"
        const val EXTRA_TIME = "time"
    }
}
