package com.namazapp.hubli.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.namazapp.hubli.MainActivity
import com.namazapp.hubli.R
import com.namazapp.hubli.domain.Prayer
import com.namazapp.hubli.domain.TimeFormat
import java.time.LocalDate
import java.time.LocalTime

object NotificationHelper {

    const val CHANNEL_PRAYER = "prayer_times"
    const val CHANNEL_REMINDER = "prayer_reminders"

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val prayer = NotificationChannel(
            CHANNEL_PRAYER,
            context.getString(R.string.channel_prayer_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.channel_prayer_desc)
            setSound(sound, attrs)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        val reminder = NotificationChannel(
            CHANNEL_REMINDER,
            context.getString(R.string.channel_reminder_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.channel_reminder_desc)
            setSound(sound, attrs)
            enableVibration(true)
        }
        nm.createNotificationChannels(listOf(prayer, reminder))
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun showPrayerNotification(
        context: Context,
        prayer: Prayer,
        time: LocalTime,
        date: LocalDate,
        isReminder: Boolean,
        reminderMinutes: Int,
        use24Hour: Boolean,
        vibrate: Boolean,
    ) {
        if (!hasPermission(context)) return

        val name = if (prayer.isJumuah(date)) "Jumu'ah" else prayer.displayName
        val timeText = TimeFormat.time(time, use24Hour)
        val city = context.getString(R.string.city_name)

        val title: String
        val body: String
        if (isReminder) {
            title = "$name in $reminderMinutes min"
            body = "$name begins at $timeText in $city"
        } else if (prayer == Prayer.SUNRISE) {
            title = "Sunrise · $timeText"
            body = "Fajr time has ended in $city"
        } else {
            title = "$name · $timeText"
            body = "It is time for $name prayer in $city"
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(
            context, if (isReminder) CHANNEL_REMINDER else CHANNEL_PRAYER,
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(if (isReminder) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setShowWhen(true)

        // Channels own sound/vibration on O+, but per-notification vibration can still be
        // suppressed by handing over an empty pattern.
        if (!vibrate) builder.setVibrate(longArrayOf(0L))

        val id = notificationId(prayer, isReminder)
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    fun notificationId(prayer: Prayer, isReminder: Boolean): Int =
        1000 + prayer.ordinal * 2 + if (isReminder) 1 else 0
}
