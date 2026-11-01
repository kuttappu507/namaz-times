package com.namazapp.hubli.data

import java.time.LocalTime
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * Parses the printed timetable CSV that ships in `assets/namaz_timings.csv`.
 *
 * The file looks like:
 * ```
 * Complete Timings -hubli - All 12 Months (odd dates as printed),,,,,,,,
 * ,,,,,,,,
 * Month,Date,Fajr,Sunrise,Zuhr,Asr (Shafi),Asr (Hanafi),Maghrib,Isha
 * January,1,5:38,6:55,12:37,3:51,4:39,6:15,7:31
 * ```
 *
 * Quirks handled here:
 *  * Only odd dates are printed (1, 3, 5 … 31). Unprinted dates carry forward the previous
 *    printed day's times – see [Timetable].
 *  * Every month is printed with 16 rows (dates 1..31), including days that do not exist
 *    (e.g. "February 29/31"). Those rows are kept as raw entries; [Timetable] only
 *    materialises real calendar dates.
 *  * Times are printed in 12-hour form without AM/PM. Fajr/Sunrise are AM; Zuhr is around
 *    noon (12:xx is already PM); Asr/Maghrib/Isha are PM.
 */
object TimetableParser {

    /** A raw printed row; month/day are as printed, not validated against the calendar. */
    data class RawEntry(
        val month: Month,
        val day: Int,
        val fajr: LocalTime,
        val sunrise: LocalTime,
        val zuhr: LocalTime,
        val asrShafi: LocalTime,
        val asrHanafi: LocalTime,
        val maghrib: LocalTime,
        val isha: LocalTime,
    )

    private val monthsByName: Map<String, Month> = Month.entries.associateBy {
        it.getDisplayName(TextStyle.FULL, Locale.ENGLISH).lowercase()
    }

    fun parse(csv: String): List<RawEntry> {
        val entries = ArrayList<RawEntry>(200)
        csv.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach
            val cols = line.split(',').map { it.trim() }
            if (cols.size < 9) return@forEach
            val month = monthsByName[cols[0].lowercase()] ?: return@forEach   // skips header rows
            val day = cols[1].toIntOrNull() ?: return@forEach
            if (day !in 1..31) return@forEach
            entries += RawEntry(
                month = month,
                day = day,
                fajr = parseTime(cols[2], pm = false),
                sunrise = parseTime(cols[3], pm = false),
                zuhr = parseTime(cols[4], pm = true),
                asrShafi = parseTime(cols[5], pm = true),
                asrHanafi = parseTime(cols[6], pm = true),
                maghrib = parseTime(cols[7], pm = true),
                isha = parseTime(cols[8], pm = true),
            )
        }
        return entries
    }

    /**
     * Converts a printed "h:mm" (12-hour, no AM/PM marker) into a [LocalTime].
     * Hour 12 is treated as noon for PM columns and midnight for AM columns.
     */
    internal fun parseTime(text: String, pm: Boolean): LocalTime {
        val parts = text.split(':')
        require(parts.size == 2) { "Bad time '$text'" }
        var hour = parts[0].trim().toInt()
        val minute = parts[1].trim().toInt()
        require(hour in 0..12 && minute in 0..59) { "Bad time '$text'" }
        if (pm) {
            if (hour != 12) hour += 12
        } else {
            if (hour == 12) hour = 0
        }
        return LocalTime.of(hour, minute)
    }
}
