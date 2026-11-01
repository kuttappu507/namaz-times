package com.namazapp.hubli.domain

import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/**
 * Islamic (Hijri) date derived from the JDK's Umm al-Qura calendar, with a user-adjustable
 * offset of a few days to match local moon sighting.
 */
data class HijriDate(
    val day: Int,
    val month: Int,      // 1..12
    val year: Int,
) {
    val monthName: String get() = MONTHS[month - 1]
    val monthShortName: String get() = MONTHS_SHORT[month - 1]
    val monthNameArabic: String get() = MONTHS_AR[month - 1]

    fun formatted(): String = "$day $monthName $year AH"

    companion object {
        val MONTHS = listOf(
            "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
            "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
            "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah",
        )
        val MONTHS_SHORT = listOf(
            "Muh", "Saf", "Rab I", "Rab II", "Jum I", "Jum II",
            "Raj", "Sha", "Ram", "Shw", "DhQ", "DhH",
        )
        val MONTHS_AR = listOf(
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
            "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
            "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
        )

        fun from(date: LocalDate, adjustmentDays: Int = 0): HijriDate {
            val adjusted = date.plusDays(adjustmentDays.toLong())
            val h: HijrahDate = HijrahChronology.INSTANCE.date(adjusted)
            return HijriDate(
                day = h.get(ChronoField.DAY_OF_MONTH),
                month = h.get(ChronoField.MONTH_OF_YEAR),
                year = h.get(ChronoField.YEAR),
            )
        }
    }
}
