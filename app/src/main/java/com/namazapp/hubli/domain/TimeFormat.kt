package com.namazapp.hubli.domain

import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormat {
    private val h12 = DateTimeFormatter.ofPattern("h:mm", Locale.ENGLISH)
    private val h12Ampm = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
    private val h24 = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
    private val ampm = DateTimeFormatter.ofPattern("a", Locale.ENGLISH)

    /** "5:38 AM" or "05:38". */
    fun time(t: LocalTime, use24Hour: Boolean): String =
        if (use24Hour) h24.format(t) else h12Ampm.format(t)

    /** The numeric part only – "5:38" / "05:38" – for layouts that render AM/PM separately. */
    fun timeNumber(t: LocalTime, use24Hour: Boolean): String =
        if (use24Hour) h24.format(t) else h12.format(t)

    /** "AM" / "PM" or empty in 24-hour mode. */
    fun amPm(t: LocalTime, use24Hour: Boolean): String =
        if (use24Hour) "" else ampm.format(t).uppercase(Locale.ENGLISH)

    /** "01:23:45" style countdown. */
    fun countdown(d: Duration): String {
        val total = d.seconds.coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return String.format(Locale.ENGLISH, "%02d:%02d:%02d", h, m, s)
    }

    /** Human phrase such as "in 2h 15m" or "in 40 sec". */
    fun relative(d: Duration): String {
        val total = d.seconds.coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        return when {
            h > 0 && m > 0 -> "in ${h}h ${m}m"
            h > 0 -> "in ${h}h"
            m > 0 -> "in ${m} min"
            else -> "in ${total} sec"
        }
    }
}
