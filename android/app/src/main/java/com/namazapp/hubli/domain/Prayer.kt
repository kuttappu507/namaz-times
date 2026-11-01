package com.namazapp.hubli.domain

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The six daily events shown by the app. [SUNRISE] is not a prayer, but it marks the end of
 * Fajr and is printed in every timetable, so it is modelled alongside the five salah.
 */
enum class Prayer(
    val displayName: String,
    val arabicName: String,
    val isSalah: Boolean,
) {
    FAJR("Fajr", "الفجر", isSalah = true),
    SUNRISE("Sunrise", "الشروق", isSalah = false),
    ZUHR("Zuhr", "الظهر", isSalah = true),
    ASR("Asr", "العصر", isSalah = true),
    MAGHRIB("Maghrib", "المغرب", isSalah = true),
    ISHA("Isha", "العشاء", isSalah = true);

    /** Zuhr on a Friday is Jumu'ah. */
    fun isJumuah(date: LocalDate): Boolean = this == ZUHR && date.dayOfWeek == DayOfWeek.FRIDAY

    companion object {
        val salah: List<Prayer> = entries.filter { it.isSalah }
    }
}

/** School of thought used for the Asr time. Both columns are present in the timetable. */
enum class AsrMadhab(val label: String, val description: String) {
    SHAFI("Shafi'i", "Shadow equals object length (also Maliki, Hanbali)"),
    HANAFI("Hanafi", "Shadow equals twice the object length"),
}
