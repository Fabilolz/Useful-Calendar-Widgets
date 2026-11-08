package com.example.data

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class PublicCalendarTimesTest {
    @Test fun importedGoogleIdMatchesOriginalIcalendarUid() {
        assertEquals("CSVConverte5cecbb6e94d8938047a7b0c650e8cd5", PublicCalendarTimes.decodeGoogleUid("_8d9lcgrfdpr6asjkckqm6pb3c9h3cp9p6hi3ge9j70o38dr16th30opm6ko6ae33cgqg"))
        assertNull(PublicCalendarTimes.decodeGoogleUid("ordinary-recurring-id"))
    }

    @Test fun sourceUtcTimeMatchesGoogleCalendarScreenshot() {
        val body = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nUID:test\r\nDTSTART:20261106T153000Z\r\nDTEND:20261106T163000Z\r\nEND:VEVENT\r\nEND:VCALENDAR"
        val actual = PublicCalendarTimes.parse(body).getValue("test")
        val formatter = SimpleDateFormat("HH:mm", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("Europe/Rome") }
        assertEquals("16:30", formatter.format(actual.start))
        assertEquals("17:30", formatter.format(actual.end))
    }

    @Test fun explicitTimezoneAndFoldedUidAreParsed() {
        val result = PublicCalendarTimes.parse("BEGIN:VEVENT\nUID:long-\n uid\nDTSTART;TZID=Europe/London:20260706T153000\nDTEND;TZID=Europe/London:20260706T163000\nEND:VEVENT")
        val formatter = SimpleDateFormat("HH:mm", Locale.ROOT).apply { timeZone = TimeZone.getTimeZone("Europe/Rome") }
        assertEquals("16:30", formatter.format(result.getValue("long-uid").start))
    }

    @Test fun recurringAllDayCancelledAndFloatingEventsStayWithProvider() {
        for (extra in listOf("RRULE:FREQ=DAILY", "RECURRENCE-ID:20261106T153000Z", "STATUS:CANCELLED")) {
            assertTrue(PublicCalendarTimes.parse("BEGIN:VEVENT\nUID:test\nDTSTART:20261106T153000Z\nDTEND:20261106T163000Z\n$extra\nEND:VEVENT").isEmpty())
        }
        assertTrue(PublicCalendarTimes.parse("BEGIN:VEVENT\nUID:test\nDTSTART;VALUE=DATE:20261106\nDTEND;VALUE=DATE:20261107\nEND:VEVENT").isEmpty())
        assertTrue(PublicCalendarTimes.parse("BEGIN:VEVENT\nUID:test\nDTSTART:20261106T153000\nDTEND:20261106T163000\nEND:VEVENT").isEmpty())
    }
}
