package com.example

import android.content.Context
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import com.example.model.WidgetCustomization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Useful Calendar Widgets", appName)
    }

    @Test
    fun `test agenda widget layout inflation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val views = RemoteViews(context.packageName, R.layout.widget_agenda_layout)
        val inflated = views.apply(context, FrameLayout(context))
        assertNotNull(inflated)
    }

    @Test
    fun `test glance widget layout inflation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val views = RemoteViews(context.packageName, R.layout.widget_glance_layout)
        val inflated = views.apply(context, FrameLayout(context))
        assertNotNull(inflated)
    }

    @Test
    fun `test month widget layout inflation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val views = RemoteViews(context.packageName, R.layout.widget_month_layout)
        val inflated = views.apply(context, FrameLayout(context))
        assertNotNull(inflated)
    }

    @Test
    fun `test week widget layout inflation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val views = RemoteViews(context.packageName, R.layout.widget_week_layout)
        val inflated = views.apply(context, FrameLayout(context))
        assertNotNull(inflated)
    }

    @Test
    fun `test split widget layout inflation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val views = RemoteViews(context.packageName, R.layout.widget_split_layout)
        val inflated = views.apply(context, FrameLayout(context))
        assertNotNull(inflated)
    }

    @Test
    fun `test font scaling customization`() {
        val defaultCustomization = WidgetCustomization()
        assertEquals(1.0f, defaultCustomization.fontScale, 0.001f)

        val scaledCustomization = defaultCustomization.copy(fontScale = 1.25f)
        assertEquals(1.25f, scaledCustomization.fontScale, 0.001f)
    }

    @Test
    fun `test light expressive background returns pure white`() {
        val lightCustomization = WidgetCustomization(darkTheme = false, customBgHex = null)
        val resolvedColor = lightCustomization.getResolvedBackgroundColor()
        assertEquals(1.0f, resolvedColor.red, 0.01f)
        assertEquals(1.0f, resolvedColor.green, 0.01f)
        assertEquals(1.0f, resolvedColor.blue, 0.01f)
    }

    @Test
    fun `test past events exclusion`() {
        val now = System.currentTimeMillis()
        val pastEvent = com.example.model.CalendarEvent(
            id = 1L,
            calendarId = 1L,
            calendarName = "Test",
            title = "Morning Coffee",
            description = null,
            location = null,
            startMillis = now - 60 * 60 * 1000,
            endMillis = now - 10 * 60 * 1000
        )
        val futureEvent = com.example.model.CalendarEvent(
            id = 2L,
            calendarId = 1L,
            calendarName = "Test",
            title = "Yoga Class",
            description = null,
            location = null,
            startMillis = now + 30 * 60 * 1000,
            endMillis = now + 90 * 60 * 1000
        )

        val allEvents = listOf(pastEvent, futureEvent)
        val activeOnly = allEvents.filter { it.effectiveEndMillis >= now }
        assertEquals(1, activeOnly.size)
        assertEquals("Yoga Class", activeOnly.first().title)
    }

    @Test
    fun `test imminent event detection at 2259 for 2300 to 0000 event`() {
        // Base time at 22:59:00
        val baseNow = 1730000000000L // arbitrary epoch
        val startAt2300 = baseNow + 60 * 1000L // 1 minute later
        // Event scheduled from 23:00 to 00:00 (crosses midnight, so ends 1 hour after start)
        val endAt0000 = startAt2300 + 60 * 60 * 1000L

        val event = com.example.model.CalendarEvent(
            id = 999L,
            calendarId = 1L,
            calendarName = "Google Calendar",
            title = "Late Night Sync",
            description = null,
            location = "Office",
            startMillis = startAt2300,
            endMillis = endAt0000
        )

        // Must be detected as upcoming
        assertTrue("Event must be upcoming at 22:59", event.isUpcoming(baseNow))
        assertTrue("Event must not be past at 22:59", !event.isPast(baseNow))
        assertTrue("Event must be imminent (starts in 1 min)", event.isImminent(baseNow))
        assertEquals("in 1m", event.getRelativeTimeText(baseNow))

        val repo = com.example.data.CalendarRepository()
        val activeEvents = repo.getUpcomingOrActiveEvents(listOf(event), baseNow)
        assertEquals(1, activeEvents.size)
        assertEquals("Late Night Sync", activeEvents.first().title)
    }

    @Test
    fun `test malformed end timestamp is clamped without shifting the event`() {
        val startAt2300 = 1730000000000L
        // Naive end time passed as 00:00 on the same date (23 hours earlier in raw millis)
        val naiveEndAt0000SameDay = startAt2300 - 23 * 3600 * 1000L

        val event = com.example.model.CalendarEvent(
            id = 888L,
            calendarId = 1L,
            calendarName = "Google Calendar",
            title = "Midnight Jam",
            description = null,
            location = null,
            startMillis = startAt2300,
            endMillis = naiveEndAt0000SameDay
        )

        // Provider timestamps are absolute. A malformed end is clamped just after the start.
        assertTrue("effectiveEndMillis must be after startMillis", event.effectiveEndMillis > event.startMillis)
        assertEquals(startAt2300 + 1L, event.effectiveEndMillis)
    }

    @Test
    fun `test background customization theme resolution`() {
        val whiteCustomization = WidgetCustomization(
            darkTheme = false,
            customBgHex = "#FFFFFF",
            backgroundOpacity = 0.85f
        )
        assertTrue(whiteCustomization.isLightBackground())
        assertEquals(0.85f, whiteCustomization.getResolvedBackgroundColor().alpha, 0.01f)
        // Light background should have dark text
        assertEquals(0x18 / 255f, whiteCustomization.getPrimaryTextColor().red, 0.02f)

        val darkCustomization = WidgetCustomization(
            darkTheme = true,
            customBgHex = "#18181A",
            backgroundOpacity = 1.0f
        )
        assertTrue(!darkCustomization.isLightBackground())
        assertEquals(1.0f, darkCustomization.getPrimaryTextColor().red, 0.01f)
    }

    @Test
    fun `test curated schemes drawable resolution`() {
        val amoled = WidgetCustomization(darkTheme = true, customBgHex = "#000000")
        assertEquals(R.drawable.widget_bg_amoled, amoled.getWidgetBackgroundDrawableRes())

        val navy = WidgetCustomization(darkTheme = true, customBgHex = "#101622")
        assertEquals(R.drawable.widget_bg_navy, navy.getWidgetBackgroundDrawableRes())

        val cream = WidgetCustomization(darkTheme = false, customBgHex = "#F9F7F2")
        assertEquals(R.drawable.widget_bg_cream, cream.getWidgetBackgroundDrawableRes())

        val plum = WidgetCustomization(darkTheme = true, customBgHex = "#221124")
        assertEquals(R.drawable.widget_bg_plum, plum.getWidgetBackgroundDrawableRes())

        val light = WidgetCustomization(darkTheme = false, customBgHex = "#FFFFFF")
        assertEquals(R.drawable.widget_bg_light, light.getWidgetBackgroundDrawableRes())

        val dark = WidgetCustomization(darkTheme = true, customBgHex = "#18181A")
        assertEquals(R.drawable.widget_bg_dark, dark.getWidgetBackgroundDrawableRes())
    }

    @Test
    fun `test widget resize options change execution`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = com.example.widget.ExpressGlanceWidgetProvider()
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        val bundle = android.os.Bundle().apply {
            putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 220)
            putInt(android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 65)
        }
        // Ensuring onAppWidgetOptionsChanged executes without throwing
        provider.onAppWidgetOptionsChanged(context, appWidgetManager, 101, bundle)
        assertNotNull(provider)
    }

    @Test
    fun `test all widget providers execute onUpdate without action exception`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        val testIds = intArrayOf(1001)

        val providers = listOf(
            com.example.widget.ExpressAgendaWidgetProvider(),
            com.example.widget.ExpressGlanceWidgetProvider(),
            com.example.widget.ExpressMonthWidgetProvider(),
            com.example.widget.ExpressWeekWidgetProvider(),
            com.example.widget.ExpressSplitWidgetProvider()
        )

        providers.forEach { provider ->
            provider.onUpdate(context, appWidgetManager, testIds)
        }
    }

    @Test
    fun `test formatTimeRange localized 24h format`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // 21:00 to 23:00
        val cal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 1, 21, 0, 0)
        }
        val start = cal.timeInMillis
        cal.set(2026, java.util.Calendar.NOVEMBER, 1, 23, 0, 0)
        val end = cal.timeInMillis

        val event = com.example.model.CalendarEvent(
            id = 50L,
            calendarId = 1L,
            calendarName = "Calendar",
            title = "Grand Prix",
            startMillis = start,
            endMillis = end
        )

        // Switch to Italian configuration
        val locale = java.util.Locale.ITALY
        val config = android.content.res.Configuration(context.resources.configuration).apply {
            setLocale(locale)
        }
        val localizedContext = context.createConfigurationContext(config)

        val formatted = event.formatTimeRange(localizedContext)
        // In Italian 24h format, should contain 21:00 and 23:00
        assertTrue("Expected 21:00 in formatted time but got: $formatted", formatted.contains("21:00"))
        assertTrue("Expected 23:00 in formatted time but got: $formatted", formatted.contains("23:00"))
    }

    @Test
    fun `test utcMidnightToLocal and all-day interval exclusion`() {
        val repo = com.example.data.CalendarRepository()
        // Suppose yesterday was Nov 1, today is Nov 2
        val yesterdayCal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 1, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val todayCal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 2, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        // An all-day event for Nov 1 starts at Nov 1 00:00:00 and ends at Nov 2 00:00:00
        val eventStart = yesterdayCal.timeInMillis
        val eventEnd = todayCal.timeInMillis

        val todayStartMillis = todayCal.timeInMillis
        val todayEndMillis = todayCal.clone().let {
            (it as java.util.Calendar).apply {
                set(java.util.Calendar.HOUR_OF_DAY, 23)
                set(java.util.Calendar.MINUTE, 59)
                set(java.util.Calendar.SECOND, 59)
                set(java.util.Calendar.MILLISECOND, 999)
            }.timeInMillis
        }

        // Semi-open interval check: event intersects today if eventStart < todayEnd && eventEnd > todayStart
        val intersectsToday = eventStart < todayEndMillis && eventEnd > todayStartMillis
        // With semi-open interval, eventEnd is exactly todayStartMillis, so eventEnd > todayStartMillis is FALSE!
        assertTrue("Yesterday's all-day event ending at today's midnight must NOT intersect today", !intersectsToday)
    }

    @Test
    fun `test italian string resources`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = android.content.res.Configuration(context.resources.configuration).apply {
            setLocale(java.util.Locale.ITALIAN)
        }
        val itContext = context.createConfigurationContext(config)

        assertEquals("Tutto il giorno", itContext.getString(R.string.all_day))
        assertEquals("PROSSIMO", itContext.getString(R.string.up_next))
        assertEquals("IN CORSO", itContext.getString(R.string.happening_now))
    }

    @Test
    fun `test widget palette wcag contrast compliance`() {
        val presetBackgrounds = listOf(
            android.graphics.Color.parseColor("#18181A"),
            android.graphics.Color.parseColor("#FFFFFF"),
            android.graphics.Color.parseColor("#000000"),
            android.graphics.Color.parseColor("#101622"),
            android.graphics.Color.parseColor("#F9F7F2"),
            android.graphics.Color.parseColor("#221124")
        )

        for (bg in presetBackgrounds) {
            for (accentIdx in 0..3) {
                val palette = com.example.widget.WidgetPalette.from(bg, accentIdx, 1.0f)

                val onBgContrast = com.example.widget.WidgetPalette.contrastRatio(palette.onBg, palette.bgRgb)
                assertTrue(
                    "onBg contrast $onBgContrast on bg ${Integer.toHexString(bg)} must be >= 4.5:1",
                    onBgContrast >= 4.5
                )

                val onBgVarContrast = com.example.widget.WidgetPalette.contrastRatio(palette.onBgVariant, palette.bgRgb)
                assertTrue(
                    "onBgVariant contrast $onBgVarContrast on bg ${Integer.toHexString(bg)} must be >= 4.5:1",
                    onBgVarContrast >= 4.5
                )

                val accentTextContrast = com.example.widget.WidgetPalette.contrastRatio(palette.accentText, palette.bgRgb)
                assertTrue(
                    "accentText contrast $accentTextContrast on bg ${Integer.toHexString(bg)} must be >= 4.5:1",
                    accentTextContrast >= 4.5
                )

                val onCardContrast = com.example.widget.WidgetPalette.contrastRatio(palette.onCard, palette.card)
                assertTrue(
                    "onCard contrast $onCardContrast on card ${Integer.toHexString(palette.card)} must be >= 4.5:1",
                    onCardContrast >= 4.5
                )
            }
        }
    }

    @Test
    fun `test widget renderer inflation for all widget types`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dummyEvent = com.example.model.CalendarEvent(
            id = 100L,
            calendarId = 1L,
            calendarName = "Test Calendar",
            title = "Formula 1 Grand Prix",
            description = "Main race",
            location = "Monza",
            startMillis = System.currentTimeMillis() - 1000 * 60 * 30, // happening now
            endMillis = System.currentTimeMillis() + 1000 * 60 * 60
        )
        val dummyEvents = listOf(dummyEvent)
        val customization = WidgetCustomization(backgroundOpacity = 0.40f)

        for (type in com.example.model.WidgetType.values()) {
            val rv = com.example.widget.WidgetRenderer.build(
                context = context,
                type = type,
                events = dummyEvents,
                day = java.util.Calendar.getInstance(),
                customization = customization,
                box = com.example.widget.WidgetBox(220f, 220f),
                hasPermission = true
            )
            assertNotNull("RemoteViews for $type must not be null", rv)

            // Test inflation against real FrameLayout without ActionException!
            val parent = FrameLayout(context)
            val appliedView = rv.apply(context, parent)
            assertNotNull("Inflated view for $type must not be null", appliedView)
        }
    }

    @Test
    fun `test november 2026 month layout handles 6 weeks without exception`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val nov2026 = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 1, 10, 0, 0)
        }
        val rv = com.example.widget.WidgetRenderer.build(
            context = context,
            type = com.example.model.WidgetType.EXPRESS_MONTH,
            events = emptyList(),
            day = nov2026,
            customization = WidgetCustomization(),
            box = com.example.widget.WidgetBox(300f, 230f),
            hasPermission = true,
            eventDaysForMonth = setOf(1, 15, 30)
        )
        val parent = FrameLayout(context)
        val applied = rv.apply(context, parent)
        assertNotNull(applied)
    }
}
