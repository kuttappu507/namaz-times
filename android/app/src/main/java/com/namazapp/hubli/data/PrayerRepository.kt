package com.namazapp.hubli.data

import android.content.Context
import com.namazapp.hubli.domain.AsrMadhab
import com.namazapp.hubli.domain.PrayerDay
import com.namazapp.hubli.domain.PrayerEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * Single source of prayer-time data for the whole app (UI, alarms, boot receiver).
 * Loads the bundled CSV once, lazily, and answers every query from memory.
 */
class PrayerRepository(context: Context) {

    private val appContext = context.applicationContext

    private val timetable: Timetable by lazy {
        val csv = appContext.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        Timetable.fromCsv(csv)
    }

    fun day(date: LocalDate): PrayerDay = timetable.forDate(date)

    fun month(yearMonth: YearMonth): List<PrayerDay> = timetable.forMonth(yearMonth)

    /**
     * The first prayer event strictly after [now], looking across midnight into the following
     * day (so after Isha, "next" is tomorrow's Fajr). Sunrise is skipped when
     * [includeSunrise] is false.
     */
    fun nextEvent(now: LocalDateTime, madhab: AsrMadhab, includeSunrise: Boolean = false): PrayerEvent {
        var date = now.toLocalDate()
        repeat(3) {
            val hit = day(date).events(madhab)
                .filter { includeSunrise || it.prayer.isSalah }
                .firstOrNull { it.at.isAfter(now) }
            if (hit != null) return hit
            date = date.plusDays(1)
        }
        error("No upcoming prayer found") // unreachable: every day has 5 prayers
    }

    /** The most recent prayer event at or before [now]. */
    fun currentEvent(now: LocalDateTime, madhab: AsrMadhab, includeSunrise: Boolean = false): PrayerEvent {
        var date = now.toLocalDate()
        repeat(3) {
            val hit = day(date).events(madhab)
                .filter { includeSunrise || it.prayer.isSalah }
                .lastOrNull { !it.at.isAfter(now) }
            if (hit != null) return hit
            date = date.minusDays(1)
        }
        error("No previous prayer found")
    }

    companion object {
        const val ASSET_NAME = "namaz_timings.csv"
    }
}
