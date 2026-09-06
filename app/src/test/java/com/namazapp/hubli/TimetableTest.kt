package com.namazapp.hubli

import com.namazapp.hubli.data.Timetable
import com.namazapp.hubli.data.TimetableParser
import com.namazapp.hubli.domain.AsrMadhab
import com.namazapp.hubli.domain.Prayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.Year
import java.time.YearMonth

class TimetableTest {

    private val csv: String by lazy {
        // Unit tests run on the JVM, so read the asset straight from the source tree.
        File("src/main/assets/namaz_timings.csv").readText()
    }
    private val table by lazy { Timetable.fromCsv(csv) }

    @Test
    fun `parses all printed rows`() {
        val rows = TimetableParser.parse(csv)
        assertEquals(12 * 16, rows.size)
    }

    @Test
    fun `am pm inference is correct`() {
        assertEquals(LocalTime.of(5, 38), TimetableParser.parseTime("5:38", pm = false))
        assertEquals(LocalTime.of(12, 37), TimetableParser.parseTime("12:37", pm = true))
        assertEquals(LocalTime.of(15, 51), TimetableParser.parseTime("3:51", pm = true))
        assertEquals(LocalTime.of(19, 31), TimetableParser.parseTime("7:31", pm = true))
        assertEquals(LocalTime.of(0, 5), TimetableParser.parseTime("12:05", pm = false))
    }

    @Test
    fun `printed odd dates are returned verbatim`() {
        val d = table.forDate(LocalDate.of(2025, 1, 1))
        assertEquals(LocalTime.of(5, 38), d.fajr)
        assertEquals(LocalTime.of(6, 55), d.sunrise)
        assertEquals(LocalTime.of(12, 37), d.zuhr)
        assertEquals(LocalTime.of(15, 51), d.asrShafi)
        assertEquals(LocalTime.of(16, 39), d.asrHanafi)
        assertEquals(LocalTime.of(18, 15), d.maghrib)
        assertEquals(LocalTime.of(19, 31), d.isha)
    }

    @Test
    fun `unprinted dates carry forward the previous printed day`() {
        // Jan 2 is not printed -> identical to Jan 1 (no interpolation, no rounding).
        val jan1 = table.forDate(LocalDate.of(2025, 1, 1))
        val jan2 = table.forDate(LocalDate.of(2025, 1, 2))
        for (p in Prayer.entries) {
            assertEquals("$p", jan1.timeOf(p, AsrMadhab.HANAFI), jan2.timeOf(p, AsrMadhab.HANAFI))
            assertEquals("$p", jan1.timeOf(p, AsrMadhab.SHAFI), jan2.timeOf(p, AsrMadhab.SHAFI))
        }
        assertEquals(LocalTime.of(5, 38), jan2.fajr)
        assertEquals(LocalTime.of(19, 31), jan2.isha)
        // Jan 4 -> Jan 3 (Zuhr 12:38), Jan 10 -> Jan 9 (Maghrib 6:20)
        assertEquals(LocalTime.of(12, 38), table.forDate(LocalDate.of(2025, 1, 4)).zuhr)
        assertEquals(LocalTime.of(18, 20), table.forDate(LocalDate.of(2025, 1, 10)).maghrib)
        assertFalse(table.isPrinted(LocalDate.of(2025, 1, 2)))
        assertTrue(table.isPrinted(LocalDate.of(2025, 1, 3)))
    }

    @Test
    fun `every displayed time appears verbatim in the csv`() {
        val printedTimes = HashSet<LocalTime>()
        TimetableParser.parse(csv).forEach {
            printedTimes += listOf(it.fajr, it.sunrise, it.zuhr, it.asrShafi, it.asrHanafi, it.maghrib, it.isha)
        }
        for (day in 1..365) {
            val d = table.forDate(LocalDate.ofYearDay(2025, day))
            for (p in Prayer.entries) {
                assertTrue("${d.date} $p", d.timeOf(p, AsrMadhab.HANAFI) in printedTimes)
                assertTrue("${d.date} $p", d.timeOf(p, AsrMadhab.SHAFI) in printedTimes)
            }
        }
    }

    @Test
    fun `every day of the year resolves and is chronologically ordered`() {
        for (year in listOf(2024, 2025, 2026)) {
            for (day in 1..Year.of(year).length()) {
                val date = LocalDate.ofYearDay(year, day)
                val d = table.forDate(date)
                assertTrue("$date fajr<sunrise", d.fajr < d.sunrise)
                assertTrue("$date sunrise<zuhr", d.sunrise < d.zuhr)
                assertTrue("$date zuhr<asrS", d.zuhr < d.asrShafi)
                assertTrue("$date asrS<asrH", d.asrShafi < d.asrHanafi)
                assertTrue("$date asrH<maghrib", d.asrHanafi < d.maghrib)
                assertTrue("$date maghrib<isha", d.maghrib < d.isha)
            }
        }
    }

    /**
     * Guards against typos in the printed sheet. The original CSV had "November,27 Sunrise 5:36"
     * (an obvious misprint for 6:36 – neighbours are 6:35 and 6:37), which this test caught.
     */
    @Test
    fun `consecutive days never jump more than 3 minutes`() {
        var prev = table.forDate(LocalDate.of(2025, 1, 1))
        for (day in 2..365) {
            val cur = table.forDate(LocalDate.ofYearDay(2025, day))
            for (p in Prayer.entries) {
                val a = prev.timeOf(p, AsrMadhab.HANAFI).toSecondOfDay()
                val b = cur.timeOf(p, AsrMadhab.HANAFI).toSecondOfDay()
                assertTrue("${cur.date} $p jumped ${Math.abs(a - b) / 60} min", Math.abs(a - b) <= 3 * 60)
            }
            prev = cur
        }
    }

    @Test
    fun `month view has the right number of days`() {
        assertEquals(28, table.forMonth(YearMonth.of(2025, 2)).size)
        assertEquals(29, table.forMonth(YearMonth.of(2024, 2)).size)
        assertEquals(31, table.forMonth(YearMonth.of(2025, 12)).size)
        assertEquals(30, table.forMonth(YearMonth.of(2025, 4)).size)
    }

    @Test
    fun `feb 30 and 31 phantom rows do not leak`() {
        val feb = table.forMonth(YearMonth.of(2025, 2))
        assertEquals(LocalDate.of(2025, 2, 28), feb.last().date)
        // Feb 28 is not printed -> carries forward the printed Feb 27 row (Fajr 5:36).
        assertEquals(LocalTime.of(5, 36), feb.last().fajr)
        assertEquals(LocalTime.of(18, 41), feb.last().maghrib)
    }
}
