package com.namazapp.hubli

import com.namazapp.hubli.domain.HijriDate
import com.namazapp.hubli.domain.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

class HijriAndFormatTest {

    @Test
    fun `hijri conversion matches a known date`() {
        // 1 Ramadan 1446 AH = 1 March 2025 (Umm al-Qura)
        val h = HijriDate.from(LocalDate.of(2025, 3, 1))
        assertEquals(1446, h.year)
        assertEquals(9, h.month)
        assertEquals(1, h.day)
        assertEquals("Ramadan", h.monthName)
    }

    @Test
    fun `hijri adjustment shifts by days`() {
        // 1 Ramadan 1446 with -1 day -> last day of Sha'ban 1446 (which has 29 days)
        val minus = HijriDate.from(LocalDate.of(2025, 3, 1), adjustmentDays = -1)
        assertEquals(8, minus.month)
        assertEquals("Sha'ban", minus.monthName)
        assertEquals(1446, minus.year)
        // +1 day -> 2 Ramadan
        val plus = HijriDate.from(LocalDate.of(2025, 3, 1), adjustmentDays = 1)
        assertEquals(9, plus.month)
        assertEquals(2, plus.day)
        // Adjustment is the same as asking for the neighbouring Gregorian date
        assertEquals(HijriDate.from(LocalDate.of(2025, 3, 2)), plus)
    }

    @Test
    fun `time formatting`() {
        assertEquals("5:38 AM", TimeFormat.time(LocalTime.of(5, 38), use24Hour = false))
        assertEquals("05:38", TimeFormat.time(LocalTime.of(5, 38), use24Hour = true))
        assertEquals("12:37 PM", TimeFormat.time(LocalTime.of(12, 37), use24Hour = false))
        assertEquals("7:31", TimeFormat.timeNumber(LocalTime.of(19, 31), use24Hour = false))
        assertEquals("PM", TimeFormat.amPm(LocalTime.of(19, 31), use24Hour = false))
        assertEquals("", TimeFormat.amPm(LocalTime.of(19, 31), use24Hour = true))
    }

    @Test
    fun `countdown formatting`() {
        assertEquals("01:02:03", TimeFormat.countdown(Duration.ofSeconds(3723)))
        assertEquals("00:00:00", TimeFormat.countdown(Duration.ofSeconds(-5)))
        assertEquals("in 2h 15m", TimeFormat.relative(Duration.ofMinutes(135)))
        assertEquals("in 40 sec", TimeFormat.relative(Duration.ofSeconds(40)))
    }
}
