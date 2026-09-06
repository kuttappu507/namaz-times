package com.namazapp.hubli.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.namazapp.hubli.NamazApplication
import com.namazapp.hubli.data.PrayerRepository
import com.namazapp.hubli.data.SettingsRepository
import com.namazapp.hubli.data.ThemeMode
import com.namazapp.hubli.data.UserSettings
import com.namazapp.hubli.domain.AsrMadhab
import com.namazapp.hubli.domain.HijriDate
import com.namazapp.hubli.domain.Prayer
import com.namazapp.hubli.domain.PrayerDay
import com.namazapp.hubli.domain.PrayerEvent
import com.namazapp.hubli.notifications.PrayerAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/** Everything the Today screen needs, recomputed once per second. */
data class TodayUiState(
    val now: LocalDateTime,
    val today: PrayerDay,
    val hijri: HijriDate,
    val current: PrayerEvent,
    val next: PrayerEvent,
    val timeToNext: Duration,
    /** 0f..1f – how far we are through the current prayer window. */
    val progress: Float,
    val settings: UserSettings,
)

data class MonthlyUiState(
    val yearMonth: YearMonth,
    val days: List<PrayerDay>,
    val today: LocalDate,
    val settings: UserSettings,
)

class AppViewModel(
    application: Application,
    private val prayers: PrayerRepository,
    private val settingsRepo: SettingsRepository,
) : AndroidViewModel(application) {

    private val scheduler = PrayerAlarmScheduler(application)

    val settings: StateFlow<UserSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    /** A 1 Hz clock, used by the countdown. */
    private val ticker = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(1_000L - (System.currentTimeMillis() % 1_000L))
        }
    }

    val todayState: StateFlow<TodayUiState?> = combine(ticker, settings) { now, s ->
        val today = prayers.day(now.toLocalDate())
        // `current` includes Sunrise so the UI can say "Fajr has ended"; the progress ring
        // measures the span between the two surrounding *prayers* so it never jumps at sunrise.
        val current = prayers.currentEvent(now, s.madhab, includeSunrise = true)
        val currentSalah = prayers.currentEvent(now, s.madhab, includeSunrise = false)
        val next = prayers.nextEvent(now, s.madhab)
        val window = Duration.between(currentSalah.at, next.at).seconds.coerceAtLeast(1)
        val elapsed = Duration.between(currentSalah.at, now).seconds.coerceIn(0, window)
        TodayUiState(
            now = now,
            today = today,
            hijri = HijriDate.from(now.toLocalDate(), s.hijriAdjustment),
            current = current,
            next = next,
            timeToNext = Duration.between(now, next.at),
            progress = elapsed.toFloat() / window.toFloat(),
            settings = s,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ---- Monthly ----------------------------------------------------------------------------

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val monthlyState: StateFlow<MonthlyUiState> = combine(_selectedMonth, settings) { ym, s ->
        MonthlyUiState(
            yearMonth = ym,
            days = prayers.month(ym),
            today = LocalDate.now(),
            settings = s,
        )
    }.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000),
        MonthlyUiState(YearMonth.now(), prayers.month(YearMonth.now()), LocalDate.now(), UserSettings()),
    )

    fun previousMonth() { _selectedMonth.value = _selectedMonth.value.minusMonths(1) }
    fun nextMonth() { _selectedMonth.value = _selectedMonth.value.plusMonths(1) }
    fun jumpToCurrentMonth() { _selectedMonth.value = YearMonth.now() }

    // ---- Settings ---------------------------------------------------------------------------

    fun setMadhab(m: AsrMadhab) = viewModelScope.launch { settingsRepo.setMadhab(m) }
    fun setUse24Hour(v: Boolean) = viewModelScope.launch { settingsRepo.setUse24Hour(v) }
    fun setThemeMode(m: ThemeMode) = viewModelScope.launch { settingsRepo.setThemeMode(m) }
    fun setNotificationsEnabled(v: Boolean) = viewModelScope.launch { settingsRepo.setNotificationsEnabled(v) }
    fun setNotify(p: Prayer, v: Boolean) = viewModelScope.launch { settingsRepo.setNotify(p, v) }
    fun setReminderMinutes(m: Int) = viewModelScope.launch { settingsRepo.setReminderMinutes(m) }
    fun setVibrate(v: Boolean) = viewModelScope.launch { settingsRepo.setVibrate(v) }
    fun setHijriAdjustment(d: Int) = viewModelScope.launch { settingsRepo.setHijriAdjustment(d) }

    fun canScheduleExactAlarms(): Boolean = scheduler.canScheduleExact()

    /** Called when permissions change or the app comes to the foreground. */
    fun rescheduleAlarms() = viewModelScope.launch(Dispatchers.IO) {
        scheduler.scheduleAll(prayers, settingsRepo.current())
    }

    init {
        // Whenever a notification-relevant setting changes, rebuild the alarm schedule.
        viewModelScope.launch {
            settingsRepo.settings
                .map { listOf(it.notificationsEnabled, it.notifyFor, it.reminderMinutes, it.madhab) }
                .distinctUntilChanged()
                .drop(1) // initial value is handled by rescheduleAlarms() from MainActivity
                .collect {
                    withContext(Dispatchers.IO) { scheduler.scheduleAll(prayers, settingsRepo.current()) }
                }
        }
    }

    companion object {
        val Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as NamazApplication
                return AppViewModel(app, app.prayerRepository, app.settingsRepository) as T
            }
        }
    }
}
