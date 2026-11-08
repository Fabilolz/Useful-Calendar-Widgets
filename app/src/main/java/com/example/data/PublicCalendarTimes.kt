package com.example.data

import android.content.Context
import com.example.widget.WidgetHelper
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

internal data class SourceTime(val start: Long, val end: Long)

/** Refreshes public Google subscriptions without changing the device calendar or its events. */
internal object PublicCalendarTimes {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pending = ConcurrentHashMap.newKeySet<String>()
    private val attempts = ConcurrentHashMap<String, Long>()
    private val values = ConcurrentHashMap<Long, Map<String, SourceTime>>()

    fun refresh(context: Context, calendarId: Long, owner: String) {
        if (!owner.endsWith("@group.calendar.google.com")) return
        val app = context.applicationContext
        val prefs = app.getSharedPreferences("public_calendar_times", Context.MODE_PRIVATE)
        if (!values.containsKey(calendarId)) {
            values[calendarId] = runCatching {
                val cached = JSONObject(prefs.getString(owner, "{}")!!)
                cached.keys().asSequence().associateWith { uid ->
                    val item = cached.getJSONArray(uid)
                    SourceTime(item.getLong(0), item.getLong(1))
                }
            }.getOrDefault(emptyMap())
        }
        val now = System.currentTimeMillis()
        if (now - (attempts[owner] ?: 0) < 15 * 60_000 || !pending.add(owner)) return
        attempts[owner] = now
        scope.launch {
            try {
                val encoded = URLEncoder.encode(owner, "UTF-8")
                val conn = URL("https://calendar.google.com/calendar/ical/$encoded/public/basic.ics").openConnection() as HttpURLConnection
                try {
                    conn.connectTimeout = 10000
                    conn.readTimeout = 15000
                    if (conn.responseCode != 200) return@launch // Private calendars stay with the permission-controlled provider.
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    if (!body.startsWith("BEGIN:VCALENDAR")) return@launch
                    val parsed = parse(body)
                    val json = JSONObject()
                    parsed.forEach { (uid, time) -> json.put(uid, org.json.JSONArray(listOf(time.start, time.end))) }
                    prefs.edit().putString(owner, json.toString()).apply()
                    values[calendarId] = parsed
                    withContext(Dispatchers.Main) {
                        app.sendBroadcast(android.content.Intent(ACTION_UPDATED).setPackage(app.packageName))
                        WidgetHelper.updateAllWidgets(app)
                    }
                } finally { conn.disconnect() }
            } catch (_: Exception) {
                // Retain the last successful source snapshot while offline.
            } finally { pending.remove(owner) }
        }
    }

    const val ACTION_UPDATED = "com.example.PUBLIC_CALENDAR_UPDATED"

    fun resolve(calendarId: Long, syncId: String?, uid: String?): SourceTime? {
        val key = uid?.takeIf { it.isNotBlank() } ?: decodeGoogleUid(syncId) ?: return null
        return values[calendarId]?.get(key)
    }

    internal fun decodeGoogleUid(id: String?): String? {
        if (id == null || !id.startsWith("_")) return null
        val bytes = java.io.ByteArrayOutputStream()
        var bits = 0
        var count = 0
        for (c in id.drop(1).lowercase(Locale.ROOT)) {
            val v = "0123456789abcdefghijklmnopqrstuv".indexOf(c)
            if (v < 0) return null
            bits = (bits shl 5) or v
            count += 5
            if (count >= 8) { count -= 8; bytes.write((bits shr count) and 255) }
        }
        return bytes.toString("UTF-8")
    }

    internal fun parse(body: String): Map<String, SourceTime> {
        val unfolded = body.replace(Regex("\\r?\\n[ \\t]"), "")
        val result = mutableMapOf<String, SourceTime>()
        for (block in unfolded.split("BEGIN:VEVENT").drop(1)) {
            val lines = block.substringBefore("END:VEVENT").lineSequence().map { it.trimEnd('\r') }.toList()
            // Recurrences and exceptions must be expanded by Calendar Provider, never guessed here.
            if (lines.any { it.startsWith("RRULE:") || it.startsWith("RECURRENCE-ID") || it.startsWith("RDATE") || it == "STATUS:CANCELLED" }) continue
            val uid = lines.firstOrNull { it.startsWith("UID:") }?.substringAfter(':') ?: continue
            val startLine = lines.firstOrNull { it.startsWith("DTSTART:") || it.startsWith("DTSTART;") } ?: continue
            val endLine = lines.firstOrNull { it.startsWith("DTEND:") || it.startsWith("DTEND;") } ?: continue
            val start = parseDate(startLine) ?: continue
            val end = parseDate(endLine) ?: continue
            if (end > start) result[uid] = SourceTime(start, end)
        }
        return result
    }

    private fun parseDate(line: String): Long? = runCatching {
        val value = line.substringAfter(':')
        if (value.length < 15) return null // All-day dates retain provider date semantics.
        val zone = if (value.endsWith('Z')) TimeZone.getTimeZone("UTC") else {
            val id = Regex("TZID=([^;:]+)").find(line.substringBefore(':'))?.groupValues?.get(1)?.trim('"') ?: return null
            if (id !in TimeZone.getAvailableIDs()) return null
            TimeZone.getTimeZone(id)
        }
        SimpleDateFormat(if (value.endsWith('Z')) "yyyyMMdd'T'HHmmss'Z'" else "yyyyMMdd'T'HHmmss", Locale.ROOT).apply {
            timeZone = zone
            isLenient = false
        }.parse(value)?.time
    }.getOrNull()
}
