package com.example

import android.Manifest
import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import com.example.data.CalendarRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalendarRepositoryTimeTest {
    @Test fun `instance epoch times are preserved on daylight saving transition`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(Manifest.permission.READ_CALENDAR)
        val begin = 1774744200000L
        val end = begin + 3600000
        val provider = object : ContentProvider() {
            override fun onCreate() = true
            override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
                val cols = projection ?: emptyArray()
                return MatrixCursor(cols).apply {
                    if (uri.path?.startsWith("/instances") == true) {
                        addRow(cols.map { when (it) {
                            CalendarContract.Instances.EVENT_ID -> 1L
                            CalendarContract.Instances.CALENDAR_ID -> 2L
                            CalendarContract.Instances.TITLE -> "Evento"
                            CalendarContract.Instances.BEGIN -> begin
                            CalendarContract.Instances.END -> end
                            CalendarContract.Instances.ALL_DAY -> 0
                            CalendarContract.Instances.DISPLAY_COLOR -> 0
                            else -> null
                        } })
                    }
                }
            }
            override fun getType(uri: Uri): String? = null
            override fun insert(uri: Uri, values: ContentValues?): Uri? = null
            override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
            override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
        }
        ShadowContentResolver.registerProviderInternal("com.android.calendar", provider)
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"))
            val day = Calendar.getInstance().apply { timeInMillis = begin }
            val event = CalendarRepository().getEventsForDay(app, null, day).single()
            assertEquals(begin, event.startMillis)
            assertEquals(end, event.endMillis)
        } finally { TimeZone.setDefault(previous) }
    }
}
