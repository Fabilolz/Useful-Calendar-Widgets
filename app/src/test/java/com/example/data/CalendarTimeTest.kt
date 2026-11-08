package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class CalendarTimeTest {
    private val rome = TimeZone.getTimeZone("Europe/Rome")
    private fun instant(value: String): Long = SimpleDateFormat("yyyy-MM-dd'T'HH:mmX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.parse(value)!!.time

    @Test fun timedInstantsRespectZoneAndDaylightSaving() {
        val formatter = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = rome }
        assertEquals("20:30", formatter.format(instant("2026-11-06T19:30Z")))
        assertEquals("21:30", formatter.format(instant("2026-07-06T19:30Z")))
    }

    @Test fun dayBoundsFollowClockChanges() {
        val day = Calendar.getInstance(rome).apply { clear(); set(2026, Calendar.MARCH, 29, 12, 0) }
        val (start, end) = CalendarTime.dayBounds(day)
        assertEquals(23 * 3600000L, end - start)
        assertFalse(CalendarTime.overlaps(end, end + 1000, start, end))
        assertTrue(CalendarTime.overlaps(end - 1000, end + 1000, start, end))
    }

    @Test fun allDayValuesRetainTheirUtcDateAcrossDst() {
        val start = CalendarTime.allDayLocal(instant("2026-03-29T00:00Z"), rome)
        val end = CalendarTime.allDayLocal(instant("2026-03-30T00:00Z"), rome)
        assertEquals(23 * 3600000L, end - start)
        assertEquals(end, CalendarTime.nextLocalDay(start, rome))
    }
}
