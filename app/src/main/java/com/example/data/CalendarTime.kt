package com.example.data

import java.util.Calendar
import java.util.TimeZone

/** Calendar Provider timed values are absolute instants; all-day values are UTC dates. */
internal object CalendarTime {
    fun dayBounds(day: Calendar): Pair<Long, Long> {
        val start = (day.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        return start.timeInMillis to end.timeInMillis
    }

    fun allDayLocal(utcMillis: Long, zone: TimeZone = TimeZone.getDefault()): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
        return Calendar.getInstance(zone).apply {
            clear()
            set(utc[Calendar.YEAR], utc[Calendar.MONTH], utc[Calendar.DAY_OF_MONTH], 0, 0, 0)
        }.timeInMillis
    }

    fun nextLocalDay(start: Long, zone: TimeZone = TimeZone.getDefault()): Long =
        Calendar.getInstance(zone).apply { timeInMillis = start; add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis

    fun overlaps(begin: Long, end: Long, startOfDay: Long, endOfDay: Long): Boolean =
        begin < endOfDay && end > startOfDay
}
