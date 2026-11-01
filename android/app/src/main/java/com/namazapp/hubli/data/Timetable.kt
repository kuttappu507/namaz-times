package com.namazapp.hubli.data

import com.namazapp.hubli.domain.PrayerDay
import java.time.LocalDate
import java.time.YearMonth
import java.util.concurrent.ConcurrentHashMap

/**
 * The in-memory timetable for the city, indexed by month/day so it can be applied to any year.
 *
 * The printed source only lists odd dates. A date that is not printed simply **uses the most
 * recent printed date's times** (the 2nd shows the 1st's times, the 30th shows the 29th's) –
 * exactly the way the paper sheet is read. No interpolation or rounding is performed, so every
 * time displayed or scheduled is a value that appears verbatim in the CSV.
 *
 * Internally every month is modelled with 31 "slots" (matching the printed sheet, which lists
 * dates 1–31 for every month). Real calendar dates always map onto a valid slot; the phantom
 * rows (e.g. "February 31") are never displayed because such dates never exist.
 */
class Timetable internal constructor(private val printed: Map<Int, TimetableParser.RawEntry>) {

    private val cache = ConcurrentHashMap<Int, PrayerDay>(372)

    /** Prayer times for [date]. Never throws for a valid calendar date. */
    fun forDate(date: LocalDate): PrayerDay {
        val slot = slotOf(date.monthValue, date.dayOfMonth)
        val template = cache.computeIfAbsent(slot) { build(it, date) }
        // The cached instance carries whatever year was first requested – re-stamp it.
        return if (template.date == date) template else template.copy(date = date)
    }

    /** Every real calendar day of [yearMonth], in order. */
    fun forMonth(yearMonth: YearMonth): List<PrayerDay> =
        (1..yearMonth.lengthOfMonth()).map { forDate(yearMonth.atDay(it)) }

    /** `true` if this exact date is printed in the sheet (as opposed to carried forward). */
    fun isPrinted(date: LocalDate): Boolean = slotOf(date.monthValue, date.dayOfMonth) in printed

    private fun build(slot: Int, date: LocalDate): PrayerDay {
        // Walk backwards (wrapping from January 1st to December 31st) to the nearest printed row.
        var s = slot
        var steps = 0
        while (s !in printed && steps < SLOTS) {
            s = (s - 1 + SLOTS) % SLOTS
            steps++
        }
        val source = printed[s] ?: error("Timetable has no printed rows")
        return PrayerDay(
            date = date,
            fajr = source.fajr,
            sunrise = source.sunrise,
            zuhr = source.zuhr,
            asrShafi = source.asrShafi,
            asrHanafi = source.asrHanafi,
            maghrib = source.maghrib,
            isha = source.isha,
        )
    }

    companion object {
        private const val SLOTS = 12 * 31

        private fun slotOf(month: Int, day: Int) = (month - 1) * 31 + (day - 1)

        fun fromCsv(csv: String): Timetable {
            val entries = TimetableParser.parse(csv)
            require(entries.isNotEmpty()) { "Timetable CSV contained no rows" }
            val map = HashMap<Int, TimetableParser.RawEntry>(entries.size * 2)
            for (e in entries) map.putIfAbsent(slotOf(e.month.value, e.day), e)
            return Timetable(map)
        }
    }
}
