package com.namazapp.hubli.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** One row of the timetable: the prayer times for a single calendar date. */
data class PrayerDay(
    val date: LocalDate,
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val zuhr: LocalTime,
    val asrShafi: LocalTime,
    val asrHanafi: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
) {
    fun timeOf(prayer: Prayer, madhab: AsrMadhab): LocalTime = when (prayer) {
        Prayer.FAJR -> fajr
        Prayer.SUNRISE -> sunrise
        Prayer.ZUHR -> zuhr
        Prayer.ASR -> if (madhab == AsrMadhab.HANAFI) asrHanafi else asrShafi
        Prayer.MAGHRIB -> maghrib
        Prayer.ISHA -> isha
    }

    fun dateTimeOf(prayer: Prayer, madhab: AsrMadhab): LocalDateTime =
        LocalDateTime.of(date, timeOf(prayer, madhab))

    /** All six events for this day, in chronological order. */
    fun events(madhab: AsrMadhab): List<PrayerEvent> =
        Prayer.entries.map { PrayerEvent(it, dateTimeOf(it, madhab)) }
}

/** A concrete prayer at a concrete moment in time. */
data class PrayerEvent(val prayer: Prayer, val at: LocalDateTime)
