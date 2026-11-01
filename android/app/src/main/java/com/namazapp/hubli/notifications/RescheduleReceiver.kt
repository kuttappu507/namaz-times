package com.namazapp.hubli.notifications

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.namazapp.hubli.NamazApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Alarms do not survive a reboot, and can be off after time/zone changes or when the user grants
 * the exact-alarm permission. This receiver simply rebuilds the schedule from scratch.
 */
class RescheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> Unit
            else -> return
        }
        val app = context.applicationContext as NamazApplication
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = app.settingsRepository.current()
                PrayerAlarmScheduler(context).scheduleAll(app.prayerRepository, settings)
                Log.d(TAG, "Rescheduled after ${intent.action}")
            } catch (t: Throwable) {
                Log.e(TAG, "Reschedule failed", t)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "RescheduleReceiver"
    }
}
