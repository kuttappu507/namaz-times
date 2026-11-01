package com.namazapp.hubli

import android.app.Application
import com.namazapp.hubli.data.PrayerRepository
import com.namazapp.hubli.data.SettingsRepository
import com.namazapp.hubli.notifications.NotificationHelper

/**
 * Tiny manual service locator – the app is small enough that a DI framework would be overkill.
 * Repositories are shared between the UI and the broadcast receivers.
 */
class NamazApplication : Application() {

    val prayerRepository: PrayerRepository by lazy { PrayerRepository(this) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
    }
}
