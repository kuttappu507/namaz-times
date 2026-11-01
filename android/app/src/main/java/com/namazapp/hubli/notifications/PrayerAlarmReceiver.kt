package com.namazapp.hubli.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.namazapp.hubli.NamazApplication
import com.namazapp.hubli.domain.Prayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** Fires at a prayer (or pre-reminder) time: posts the notification and re-arms the chain. */
class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != PrayerAlarmScheduler.ACTION_PRAYER_ALARM) return
        val prayerName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER) ?: return
        val prayer = runCatching { Prayer.valueOf(prayerName) }.getOrNull() ?: return
        val isReminder = intent.getBooleanExtra(PrayerAlarmScheduler.EXTRA_IS_REMINDER, false)

        val app = context.applicationContext as NamazApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = app.settingsRepository.current()
                val date = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_DATE)
                    ?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
                val firedAt = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_TIME)
                    ?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
                    ?.let { LocalDateTime.of(date, it) }
                if (settings.isNotifyOn(prayer)) {
                    val time = app.prayerRepository.day(date).timeOf(prayer, settings.madhab)
                    NotificationHelper.showPrayerNotification(
                        context = context,
                        prayer = prayer,
                        time = time,
                        date = date,
                        isReminder = isReminder,
                        reminderMinutes = settings.reminderMinutes,
                        use24Hour = settings.use24Hour,
                        vibrate = settings.vibrate,
                    )
                }
                // Roll every alarm forward (this one now points at tomorrow).
                PrayerAlarmScheduler(context).scheduleAll(app.prayerRepository, settings, notBefore = firedAt)
            } catch (t: Throwable) {
                Log.e(TAG, "Failed handling prayer alarm", t)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "PrayerAlarmReceiver"
    }
}
