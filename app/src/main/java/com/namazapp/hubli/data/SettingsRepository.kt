package com.namazapp.hubli.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.namazapp.hubli.domain.AsrMadhab
import com.namazapp.hubli.domain.Prayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Dark / light / follow-system. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Everything the user can configure, as one immutable snapshot. */
data class UserSettings(
    val madhab: AsrMadhab = AsrMadhab.HANAFI,
    val use24Hour: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.DARK,
    /** Master switch for prayer-time notifications. */
    val notificationsEnabled: Boolean = true,
    /** Per-prayer switches (Sunrise is off by default – it is not a prayer). */
    val notifyFor: Map<Prayer, Boolean> = Prayer.entries.associateWith { it.isSalah },
    /** Minutes before each prayer to send a heads-up; 0 disables the pre-reminder. */
    val reminderMinutes: Int = 0,
    val vibrate: Boolean = true,
    /** Manual correction (in days, −2..+2) applied to the calculated Hijri date. */
    val hijriAdjustment: Int = 0,
) {
    fun isNotifyOn(prayer: Prayer): Boolean = notificationsEnabled && (notifyFor[prayer] ?: false)
}

class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    val settings: Flow<UserSettings> = store.data.map { p -> p.toSettings() }

    suspend fun current(): UserSettings = settings.first()

    suspend fun setMadhab(madhab: AsrMadhab) = store.edit { it[KEY_MADHAB] = madhab.name }
    suspend fun setUse24Hour(value: Boolean) = store.edit { it[KEY_24H] = value }
    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[KEY_THEME] = mode.name }
    suspend fun setNotificationsEnabled(value: Boolean) = store.edit { it[KEY_NOTIF_MASTER] = value }
    suspend fun setNotify(prayer: Prayer, value: Boolean) = store.edit { it[notifyKey(prayer)] = value }
    suspend fun setReminderMinutes(minutes: Int) = store.edit { it[KEY_REMINDER] = minutes.coerceIn(0, 60) }
    suspend fun setVibrate(value: Boolean) = store.edit { it[KEY_VIBRATE] = value }
    suspend fun setHijriAdjustment(days: Int) = store.edit { it[KEY_HIJRI_ADJ] = days.coerceIn(-2, 2) }

    private fun Preferences.toSettings() = UserSettings(
        madhab = this[KEY_MADHAB]?.let { runCatching { AsrMadhab.valueOf(it) }.getOrNull() }
            ?: AsrMadhab.HANAFI,
        use24Hour = this[KEY_24H] ?: false,
        themeMode = this[KEY_THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.DARK,
        notificationsEnabled = this[KEY_NOTIF_MASTER] ?: true,
        notifyFor = Prayer.entries.associateWith { p -> this[notifyKey(p)] ?: p.isSalah },
        reminderMinutes = this[KEY_REMINDER] ?: 0,
        vibrate = this[KEY_VIBRATE] ?: true,
        hijriAdjustment = this[KEY_HIJRI_ADJ] ?: 0,
    )

    private companion object {
        val KEY_MADHAB = stringPreferencesKey("asr_madhab")
        val KEY_24H = booleanPreferencesKey("use_24h")
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_NOTIF_MASTER = booleanPreferencesKey("notifications_enabled")
        val KEY_REMINDER = intPreferencesKey("reminder_minutes")
        val KEY_VIBRATE = booleanPreferencesKey("vibrate")
        val KEY_HIJRI_ADJ = intPreferencesKey("hijri_adjustment")
        fun notifyKey(prayer: Prayer) = booleanPreferencesKey("notify_${prayer.name.lowercase()}")
    }
}
