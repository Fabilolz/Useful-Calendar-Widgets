package com.example.data

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Instances
import androidx.core.content.ContextCompat
import com.example.model.CalendarAccount
import com.example.model.CalendarEvent
import java.util.Calendar

class CalendarRepository {

    fun hasCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasWriteCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getCalendars(context: Context): List<CalendarAccount> {
        if (!hasCalendarPermission(context)) {
            return emptyList()
        }

        val calendars = mutableListOf<CalendarAccount>()
        val projection = arrayOf(
            Calendars._ID,
            Calendars.CALENDAR_DISPLAY_NAME,
            Calendars.NAME,
            Calendars.ACCOUNT_NAME,
            Calendars.ACCOUNT_TYPE,
            Calendars.CALENDAR_COLOR,
            Calendars.IS_PRIMARY,
            Calendars.OWNER_ACCOUNT
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                Calendars.CONTENT_URI,
                projection,
                null,
                null,
                "${Calendars.ACCOUNT_NAME} ASC, ${Calendars.CALENDAR_DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(Calendars._ID)
                val nameCol = it.getColumnIndexOrThrow(Calendars.CALENDAR_DISPLAY_NAME)
                val altNameCol = it.getColumnIndex(Calendars.NAME)
                val accountCol = it.getColumnIndexOrThrow(Calendars.ACCOUNT_NAME)
                val typeCol = it.getColumnIndexOrThrow(Calendars.ACCOUNT_TYPE)
                val colorCol = it.getColumnIndexOrThrow(Calendars.CALENDAR_COLOR)
                val primaryCol = it.getColumnIndex(Calendars.IS_PRIMARY)
                val ownerCol = it.getColumnIndex(Calendars.OWNER_ACCOUNT)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    var name = it.getString(nameCol)?.trim() ?: ""
                    val altName = if (altNameCol != -1) it.getString(altNameCol)?.trim() else null
                    val accountName = it.getString(accountCol)?.trim() ?: "Account"
                    val owner = if (ownerCol != -1) it.getString(ownerCol)?.trim() else null
                    if (owner != null) PublicCalendarTimes.refresh(context, id, owner)
                    val type = it.getString(typeCol) ?: "Local"
                    val color = it.getInt(colorCol)
                    val isPrimary = if (primaryCol != -1) it.getInt(primaryCol) == 1 else false

                    // If provider defaults to generic "Google Calendar", use specific name, owner, or account
                    if (name.equals("Google Calendar", ignoreCase = true) || name.isBlank()) {
                        name = when {
                            !altName.isNullOrBlank() && !altName.equals("Google Calendar", ignoreCase = true) -> altName
                            !owner.isNullOrBlank() && !owner.equals("Google Calendar", ignoreCase = true) -> owner
                            accountName.isNotBlank() && !accountName.equals("Google Calendar", ignoreCase = true) -> accountName
                            else -> "Calendar"
                        }
                    }

                    calendars.add(
                        CalendarAccount(
                            id = id,
                            displayName = name,
                            accountName = accountName,
                            accountType = type,
                            color = color,
                            isPrimary = isPrimary,
                            isSelected = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return calendars
    }

    fun getUpcomingOrActiveEvents(
        events: List<CalendarEvent>,
        now: Long = System.currentTimeMillis()
    ): List<CalendarEvent> {
        return events
            .filter { it.effectiveEndMillis >= now }
            .sortedBy { it.startMillis }
    }

    fun utcMidnightToLocal(utcMillis: Long): Long {
        return CalendarTime.allDayLocal(utcMillis)
    }

    fun getEventsForDay(
        context: Context,
        selectedCalendarIds: Set<Long>?,
        targetCalendar: Calendar,
        hidePastEvents: Boolean = false
    ): List<CalendarEvent> {
        if (!hasCalendarPermission(context)) {
            return emptyList()
        }
        if (selectedCalendarIds != null && selectedCalendarIds.isEmpty()) {
            return emptyList()
        }

        val now = System.currentTimeMillis()
        val isToday = Calendar.getInstance().let {
            it.get(Calendar.DAY_OF_YEAR) == targetCalendar.get(Calendar.DAY_OF_YEAR) &&
                    it.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR)
        }

        val startOfDay = targetCalendar.clone() as Calendar
        startOfDay.set(Calendar.HOUR_OF_DAY, 0)
        startOfDay.set(Calendar.MINUTE, 0)
        startOfDay.set(Calendar.SECOND, 0)
        startOfDay.set(Calendar.MILLISECOND, 0)

        val endOfDayMillis = CalendarTime.dayBounds(targetCalendar).second

        val events = mutableListOf<CalendarEvent>()
        val calendarMap = getCalendars(context).associateBy { it.id }

        // Query window
        val builder: Uri.Builder = Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startOfDay.timeInMillis - 24 * 3600 * 1000L)
        ContentUris.appendId(builder, endOfDayMillis + 24 * 3600 * 1000L)

        val projection = arrayOf(
            Instances.EVENT_ID,
            Instances.CALENDAR_ID,
            Instances.TITLE,
            Instances.DESCRIPTION,
            Instances.EVENT_LOCATION,
            Instances.BEGIN,
            Instances.END,
            Instances.ALL_DAY,
            Events._SYNC_ID,
            Events.UID_2445,
            Instances.DISPLAY_COLOR
        )

        try {
            val cursor: Cursor? = context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                "${Instances.BEGIN} ASC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(Instances.EVENT_ID)
                val calIdCol = it.getColumnIndexOrThrow(Instances.CALENDAR_ID)
                val titleCol = it.getColumnIndexOrThrow(Instances.TITLE)
                val descCol = it.getColumnIndexOrThrow(Instances.DESCRIPTION)
                val locCol = it.getColumnIndexOrThrow(Instances.EVENT_LOCATION)
                val beginCol = it.getColumnIndexOrThrow(Instances.BEGIN)
                val endCol = it.getColumnIndexOrThrow(Instances.END)
                val allDayCol = it.getColumnIndexOrThrow(Instances.ALL_DAY)
                val colorCol = it.getColumnIndex(Instances.DISPLAY_COLOR)

                while (it.moveToNext()) {
                    val calId = it.getLong(calIdCol)
                    if (selectedCalendarIds != null && !selectedCalendarIds.contains(calId)) {
                        continue
                    }

                    var begin = it.getLong(beginCol)
                    var end = it.getLong(endCol)
                    val isAllDay = it.getInt(allDayCol) == 1
                    if (!isAllDay) {
                        PublicCalendarTimes.resolve(calId, it.getString(it.getColumnIndexOrThrow(Events._SYNC_ID)), it.getString(it.getColumnIndexOrThrow(Events.UID_2445)))?.let { source ->
                            begin = source.start
                            end = source.end
                        }
                    }

                    if (isAllDay) {
                        begin = utcMidnightToLocal(begin)
                        end = utcMidnightToLocal(end)
                        if (end <= begin) {
                            end = CalendarTime.nextLocalDay(begin)
                        }
                    } else if (end <= begin) {
                        end = begin + 1
                    }

                    // Semi-open interval: an event belongs to the day if begin < endOfDay && end > startOfDay
                    val overlapsToday = CalendarTime.overlaps(begin, end, startOfDay.timeInMillis, endOfDayMillis)
                    if (!overlapsToday) {
                        continue
                    }

                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "(No title)"
                    val description = it.getString(descCol)
                    val location = it.getString(locCol)
                    val color = if (colorCol != -1) it.getInt(colorCol) else null

                    val calAccount = calendarMap[calId]
                    val realCalendarName = calAccount?.displayName ?: "Google Calendar"
                    val realCalendarColor = calAccount?.color ?: (color ?: 0xFFE040FB.toInt())

                    val event = CalendarEvent(
                        id = id,
                        calendarId = calId,
                        calendarName = realCalendarName,
                        title = title,
                        description = description,
                        location = location,
                        startMillis = begin,
                        endMillis = end,
                        isAllDay = isAllDay,
                        color = color,
                        calendarColor = realCalendarColor
                    )

                    // Past event check: using event.isPast(now)
                    if (hidePastEvents && isToday && event.isPast(now)) {
                        continue
                    }

                    events.add(event)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // All-day events first, then by start time
        events.sortWith(compareBy<CalendarEvent> { !it.isAllDay }.thenBy { it.startMillis })
        return events
    }

    fun getEventDaysForMonth(
        context: Context,
        selectedCalendarIds: Set<Long>?,
        year: Int,
        month: Int // 0-based: Calendar.JANUARY..Calendar.DECEMBER
    ): Set<Int> {
        if (!hasCalendarPermission(context)) {
            return emptySet()
        }
        if (selectedCalendarIds != null && selectedCalendarIds.isEmpty()) {
            return emptySet()
        }

        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMonthMillis = cal.timeInMillis
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, maxDays)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMonthMillis = cal.timeInMillis

        val eventDays = mutableSetOf<Int>()
        getCalendars(context) // Load cached public source snapshots and refresh them asynchronously.
        val builder = Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMonthMillis - 24 * 3600 * 1000L)
        ContentUris.appendId(builder, endMonthMillis + 24 * 3600 * 1000L)

        val projection = arrayOf(
            Instances.CALENDAR_ID,
            Instances.BEGIN,
            Instances.END,
            Instances.ALL_DAY,
            Events._SYNC_ID,
            Events.UID_2445
        )

        try {
            val cursor = context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                null
            )
            cursor?.use {
                val calIdCol = it.getColumnIndexOrThrow(Instances.CALENDAR_ID)
                val beginCol = it.getColumnIndexOrThrow(Instances.BEGIN)
                val endCol = it.getColumnIndexOrThrow(Instances.END)
                val allDayCol = it.getColumnIndexOrThrow(Instances.ALL_DAY)

                val dayCal = Calendar.getInstance()
                while (it.moveToNext()) {
                    val calId = it.getLong(calIdCol)
                    if (selectedCalendarIds != null && !selectedCalendarIds.contains(calId)) {
                        continue
                    }

                    var begin = it.getLong(beginCol)
                    var end = it.getLong(endCol)
                    val isAllDay = it.getInt(allDayCol) == 1

                    if (!isAllDay) {
                        PublicCalendarTimes.resolve(calId, it.getString(it.getColumnIndexOrThrow(Events._SYNC_ID)), it.getString(it.getColumnIndexOrThrow(Events.UID_2445)))?.let { source ->
                            begin = source.start
                            end = source.end
                        }
                    }

                    if (isAllDay) {
                        begin = utcMidnightToLocal(begin)
                        end = utcMidnightToLocal(end)
                        if (end <= begin) end = CalendarTime.nextLocalDay(begin)
                    } else if (end <= begin) {
                        end = begin + 1
                    }

                    // Mark each day in this month that intersects [begin, end)
                    dayCal.timeInMillis = begin
                    while (dayCal.timeInMillis < end) {
                        if (dayCal.get(Calendar.YEAR) == year && dayCal.get(Calendar.MONTH) == month) {
                            eventDays.add(dayCal.get(Calendar.DAY_OF_MONTH))
                        }
                        dayCal.add(Calendar.DAY_OF_MONTH, 1)
                        dayCal.set(Calendar.HOUR_OF_DAY, 0)
                        dayCal.set(Calendar.MINUTE, 0)
                        dayCal.set(Calendar.SECOND, 0)
                        dayCal.set(Calendar.MILLISECOND, 0)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return eventDays
    }

    fun insertEvent(
        context: Context,
        calendarId: Long,
        title: String,
        location: String?,
        description: String?,
        startMillis: Long,
        endMillis: Long,
        isAllDay: Boolean
    ): Boolean {
        if (hasWriteCalendarPermission(context)) {
            try {
                val values = ContentValues().apply {
                    put(Events.DTSTART, startMillis)
                    put(Events.DTEND, endMillis)
                    put(Events.TITLE, title)
                    put(Events.DESCRIPTION, description ?: "")
                    put(Events.CALENDAR_ID, calendarId)
                    put(Events.EVENT_TIMEZONE, Calendar.getInstance().timeZone.id)
                    put(Events.EVENT_LOCATION, location ?: "")
                    put(Events.ALL_DAY, if (isAllDay) 1 else 0)
                }
                val uri = context.contentResolver.insert(Events.CONTENT_URI, values)
                return uri != null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = Events.CONTENT_URI
            putExtra(Events.TITLE, title)
            putExtra(Events.EVENT_LOCATION, location)
            putExtra(Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(Events.ALL_DAY, isAllDay)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun openCalendarAtDate(context: Context, timeMillis: Long) {
        val builder = CalendarContract.CONTENT_URI.buildUpon()
        builder.appendPath("time")
        ContentUris.appendId(builder, timeMillis)
        val intent = Intent(Intent.ACTION_VIEW).setData(builder.build()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openCalendarEvent(context: Context, eventId: Long, startMillis: Long) {
        val uri = ContentUris.withAppendedId(Events.CONTENT_URI, eventId)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = uri
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            openCalendarAtDate(context, startMillis)
        }
    }
}
