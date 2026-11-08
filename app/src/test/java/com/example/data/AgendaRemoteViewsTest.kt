package com.example.data

import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.model.CalendarEvent
import com.example.model.WidgetCustomization
import com.example.model.WidgetType
import com.example.widget.WidgetBox
import com.example.widget.WidgetRenderer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class AgendaRemoteViewsTest {
    @Test fun splitDayAddsEventsToBothColumnsWhenExpanded() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val day = java.util.Calendar.getInstance()
        val events = listOf(8, 10, 14, 16).map { hour ->
            val start = (day.clone() as java.util.Calendar).apply { set(java.util.Calendar.HOUR_OF_DAY, hour) }.timeInMillis
            CalendarEvent(hour.toLong(), 4, "Test", "Evento $hour", startMillis = start, endMillis = start + 1800000)
        }
        fun render(height: Float) = WidgetRenderer.build(context, WidgetType.EXPRESS_SPLIT_DAY, events, day,
            WidgetCustomization(hidePastEvents = false), WidgetBox(300f, height))
            .apply(context, android.widget.FrameLayout(context))
        for (id in listOf(R.id.widget_split_morning_extra, R.id.widget_split_afternoon_extra)) {
            assertEquals(0, render(180f).findViewById<android.widget.LinearLayout>(id).childCount)
            assertEquals(1, render(400f).findViewById<android.widget.LinearLayout>(id).childCount)
        }
    }
    @Test fun weekIncludesBothPastEventsOnlyWhenFilterIsOff() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val now = System.currentTimeMillis()
        val first = CalendarEvent(1, 4, "F1", "Prima prova", startMillis = now - 7200000, endMillis = now - 3600000)
        val second = CalendarEvent(2, 4, "F1", "Seconda prova", startMillis = now - 3600000, endMillis = now - 1800000)
        fun render(hidePast: Boolean) = WidgetRenderer.build(context, WidgetType.EXPRESS_WEEK_STRIP, listOf(first, second),
            customization = WidgetCustomization(hidePastEvents = hidePast), box = WidgetBox(300f, 195f))
            .apply(context, android.widget.FrameLayout(context))
        val visible = render(false)
        assertEquals("Prima prova", visible.findViewById<android.widget.TextView>(R.id.widget_week_event_title).text.toString())
        assertEquals("Seconda prova", visible.findViewById<android.widget.TextView>(R.id.widget_week_event_title_2).text.toString())
        assertEquals(first.formatTimeRange(context), visible.findViewById<android.widget.TextView>(R.id.widget_week_hours).text.toString())
        assertEquals(android.view.View.VISIBLE, visible.findViewById<android.view.View>(R.id.widget_week_event_card_2).visibility)
        val hidden = render(true)
        assertEquals(android.view.View.GONE, hidden.findViewById<android.view.View>(R.id.widget_week_event_card).visibility)
        assertEquals(android.view.View.GONE, hidden.findViewById<android.view.View>(R.id.widget_week_event_card_2).visibility)
        val fourEvents = listOf(first, second, first.copy(id = 3), second.copy(id = 4))
        val large = WidgetRenderer.build(context, WidgetType.EXPRESS_WEEK_STRIP, fourEvents,
            customization = WidgetCustomization(hidePastEvents = false), box = WidgetBox(300f, 235f))
            .apply(context, android.widget.FrameLayout(context))
        assertEquals(android.view.View.VISIBLE, large.findViewById<android.view.View>(R.id.widget_week_event_card_3).visibility)
        val fourthVisible = large.findViewById<android.view.View>(R.id.widget_week_event_card_4).visibility == android.view.View.VISIBLE
        val expectedCount = if (fourthVisible) context.getString(R.string.events_count, 4)
            else context.getString(R.string.widget_week_visible_count, 3, 4)
        assertEquals(expectedCount, large.findViewById<android.widget.TextView>(R.id.widget_week_count).text.toString())
        val month = WidgetRenderer.build(context, WidgetType.EXPRESS_MONTH, fourEvents,
            customization = WidgetCustomization(hidePastEvents = false), box = WidgetBox(322f, 306f))
            .apply(context, android.widget.FrameLayout(context))
        assertEquals(android.view.View.VISIBLE, month.findViewById<android.view.View>(R.id.widget_month_event_card_4).visibility)
    }
    @Test fun oneEventShowsStackedMonthAndWeatherWhileTwoUseInlineMonth() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        org.robolectric.Shadows.shadowOf(context as android.app.Application).grantPermissions(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        val dayFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ROOT).apply {
            timeZone = java.util.TimeZone.getTimeZone("GMT")
        }
        val today = dayFormat.format(java.util.Date())
        context.getSharedPreferences("weather_location_v2", android.content.Context.MODE_PRIVATE).edit()
            .putInt("temp", 10).putString("condition", "Nuvoloso").putString("icon", "☁️")
            .putString("outlook", """
                {"timezone":"GMT","daily":{"time":["$today"],
                "temperature_2m_max":[12],"temperature_2m_min":[7]}}
            """.trimIndent())
            .putLong("timestamp", System.currentTimeMillis()).commit()
        val event = CalendarEvent(1, 4, "F1", "FP1: Brazilian Grand Prix", location = "Interlagos",
            startMillis = 1793979000000, endMillis = 1793982600000)
        fun render(events: List<CalendarEvent>) = WidgetRenderer.build(context, WidgetType.EXPRESS_AGENDA, events,
            customization = WidgetCustomization(hidePastEvents = false), box = WidgetBox(205f, 195f))
            .apply(context, android.widget.FrameLayout(context))
        val one = render(listOf(event))
        assertEquals(android.view.View.VISIBLE, one.findViewById<android.view.View>(R.id.widget_month_below).visibility)
        assertEquals(android.view.View.GONE, one.findViewById<android.view.View>(R.id.widget_day_subtext).visibility)
        assertEquals(android.view.View.VISIBLE, one.findViewById<android.view.View>(R.id.widget_header_weather).visibility)
        assertEquals("10°", one.findViewById<android.widget.TextView>(R.id.widget_header_weather_temp).text.toString())
        val emptyUpdate = WidgetRenderer.build(context, WidgetType.EXPRESS_AGENDA, emptyList(),
            customization = WidgetCustomization(hidePastEvents = false), box = WidgetBox(205f, 195f))
        emptyUpdate.reapply(context, one)
        assertEquals(android.view.View.GONE, one.findViewById<android.view.View>(R.id.widget_header_weather).visibility)
        assertEquals(android.view.View.VISIBLE, one.findViewById<android.view.View>(R.id.widget_empty_container).visibility)
        assertEquals(android.view.View.GONE, one.findViewById<android.view.View>(R.id.widget_weather_range).visibility)
        assertEquals(android.view.View.VISIBLE, one.findViewById<android.view.View>(R.id.widget_empty_weather_inline_icon).visibility)
        assertEquals("Max 12° · Min 7°", one.findViewById<android.widget.TextView>(R.id.widget_weather_summary).text.toString())
        val two = render(listOf(event, event.copy(id = 2)))
        assertEquals(android.view.View.GONE, two.findViewById<android.view.View>(R.id.widget_month_below).visibility)
        assertEquals(android.view.View.VISIBLE, two.findViewById<android.view.View>(R.id.widget_day_subtext).visibility)
        assertEquals(android.view.View.VISIBLE, two.findViewById<android.view.View>(R.id.widget_header_weather).visibility)
    }
    @Test fun referenceProportionsKeepComfortableMarginsAndFullDate() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val density = context.resources.displayMetrics.density
        val event = CalendarEvent(1, 4, "F1", "FP1: 2026 Brazilian Grand Prix", location = "Interlagos",
            startMillis = 1793979000000, endMillis = 1793982600000)
        val root = WidgetRenderer.build(context, WidgetType.EXPRESS_AGENDA, listOf(event),
            customization = WidgetCustomization(hidePastEvents = false), box = WidgetBox(205f, 195f))
            .apply(context, android.widget.FrameLayout(context)) as android.view.ViewGroup
        root.measure(android.view.View.MeasureSpec.makeMeasureSpec((205 * density).toInt(), android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec((195 * density).toInt(), android.view.View.MeasureSpec.EXACTLY))
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        val date = root.findViewById<android.widget.TextView>(R.id.widget_day_number)
        val card = root.findViewById<android.view.View>(R.id.widget_event_1)
        val bounds = android.graphics.Rect()
        card.getDrawingRect(bounds)
        root.offsetDescendantRectToMyCoords(card, bounds)
        assertEquals(android.view.View.VISIBLE, root.findViewById<android.view.View>(R.id.widget_date_row).visibility)
        assertTrue("Date should remain prominent", date.textSize / density >= 35f)
        assertTrue("Outer margin should match the reference", bounds.left >= 15f * density && root.width - bounds.right >= 15f * density)
        assertTrue("Event should fit vertically", bounds.bottom <= root.height - 12f * density)
    }
    @Test fun compactHomeKeepsTheFullDateWithMultipleEvents() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val event = CalendarEvent(1, 4, "F1", "FP3: 2026 Brazilian Grand Prix", location = "Interlagos",
            startMillis = 1794052200000, endMillis = 1794055800000)
        val box = WidgetBox(205f, 195f)
        val views = WidgetRenderer.build(context, WidgetType.EXPRESS_AGENDA, List(3) { event.copy(id = it.toLong()) },
            customization = WidgetCustomization(hidePastEvents = false), box = box)
        val root = views.apply(context, android.widget.FrameLayout(context))
        assertEquals(android.view.View.VISIBLE, root.findViewById<android.view.View>(R.id.widget_date_row).visibility)
        assertTrue(root.findViewById<android.widget.TextView>(R.id.widget_event_1_time).text.contains("Interlagos") ||
            root.findViewById<android.view.View>(R.id.widget_event_1_location).visibility == android.view.View.VISIBLE)
        assertEquals(android.view.View.GONE, root.findViewById<android.view.View>(R.id.widget_more_events).visibility)
    }
    @Test fun twoByTwoShowsTwoReadableEventsWithTheFullDate() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val density = context.resources.displayMetrics.density
        val events = listOf(
            CalendarEvent(1, 4, "F1", "FP3: 2026 Brazilian Grand Prix", location = "Interlagos", startMillis = 1794052200000, endMillis = 1794055800000),
            CalendarEvent(2, 5, "MotoGP", "MOTOGP: Sprint (Qatar)", location = "Qatar", startMillis = 1794067200000, endMillis = 1794070800000),
            CalendarEvent(3, 4, "F1", "Qualifiche", startMillis = 1794074400000, endMillis = 1794078000000))
        val views = WidgetRenderer.build(context, WidgetType.EXPRESS_AGENDA, events,
            customization = WidgetCustomization(hidePastEvents = false, showLocation = true), box = WidgetBox(205f, 195f))
        val root = views.apply(context, android.widget.FrameLayout(context)) as android.view.ViewGroup
        root.measure(android.view.View.MeasureSpec.makeMeasureSpec((205 * density).toInt(), android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec((195 * density).toInt(), android.view.View.MeasureSpec.EXACTLY))
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        assertEquals(android.view.View.VISIBLE, root.findViewById<android.view.View>(R.id.widget_date_row).visibility)
        assertEquals(android.view.View.VISIBLE, root.findViewById<android.view.View>(R.id.widget_day_name).visibility)
        assertEquals(android.view.View.VISIBLE, root.findViewById<android.view.View>(R.id.widget_event_1).visibility)
        assertEquals(android.view.View.VISIBLE, root.findViewById<android.view.View>(R.id.widget_event_2).visibility)
        assertEquals(root.findViewById<android.view.View>(R.id.widget_event_1).height,
            root.findViewById<android.view.View>(R.id.widget_event_2).height)
        val card = root.findViewById<android.view.View>(R.id.widget_event_1)
        val content = root.findViewById<android.view.View>(R.id.widget_event_1_content)
        assertTrue("Card must have a comfortable outer margin", card.left >= 12 * density)
        assertTrue("Card must have vertical padding", content.paddingTop >= 4 * density)
        assertTrue(root.findViewById<android.widget.TextView>(R.id.widget_event_1_time).text.contains("Interlagos"))
        assertTrue(root.findViewById<android.widget.TextView>(R.id.widget_event_2_time).text.contains("Qatar"))
        assertEquals(1, root.findViewById<android.widget.TextView>(R.id.widget_event_1_title).maxLines)
        assertEquals(1, root.findViewById<android.widget.TextView>(R.id.widget_event_2_title).maxLines)
        assertEquals(android.view.View.GONE, root.findViewById<android.view.View>(R.id.widget_more_events).visibility)
        for (id in listOf(R.id.widget_event_1_title, R.id.widget_event_2_title, R.id.widget_event_1_time, R.id.widget_event_2_time)) {
            val text = root.findViewById<android.widget.TextView>(id)
            assertTrue("Text must remain readable", text.textSize / density >= 11f)
            val bounds = android.graphics.Rect()
            text.getDrawingRect(bounds)
            root.offsetDescendantRectToMyCoords(text, bounds)
            assertTrue("Event text must fit inside the widget: $bounds", bounds.bottom <= root.height && bounds.right <= root.width)
        }
    }
    @Test fun blackAndWhiteAccentRemainSelectedAndVisible() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        for (color in listOf(android.graphics.Color.BLACK, android.graphics.Color.WHITE)) {
            val palette = com.example.widget.WidgetPalette.from(android.graphics.Color.BLACK, color)
            assertEquals(color, palette.accent)
            val root = WidgetRenderer.build(context, WidgetType.EXPRESS_AGENDA, emptyList(),
                customization = WidgetCustomization(accentColorIndex = color), box = WidgetBox(205f, 195f))
                .apply(context, android.widget.FrameLayout(context))
            val weekday = root.findViewById<android.widget.TextView>(R.id.widget_day_name)
            assertTrue(com.example.widget.WidgetPalette.contrastRatio(weekday.currentTextColor, palette.bgRgb) >= 4.5)
            assertNull("Weekday must not have a white backing", weekday.background)
        }
    }
    @Test @Config(qualifiers = "xxxhdpi")
    fun highDensityDisplaysRetainTheSameInformation() {
        fontAndWidgetSizeReflowWithoutClippingCards()
    }

    @Test @Config(qualifiers = "hdpi")
    fun mediumDensityDisplaysRetainTheSameInformation() {
        fontAndWidgetSizeReflowWithoutClippingCards()
    }
    @Test fun fontAndWidgetSizeReflowWithoutClippingCards() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        org.robolectric.Shadows.shadowOf(context as android.app.Application).grantPermissions(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        context.getSharedPreferences("weather_location_v2", android.content.Context.MODE_PRIVATE).edit()
            .putInt("temp", -12).putString("condition", "Poco nuvoloso")
            .putLong("timestamp", System.currentTimeMillis()).commit()
        val density = context.resources.displayMetrics.density
        val sample = CalendarEvent(1, 4, "F1", "FP2: 2026 Brazilian Grand Prix con un titolo molto lungo",
            location = "Autodromo di Interlagos, San Paolo", startMillis = 1793991600000, endMillis = 1793995200000)
        for ((width, height) in listOf(205f to 195f, 160f to 130f, 340f to 300f, 380f to 160f)) {
            for (scale in listOf(.8f, 1f, 1.3f, 1.6f)) {
                for (count in listOf(0, 1, 4)) {
                    val views = WidgetRenderer.build(context, WidgetType.EXPRESS_AGENDA, List(count) { sample.copy(id = it.toLong()) },
                        customization = WidgetCustomization(hidePastEvents = false, fontScale = scale), box = WidgetBox(width, height))
                    val root = views.apply(context, android.widget.FrameLayout(context)) as android.view.ViewGroup
                    root.measure(android.view.View.MeasureSpec.makeMeasureSpec((width * density).toInt(), android.view.View.MeasureSpec.EXACTLY),
                        android.view.View.MeasureSpec.makeMeasureSpec((height * density).toInt(), android.view.View.MeasureSpec.EXACTLY))
                    root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                    if (count > 0) {
                        assertTrue(root.findViewById<android.view.View>(R.id.widget_event_1_location).visibility == android.view.View.VISIBLE ||
                            root.findViewById<android.widget.TextView>(R.id.widget_event_1_time).text.contains("Autodromo di Interlagos"))
                        assertEquals("Autodromo di Interlagos, San Paolo", root.findViewById<android.widget.TextView>(R.id.widget_event_1_location).text.toString())
                        assertTrue(root.findViewById<android.widget.TextView>(R.id.widget_event_1_time).text.isNotBlank())
                    } else {
                        val temperature = root.findViewById<android.widget.TextView>(R.id.widget_empty_weather_temp)
                        assertEquals("-12°", temperature.text.toString())
                        assertTrue("Temperature must fit horizontally", temperature.paint.measureText(temperature.text.toString()) <= temperature.width + 1f)
                    }
                    val ids = if (count == 0) listOf(R.id.widget_empty_container) else listOf(R.id.widget_event_1, R.id.widget_event_2, R.id.widget_event_3, R.id.widget_more_events)
                    for (id in ids) {
                        val item = root.findViewById<android.view.View>(id)
                        if (item.visibility != android.view.View.VISIBLE) continue
                        val bounds = android.graphics.Rect()
                        item.getDrawingRect(bounds)
                        root.offsetDescendantRectToMyCoords(item, bounds)
                        assertTrue("$width x $height font=$scale count=$count view=$id bounds=$bounds", bounds.bottom <= root.height && bounds.right <= root.width && bounds.top >= 0)
                    }
                }
            }
        }
    }

    @Test fun launcherCanInflateEmptyAndEventLayouts() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        for (events in listOf(emptyList(), listOf(CalendarEvent(1, 4, "F1", "FP1: 2026 Brazilian Grand Prix", startMillis = 1793979000000, endMillis = 1793982600000)))) {
            val remote = WidgetRenderer.build(context, WidgetType.EXPRESS_AGENDA, events,
                customization = WidgetCustomization(hidePastEvents = false), box = WidgetBox(205f, 195f))
            val root = remote.apply(context, android.widget.FrameLayout(context))
            val density = context.resources.displayMetrics.density
            root.measure(android.view.View.MeasureSpec.makeMeasureSpec((205 * density).toInt(), android.view.View.MeasureSpec.EXACTLY),
                android.view.View.MeasureSpec.makeMeasureSpec((195 * density).toInt(), android.view.View.MeasureSpec.EXACTLY))
            root.layout(0, 0, root.measuredWidth, root.measuredHeight)
            val bottomView = root.findViewById<android.view.View>(if (events.isEmpty()) R.id.widget_empty_weather_desc else R.id.widget_event_1_time)
            val bounds = android.graphics.Rect()
            bottomView.getDrawingRect(bounds)
            (root as android.view.ViewGroup).offsetDescendantRectToMyCoords(bottomView, bounds)
            assertTrue("Content must fit inside a 2 by 2 widget: $bounds density=$density height=${root.height} day=${root.findViewById<android.widget.TextView>(R.id.widget_day_number).measuredHeight} empty=${root.findViewById<android.view.View>(R.id.widget_empty_container).measuredHeight} events=${events.size}", bounds.bottom <= root.height)
            assertNotNull(root.findViewById<android.widget.TextView>(R.id.widget_day_number))
            assertEquals(android.view.View.VISIBLE, root.findViewById<android.view.View>(
                if (events.isEmpty()) R.id.widget_empty_container else R.id.widget_event_1).visibility)
        }
    }
}
