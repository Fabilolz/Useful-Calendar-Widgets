package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.example.R
import com.example.data.WeatherRepository
import com.example.model.CalendarEvent
import com.example.model.WidgetCustomization
import com.example.model.WidgetType
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Unified RemoteViews builder for all Express Cal widgets.
 * Handles responsive layout sizing, WCAG contrast compliance, continuous opacity,
 * and locale-aware week/month rendering.
 */
object WidgetRenderer {

    fun build(
        context: Context,
        type: WidgetType,
        events: List<CalendarEvent>,
        day: Calendar = Calendar.getInstance(),
        customization: WidgetCustomization,
        box: WidgetBox = WidgetBox(200f, 150f),
        hasPermission: Boolean = true,
        eventDaysForMonth: Set<Int> = emptySet(),
        appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    ): RemoteViews {
        val palette = WidgetPalette.from(
            bgArgb = customization.getResolvedBackgroundColorInt(),
            accentIndex = customization.accentColorIndex,
            opacity = customization.backgroundOpacity
        )

        val refBox = when (type) {
            WidgetType.EXPRESS_AGENDA -> WidgetBox(220f, 220f)
            WidgetType.EXPRESS_UP_NEXT -> WidgetBox(220f, 80f)
            WidgetType.EXPRESS_MONTH -> WidgetBox(260f, 180f)
            WidgetType.EXPRESS_WEEK_STRIP -> WidgetBox(300f, 235f)
            WidgetType.EXPRESS_SPLIT_DAY -> WidgetBox(220f, 160f)
        }

        val metrics = WidgetMetrics.defaultFor(
            context = context,
            box = box,
            ref = refBox,
            userFontScale = customization.fontScale
        )

        return when (type) {
            WidgetType.EXPRESS_AGENDA -> buildAgenda(context, events, day, customization, palette, metrics, hasPermission)
            WidgetType.EXPRESS_UP_NEXT -> buildGlance(context, events, day, customization, palette, metrics, hasPermission, appWidgetId)
            WidgetType.EXPRESS_WEEK_STRIP -> buildWeek(context, events, day, customization, palette, metrics, hasPermission, appWidgetId)
            WidgetType.EXPRESS_MONTH -> buildMonth(context, events, day, customization, palette, metrics, hasPermission, eventDaysForMonth, appWidgetId)
            WidgetType.EXPRESS_SPLIT_DAY -> buildSplit(context, events, day, customization, palette, metrics, hasPermission)
        }
    }

    private fun eventSurface(palette: WidgetPalette, event: CalendarEvent): Int =
        WidgetPalette.blendColors(palette.bgRgb, event.calendarColor, if (palette.isLight) 0.16f else 0.24f)

    private fun applyCommonBackground(views: RemoteViews, palette: WidgetPalette) {
        views.setInt(R.id.widget_background_image, "setColorFilter", palette.bgRgb)
        views.setInt(R.id.widget_background_image, "setImageAlpha", palette.bgAlpha255)
    }

    private fun applyAgendaInset(views: RemoteViews, root: Int, metrics: WidgetMetrics) {
        val inset = metrics.agendaInsetPx
        views.setViewPadding(root, inset, inset, inset, inset)
    }

    // --- 1. EXPRESS AGENDA ---
    private fun buildAgenda(
        context: Context,
        events: List<CalendarEvent>,
        day: Calendar,
        customization: WidgetCustomization,
        palette: WidgetPalette,
        metrics: WidgetMetrics,
        hasPermission: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_agenda_layout)
        applyCommonBackground(views, palette)
        // Launchers may reapply a RemoteViews update to an existing view tree.
        views.setViewVisibility(R.id.widget_header_weather, View.GONE)
        views.setViewVisibility(R.id.widget_empty_weather_symbol, View.GONE)
        views.setViewVisibility(R.id.widget_more_events, View.GONE)

        val pendingIntent = WidgetHelper.getLaunchAppPendingIntent(context)
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        val locale = Locale.getDefault()
        val dayName = SimpleDateFormat("EEEE", locale).format(day.time).uppercase(locale)
        val daySubtext = SimpleDateFormat("MMMM", locale).format(day.time)
        val dayNumber = day.get(Calendar.DAY_OF_MONTH).toString()

        views.setTextViewText(R.id.widget_day_name, dayName)
        views.setTextColor(R.id.widget_day_name, palette.accentText)
        views.setTextViewTextSize(R.id.widget_day_name, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(WidgetTokens.TEXT_LABEL + 1f))

        views.setTextViewText(R.id.widget_day_subtext, daySubtext)
        views.setTextViewText(R.id.widget_month_below, daySubtext)
        views.setTextColor(R.id.widget_month_below, palette.onBgVariant)
        views.setTextViewTextSize(R.id.widget_month_below, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(16f))
        views.setTextViewText(R.id.widget_day_number, dayNumber)
        views.setTextColor(R.id.widget_day_number, palette.onBg)
        views.setTextViewTextSize(R.id.widget_day_number, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(46f))
        views.setTextColor(R.id.widget_day_subtext, palette.onBgVariant)
        views.setTextViewTextSize(R.id.widget_day_subtext, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(16f))

        val candidates = if (customization.hidePastEvents) {
            events.filter { it.isUpcomingOrCurrent() }
        } else {
            events
        }
        if (!hasPermission) {
            views.setViewVisibility(R.id.widget_empty_container, View.GONE)
            views.setViewVisibility(R.id.widget_empty_text, View.VISIBLE)
            views.setTextViewText(R.id.widget_empty_text, context.getString(R.string.permission_required_widget))
            views.setTextColor(R.id.widget_empty_text, palette.onBgVariant)
            views.setViewVisibility(R.id.widget_event_1, View.GONE)
            views.setViewVisibility(R.id.widget_event_2, View.GONE)
            views.setViewVisibility(R.id.widget_event_3, View.GONE)
            return AgendaAdaptiveLayout.fit(context, views, metrics, day, 0, palette)
        }

        val activeEvents = candidates.take(3)

        if (candidates.isNotEmpty()) bindInlineWeather(context, views, palette, metrics)

        if (activeEvents.isEmpty() && !WeatherRepository.showInWidgets(context)) {
            views.setViewVisibility(R.id.widget_empty_container, View.GONE)
            views.setViewVisibility(R.id.widget_empty_text, View.VISIBLE)
            views.setTextViewText(R.id.widget_empty_text, context.getString(R.string.no_events_day))
            views.setTextColor(R.id.widget_empty_text, palette.onBgVariant)
            return AgendaAdaptiveLayout.fit(context, views, metrics, day, 0, palette)
        }
        if (activeEvents.isEmpty()) {
            views.setViewVisibility(R.id.widget_empty_text, View.GONE)
            views.setViewVisibility(R.id.widget_empty_container, View.VISIBLE)

            val weather = WeatherRepository.getWeather(context)
            val weatherIcon = weatherIconResource(weather.condition)
            val compactWeather = metrics.box.widthDp <= 230f && metrics.box.heightDp <= 230f
            val alignmentSpacer = if (compactWeather) View.VISIBLE else View.GONE
            views.setViewVisibility(R.id.widget_weather_temp_spacer, alignmentSpacer)
            views.setViewVisibility(R.id.widget_weather_condition_spacer, alignmentSpacer)
            views.setViewVisibility(R.id.widget_empty_weather_inline_icon, if (compactWeather && weather.isAvailable) View.VISIBLE else View.GONE)
            views.setImageViewResource(R.id.widget_empty_weather_inline_icon, weatherIcon)
            views.setViewVisibility(R.id.widget_empty_weather_symbol, if (!compactWeather && weather.isAvailable) View.VISIBLE else View.GONE)
            views.setImageViewResource(R.id.widget_empty_weather_symbol, weatherIcon)
            views.setTextViewText(R.id.widget_empty_weather_temp, if (weather.isAvailable) "${weather.tempC}°" else "—")
            views.setTextColor(R.id.widget_empty_weather_temp, palette.onCard)
            val conditionId = when (weather.condition) {
                "Sereno" -> R.string.weather_clear
                "Poco nuvoloso" -> R.string.weather_partly_cloudy
                "Nuvoloso" -> R.string.weather_cloudy
                "Nebbia" -> R.string.weather_fog
                "Pioggia" -> R.string.weather_rain
                "Neve" -> R.string.weather_snow
                "Rovesci" -> R.string.weather_showers
                "Temporale" -> R.string.weather_storm
                else -> R.string.weather_unknown
            }
            views.setTextViewText(R.id.widget_empty_weather_desc, context.getString(conditionId))
            views.setTextColor(R.id.widget_empty_weather_desc, palette.onBg)
            val high = weather.outlook?.high
            val low = weather.outlook?.low
            views.setViewVisibility(R.id.widget_weather_range, if (!compactWeather && high != null && low != null) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_weather_summary, if (compactWeather && high != null && low != null) View.VISIBLE else View.GONE)
            if (compactWeather && high != null && low != null) {
                views.setTextViewText(R.id.widget_weather_summary, context.getString(R.string.widget_weather_summary, high, low))
                views.setTextColor(R.id.widget_weather_summary, palette.onBgVariant)
                views.setTextViewTextSize(R.id.widget_weather_summary, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(12f))
            }
            if (!compactWeather && high != null && low != null) {
                views.setTextViewText(R.id.widget_weather_low, "${low}°")
                views.setTextViewText(R.id.widget_weather_high, "${high}°")
                views.setTextColor(R.id.widget_weather_low, palette.onBgVariant)
                views.setTextColor(R.id.widget_weather_high, palette.onBg)
                val fraction = if (high > low) ((weather.tempC - low).toFloat() / (high - low)).coerceIn(0f, 1f) else .5f
                val density = context.resources.displayMetrics.density
                val trackWidth = (metrics.box.widthDp * .52f * density).coerceAtLeast(1f)
                val trackHeight = 12f * density
                val track = android.graphics.Bitmap.createBitmap(trackWidth.toInt(), trackHeight.toInt(), android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(track)
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                val centerY = track.height / 2f
                val radius = 4f * density
                paint.color = WidgetPalette.ensureContrast(0xFF86BCFF.toInt(), palette.bgRgb, 3.0)
                canvas.drawRoundRect(0f, centerY - 1.75f * density, track.width.toFloat(), centerY + 1.75f * density, 2f * density, 2f * density, paint)
                val markerX = radius + (track.width - 2 * radius).coerceAtLeast(0f) * fraction
                paint.color = palette.bgRgb
                canvas.drawCircle(markerX, centerY, radius + density, paint)
                paint.color = palette.onBg
                canvas.drawCircle(markerX, centerY, radius, paint)
                views.setImageViewBitmap(R.id.widget_weather_track, track)
            }


            views.setContentDescription(R.id.widget_empty_container, context.getString(R.string.no_events_day) + ". " + context.getString(conditionId) + if (weather.isAvailable) ", ${weather.tempC}°" else "")

            views.setOnClickPendingIntent(
                R.id.widget_empty_container,
                WidgetHelper.getLaunchAppPendingIntent(context, "agenda")
            )

            views.setViewVisibility(R.id.widget_event_1, View.GONE)
            views.setViewVisibility(R.id.widget_event_2, View.GONE)
            views.setViewVisibility(R.id.widget_event_3, View.GONE)
            return AgendaAdaptiveLayout.fit(context, views, metrics, day, candidates.size, palette)
        }

        views.setViewVisibility(R.id.widget_empty_container, View.GONE)
        views.setViewVisibility(R.id.widget_empty_text, View.GONE)

        // Event 1
        val ev1 = activeEvents.getOrNull(0)
        if (ev1 != null) {
            views.setViewVisibility(R.id.widget_event_1, View.VISIBLE)
            views.setOnClickPendingIntent(
                R.id.widget_event_1,
                WidgetHelper.getLaunchAppPendingIntent(context, "agenda", ev1.id, ev1.startMillis)
            )
            views.setInt(R.id.widget_event_1_card_bg, "setColorFilter", eventSurface(palette, ev1))
            views.setInt(R.id.widget_event_1_bar, "setColorFilter", WidgetPalette.ensureContrast(ev1.calendarColor, palette.bgRgb, 3.0))

            views.setTextViewText(R.id.widget_event_1_title, ev1.title)
            views.setTextColor(R.id.widget_event_1_title, palette.onCard)
            views.setTextViewTextSize(R.id.widget_event_1_title, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(WidgetTokens.TEXT_TITLE + 1f))

            views.setTextViewText(R.id.widget_event_1_time, ev1.formatTimeRange(context))
            views.setTextColor(R.id.widget_event_1_time, WidgetPalette.ensureContrast(ev1.calendarColor, eventSurface(palette, ev1), 4.5))
            views.setTextViewTextSize(R.id.widget_event_1_time, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(WidgetTokens.TEXT_BODY))

            if (customization.showLocation && !ev1.location.isNullOrBlank()) {
                views.setViewVisibility(R.id.widget_event_1_location, View.VISIBLE)
                views.setTextViewText(R.id.widget_event_1_location, ev1.location)
                views.setTextColor(R.id.widget_event_1_location, palette.onBgVariant)
                views.setTextViewTextSize(R.id.widget_event_1_location, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(WidgetTokens.TEXT_LABEL + 1f))
            } else {
                views.setViewVisibility(R.id.widget_event_1_location, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.widget_event_1, View.GONE)
        }

        // Event 2 (adaptive: only show if space allows, >= 130dp height)
        val ev2 = activeEvents.getOrNull(1)
        if (ev2 != null) {
            views.setViewVisibility(R.id.widget_event_2, View.VISIBLE)
            views.setOnClickPendingIntent(
                R.id.widget_event_2,
                WidgetHelper.getLaunchAppPendingIntent(context, "agenda", ev2.id, ev2.startMillis)
            )
            views.setInt(R.id.widget_event_2_card_bg, "setColorFilter", eventSurface(palette, ev2))
            views.setInt(R.id.widget_event_2_bar, "setColorFilter", WidgetPalette.ensureContrast(ev2.calendarColor, palette.bgRgb, 3.0))

            views.setTextViewText(R.id.widget_event_2_title, ev2.title)
            views.setTextColor(R.id.widget_event_2_title, palette.onCard)
            views.setTextViewTextSize(R.id.widget_event_2_title, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(WidgetTokens.TEXT_TITLE + 1f))

            views.setTextViewText(R.id.widget_event_2_time, ev2.formatTimeRange(context))
            views.setTextColor(R.id.widget_event_2_time, WidgetPalette.ensureContrast(ev2.calendarColor, eventSurface(palette, ev2), 4.5))
            views.setTextViewTextSize(R.id.widget_event_2_time, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(WidgetTokens.TEXT_BODY))

            if (customization.showLocation && !ev2.location.isNullOrBlank()) {
                views.setViewVisibility(R.id.widget_event_2_location, View.VISIBLE)
                views.setTextViewText(R.id.widget_event_2_location, ev2.location)
                views.setTextColor(R.id.widget_event_2_location, palette.onBgVariant)
                views.setTextViewTextSize(R.id.widget_event_2_location, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(WidgetTokens.TEXT_LABEL + 1f))
            } else {
                views.setViewVisibility(R.id.widget_event_2_location, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.widget_event_2, View.GONE)
        }

        // Event 3 (adaptive: only show if height >= 220dp)
        val ev3 = activeEvents.getOrNull(2)
        if (ev3 != null) {
            views.setViewVisibility(R.id.widget_event_3, View.VISIBLE)
            views.setOnClickPendingIntent(
                R.id.widget_event_3,
                WidgetHelper.getLaunchAppPendingIntent(context, "agenda", ev3.id, ev3.startMillis)
            )
            views.setInt(R.id.widget_event_3_card_bg, "setColorFilter", eventSurface(palette, ev3))
            views.setInt(R.id.widget_event_3_bar, "setColorFilter", WidgetPalette.ensureContrast(ev3.calendarColor, palette.bgRgb, 3.0))

            views.setTextViewText(R.id.widget_event_3_title, ev3.title)
            views.setTextColor(R.id.widget_event_3_title, palette.onCard)
            views.setTextViewTextSize(R.id.widget_event_3_title, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(WidgetTokens.TEXT_TITLE + 1f))

            views.setTextViewText(R.id.widget_event_3_time, ev3.formatTimeRange(context))
            views.setTextColor(R.id.widget_event_3_time, WidgetPalette.ensureContrast(ev3.calendarColor, eventSurface(palette, ev3), 4.5))
            views.setTextViewTextSize(R.id.widget_event_3_time, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(WidgetTokens.TEXT_BODY))

            if (customization.showLocation && !ev3.location.isNullOrBlank()) {
                views.setViewVisibility(R.id.widget_event_3_location, View.VISIBLE)
                views.setTextViewText(R.id.widget_event_3_location, ev3.location)
                views.setTextColor(R.id.widget_event_3_location, palette.onBgVariant)
                views.setTextViewTextSize(R.id.widget_event_3_location, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(WidgetTokens.TEXT_LABEL + 1f))
            } else {
                views.setViewVisibility(R.id.widget_event_3_location, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.widget_event_3, View.GONE)
        }

        return AgendaAdaptiveLayout.fit(context, views, metrics, day, candidates.size, palette)
    }

    // --- 2. EXPRESS UP NEXT (GLANCE) ---
    private fun weatherIconResource(condition: String): Int = when (condition) {
        "Sereno" -> R.drawable.weather_sun_outline
        "Pioggia", "Rovesci" -> R.drawable.weather_rain_outline
        "Neve" -> R.drawable.weather_snow_outline
        "Temporale" -> R.drawable.weather_storm_outline
        "Nebbia" -> R.drawable.weather_fog_outline
        else -> R.drawable.weather_cloud_outline
    }
    private fun bindInlineWeather(context: Context, views: RemoteViews, palette: WidgetPalette, metrics: WidgetMetrics) {
        if (!WeatherRepository.showInWidgets(context)) return
        val weather = WeatherRepository.getWeather(context)
        val icon = weatherIconResource(weather.condition)
        views.setViewVisibility(R.id.widget_header_weather, View.VISIBLE)
        views.setViewVisibility(R.id.widget_header_weather_icon, if (weather.isAvailable) View.VISIBLE else View.GONE)
        views.setImageViewResource(R.id.widget_header_weather_icon, icon)
        views.setTextViewText(R.id.widget_header_weather_temp, if (weather.isAvailable) "${weather.tempC}°" else "—")
        views.setTextColor(R.id.widget_header_weather_temp, palette.onBg)
        views.setTextViewTextSize(R.id.widget_header_weather_temp, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(22f))
        views.setContentDescription(R.id.widget_header_weather, if (weather.isAvailable) "${weather.condition}, ${weather.tempC}°" else context.getString(R.string.weather_unknown))
    }

    private fun buildGlance(
        context: Context,
        events: List<CalendarEvent>,
        day: Calendar,
        customization: WidgetCustomization,
        palette: WidgetPalette,
        metrics: WidgetMetrics,
        hasPermission: Boolean,
        appWidgetId: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_glance_layout)
        applyCommonBackground(views, palette)

        val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        val columns = options.getInt("semAppWidgetColumnSpan")
        val rows = options.getInt("semAppWidgetRowSpan")
        val wideSingleRow = if (columns > 0 && rows > 0) {
            columns == 4 && rows == 1
        } else {
            metrics.box.widthDp >= 300f && metrics.box.heightDp <= 110f
        }
        views.setViewVisibility(R.id.widget_header_weather, View.GONE)
        if (wideSingleRow) bindInlineWeather(context, views, palette, metrics)
        val pendingIntent = WidgetHelper.getLaunchAppPendingIntent(context)
        views.setOnClickPendingIntent(R.id.widget_glance_root, pendingIntent)

        val locale = Locale.getDefault()
        val dayName = SimpleDateFormat("EEE", locale).format(day.time).uppercase(locale)
        val dayNumber = day.get(Calendar.DAY_OF_MONTH).toString()

        views.setInt(R.id.widget_glance_date_bubble_bg, "setColorFilter", palette.card)
        views.setTextViewText(R.id.widget_glance_day_name, dayName)
        views.setTextColor(R.id.widget_glance_day_name, palette.accentText)
        views.setTextViewTextSize(R.id.widget_glance_day_name, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(10f))

        views.setTextViewText(R.id.widget_glance_day_number, dayNumber)
        views.setTextColor(R.id.widget_glance_day_number, palette.onBg)
        views.setTextViewTextSize(R.id.widget_glance_day_number, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(WidgetTokens.TEXT_DISPLAY_GLANCE))

        if (!hasPermission) {
            views.setViewVisibility(R.id.widget_glance_label, View.GONE)
            views.setViewVisibility(R.id.widget_glance_badge, View.GONE)
            views.setViewVisibility(R.id.widget_glance_title, View.GONE)
            views.setViewVisibility(R.id.widget_glance_time, View.GONE)
            views.setViewVisibility(R.id.widget_glance_empty_text, View.VISIBLE)
            views.setTextViewText(R.id.widget_glance_empty_text, context.getString(R.string.permission_required_widget))
            views.setTextColor(R.id.widget_glance_empty_text, palette.onBgVariant)
            return views
        }

        val now = System.currentTimeMillis()
        val currentOrNext = events.firstOrNull { it.isHappeningNow(now) }
            ?: events.firstOrNull { it.isUpcoming(now) }
            ?: events.firstOrNull { it.isUpcomingOrCurrent(now) }

        if (currentOrNext == null) {
            views.setViewVisibility(R.id.widget_glance_label, View.GONE)
            views.setViewVisibility(R.id.widget_glance_badge, View.GONE)
            views.setViewVisibility(R.id.widget_glance_title, View.GONE)
            views.setViewVisibility(R.id.widget_glance_time, View.GONE)
            views.setViewVisibility(R.id.widget_glance_empty_text, View.VISIBLE)
            views.setTextViewText(R.id.widget_glance_empty_text, context.getString(R.string.all_clear_today))
            views.setTextColor(R.id.widget_glance_empty_text, palette.onBgVariant)
            return views
        }

        views.setViewVisibility(R.id.widget_glance_empty_text, View.GONE)
        views.setViewVisibility(R.id.widget_glance_label, View.VISIBLE)
        views.setViewVisibility(R.id.widget_glance_badge, View.VISIBLE)
        views.setViewVisibility(R.id.widget_glance_title, View.VISIBLE)
        views.setViewVisibility(R.id.widget_glance_time, View.VISIBLE)

        views.setOnClickPendingIntent(
            R.id.widget_glance_root,
            WidgetHelper.getLaunchAppPendingIntent(context, "agenda", currentOrNext.id, currentOrNext.startMillis)
        )

        val isHappening = currentOrNext.isHappeningNow(now)
        val labelText = if (isHappening) {
            context.getString(R.string.happening_now)
        } else {
            context.getString(R.string.up_next)
        }
        views.setTextViewText(R.id.widget_glance_label, labelText)
        views.setTextColor(R.id.widget_glance_label, palette.accentText)
        views.setTextViewTextSize(R.id.widget_glance_label, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(10f))

        // Badge: NEVER duplicate "happening now". Show "Ends in X" or "in X"
        val badgeText = if (isHappening) {
            val remainMillis = currentOrNext.effectiveEndMillis - now
            val min = ((remainMillis + 59_999) / (60 * 1000)).coerceAtLeast(1)
            val hrs = min / 60
            val remStr = if (hrs > 0) "${hrs}h ${min % 60}m" else "${min}m"
            context.getString(R.string.ends_in, remStr) // "Ends in ..." with capital E
        } else {
            currentOrNext.getRelativeTimeText(context, now)
        }
        views.setInt(R.id.widget_glance_badge_bg, "setColorFilter", palette.card)
        views.setTextViewText(R.id.widget_glance_badge, badgeText)
        views.setTextColor(R.id.widget_glance_badge, palette.accentText)
        views.setTextViewTextSize(R.id.widget_glance_badge, TypedValue.COMPLEX_UNIT_DIP, minOf(metrics.responsiveTextDp(9.5f), 14f))

        views.setTextViewText(R.id.widget_glance_title, currentOrNext.title)
        views.setTextColor(R.id.widget_glance_title, palette.onBg)
        views.setTextViewTextSize(R.id.widget_glance_title, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(WidgetTokens.TEXT_TITLE))

        val loc = if (customization.showLocation && !currentOrNext.location.isNullOrBlank()) " • ${currentOrNext.location}" else ""
        views.setTextViewText(R.id.widget_glance_time, "${currentOrNext.formatTimeRange(context)}$loc")
        views.setTextColor(R.id.widget_glance_time, palette.onBgVariant)
        views.setTextViewTextSize(R.id.widget_glance_time, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(WidgetTokens.TEXT_BODY))

        return views
    }

    // --- 3. EXPRESS WEEK TIMELINE ---
    private fun buildWeek(
        context: Context,
        events: List<CalendarEvent>,
        day: Calendar,
        customization: WidgetCustomization,
        palette: WidgetPalette,
        metrics: WidgetMetrics,
        hasPermission: Boolean,
        appWidgetId: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_week_layout)
        applyCommonBackground(views, palette)
        applyAgendaInset(views, R.id.widget_week_content_root, metrics)

        val pendingIntent = WidgetHelper.getLaunchAppPendingIntent(context)
        views.setOnClickPendingIntent(R.id.widget_week_root, pendingIntent)

        val locale = Locale.getDefault()
        // Samsung exposes the actual grid spans. Other launchers report dimensions in dp.
        val largeCompactText = if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            false
        } else {
            val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
            val columns = options.getInt("semAppWidgetColumnSpan")
            val rows = options.getInt("semAppWidgetRowSpan")
            if (columns > 0 && rows > 0) {
                columns == 4 && rows == 2
            } else {
                metrics.box.widthDp >= 280f && metrics.box.heightDp < 220f &&
                    metrics.box.widthDp >= 1.8f * metrics.box.heightDp
            }
        }
        val weekHeaderTextSize = if (largeCompactText) {
            minOf(metrics.responsiveTextDp(WidgetTokens.TEXT_LABEL + 1f), 15f)
        } else {
            metrics.responsiveTextDp(WidgetTokens.TEXT_LABEL)
        }
        val monthYearTitle = SimpleDateFormat("MMMM yyyy", locale).format(day.time).uppercase(locale)
        views.setTextViewText(R.id.widget_week_title, monthYearTitle)
        views.setTextColor(R.id.widget_week_title, palette.accentText)
        views.setTextViewTextSize(R.id.widget_week_title, TypedValue.COMPLEX_UNIT_DIP, weekHeaderTextSize)

        val now = System.currentTimeMillis()
        val visibleEvents = if (customization.hidePastEvents) events.filter { it.isUpcomingOrCurrent(now) } else events
        views.setTextViewText(R.id.widget_week_count, context.getString(R.string.events_count, visibleEvents.size))
        views.setTextColor(R.id.widget_week_count, palette.accentText)
        views.setTextViewTextSize(R.id.widget_week_count, TypedValue.COMPLEX_UNIT_DIP, weekHeaderTextSize)

        // Calculate 7-day pill columns based on first day of week
        val firstDayOfWeek = Calendar.getInstance(locale).firstDayOfWeek
        val weekCal = day.clone() as Calendar
        val dayOfWeek = weekCal.get(Calendar.DAY_OF_WEEK)
        val diff = (dayOfWeek - firstDayOfWeek + 7) % 7
        weekCal.add(Calendar.DAY_OF_MONTH, -diff)

        val shortWeekdays = DateFormatSymbols.getInstance(locale).shortWeekdays

        val pillIds = intArrayOf(
            R.id.week_day_1, R.id.week_day_2, R.id.week_day_3,
            R.id.week_day_4, R.id.week_day_5, R.id.week_day_6, R.id.week_day_7
        )
        val pillBgIds = intArrayOf(
            R.id.week_day_1_bg, R.id.week_day_2_bg, R.id.week_day_3_bg,
            R.id.week_day_4_bg, R.id.week_day_5_bg, R.id.week_day_6_bg, R.id.week_day_7_bg
        )
        val pillLblIds = intArrayOf(
            R.id.week_day_1_lbl, R.id.week_day_2_lbl, R.id.week_day_3_lbl,
            R.id.week_day_4_lbl, R.id.week_day_5_lbl, R.id.week_day_6_lbl, R.id.week_day_7_lbl
        )
        val pillNumIds = intArrayOf(
            R.id.week_day_1_num, R.id.week_day_2_num, R.id.week_day_3_num,
            R.id.week_day_4_num, R.id.week_day_5_num, R.id.week_day_6_num, R.id.week_day_7_num
        )

        val todayCal = Calendar.getInstance()
        for (i in 0..6) {
            val iterCal = weekCal.clone() as Calendar
            iterCal.add(Calendar.DAY_OF_MONTH, i)

            val dNum = iterCal.get(Calendar.DAY_OF_MONTH).toString()
            val dow = iterCal.get(Calendar.DAY_OF_WEEK)
            val dLbl = shortWeekdays.getOrNull(dow)?.take(2)?.uppercase(locale) ?: "D"

            val isSelected = iterCal.get(Calendar.YEAR) == day.get(Calendar.YEAR) &&
                    iterCal.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)

            views.setTextViewText(pillLblIds[i], dLbl)
            views.setTextViewText(pillNumIds[i], dNum)

            views.setTextViewTextSize(pillLblIds[i], TypedValue.COMPLEX_UNIT_DIP, minOf(metrics.responsiveTextDp(if (largeCompactText) 12f else 9.5f), if (largeCompactText) 15f else 14f))
            views.setTextViewTextSize(pillNumIds[i], TypedValue.COMPLEX_UNIT_DIP, minOf(metrics.responsiveTextDp(if (largeCompactText) 14.5f else 13f), 19f))

            if (isSelected) {
                views.setInt(pillBgIds[i], "setColorFilter", palette.accent)
                views.setTextColor(pillLblIds[i], palette.onAccent)
                views.setTextColor(pillNumIds[i], palette.onAccent)
            } else {
                views.setInt(pillBgIds[i], "setColorFilter", palette.pill)
                views.setTextColor(pillLblIds[i], palette.onBgVariant)
                views.setTextColor(pillNumIds[i], palette.onBg)
            }

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val selectIntent = android.content.Intent(context, ExpressWeekWidgetProvider::class.java).apply {
                    action = SelectedDateStore.ACTION_SELECT_DATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(SelectedDateStore.EXTRA_YEAR, iterCal.get(Calendar.YEAR))
                    putExtra(SelectedDateStore.EXTRA_MONTH, iterCal.get(Calendar.MONTH))
                    putExtra(SelectedDateStore.EXTRA_DAY, iterCal.get(Calendar.DAY_OF_MONTH))
                }
                val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                } else {
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pi = android.app.PendingIntent.getBroadcast(
                    context,
                    (appWidgetId * 100) + i,
                    selectIntent,
                    flags
                )
                views.setOnClickPendingIntent(pillIds[i], pi)
            }
        }

        val cards = intArrayOf(R.id.widget_week_event_card, R.id.widget_week_event_card_2, R.id.widget_week_event_card_3, R.id.widget_week_event_card_4)
        val backgrounds = intArrayOf(R.id.widget_week_event_card_bg, R.id.widget_week_event_card_2_bg, R.id.widget_week_event_card_3_bg, R.id.widget_week_event_card_4_bg)
        val bars = intArrayOf(R.id.widget_week_event_bar, R.id.widget_week_event_bar_2, R.id.widget_week_event_bar_3, R.id.widget_week_event_bar_4)
        val titles = intArrayOf(R.id.widget_week_event_title, R.id.widget_week_event_title_2, R.id.widget_week_event_title_3, R.id.widget_week_event_title_4)
        val times = intArrayOf(R.id.widget_week_event_time, R.id.widget_week_event_time_2, R.id.widget_week_event_time_3, R.id.widget_week_event_time_4)
        val hours = intArrayOf(R.id.widget_week_hours, R.id.widget_week_hours_2, R.id.widget_week_hours_3, R.id.widget_week_hours_4)
        val canSetHeight = android.os.Build.VERSION.SDK_INT >= 31
        val cardHeight = if (canSetHeight && metrics.box.heightDp >= 300f) 42f else 37f
        val headerHeight = 2 * metrics.agendaInsetDp + 61f + 15f
        val capacity = ((metrics.box.heightDp - headerHeight + 6f) / (cardHeight + 6f)).toInt().coerceIn(1, cards.size)
        val scrollable = canSetHeight && appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID
        val eventsToShow = when {
            !hasPermission -> emptyList()
            scrollable -> visibleEvents
            else -> visibleEvents.take(capacity)
        }
        views.setViewVisibility(R.id.widget_week_empty_text, if (eventsToShow.isEmpty()) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.widget_week_empty_text,
            context.getString(if (hasPermission) R.string.no_events_day else R.string.permission_required_widget))
        views.setTextColor(R.id.widget_week_empty_text, palette.onBgVariant)

        fun bindEvent(row: RemoteViews, index: Int, event: CalendarEvent) {
        val surface = eventSurface(palette, event)
        row.setInt(backgrounds[index], "setColorFilter", surface)
        row.setInt(bars[index], "setColorFilter", WidgetPalette.ensureContrast(event.calendarColor, surface, 3.0))
        row.setTextViewText(titles[index], event.title)
        row.setTextColor(titles[index], palette.onCard)
        row.setTextViewTextSize(titles[index], TypedValue.COMPLEX_UNIT_DIP, minOf(metrics.responsiveTextDp(if (largeCompactText) 14.5f else 13f), if (largeCompactText) 16.5f else 16f))
        val location = if (customization.showLocation) event.location.orEmpty() else ""
        row.setTextViewText(times[index], location)
        row.setViewVisibility(times[index], if (location.isBlank()) View.GONE else View.VISIBLE)
        row.setTextColor(times[index], WidgetPalette.ensureContrast(event.calendarColor, surface, 4.5))
        row.setTextViewTextSize(times[index], TypedValue.COMPLEX_UNIT_DIP, minOf(metrics.responsiveTextDp(if (largeCompactText) 12.5f else 11f), if (largeCompactText) 13.5f else 13f))
        row.setTextViewText(hours[index], event.formatTimeRange(context))
        row.setTextColor(hours[index], WidgetPalette.ensureContrast(event.calendarColor, surface, 4.5))
        row.setTextViewTextSize(hours[index], TypedValue.COMPLEX_UNIT_DIP, minOf(metrics.responsiveTextDp(if (largeCompactText) 12.5f else 11f), if (largeCompactText) 13.5f else 13f))
        }

        cards.forEachIndexed { index, card ->
            val event = if (scrollable) null else eventsToShow.getOrNull(index)
            views.setViewVisibility(card, if (event == null) View.GONE else View.VISIBLE)
            if (event != null) {
                if (canSetHeight) views.setViewLayoutHeight(card, cardHeight, TypedValue.COMPLEX_UNIT_DIP)
                bindEvent(views, index, event)
                views.setOnClickPendingIntent(card, WidgetHelper.getLaunchAppPendingIntent(context, "agenda", event.id, event.startMillis))
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= 31 && scrollable) {
            val items = RemoteViews.RemoteCollectionItems.Builder().setViewTypeCount(1)
            eventsToShow.forEachIndexed { index, event ->
                val row = RemoteViews(context.packageName, R.layout.widget_week_scroll_row)
                row.setViewLayoutHeight(cards[0], cardHeight, TypedValue.COMPLEX_UNIT_DIP)
                bindEvent(row, 0, event)
                val fillIn = android.content.Intent().apply {
                    putExtra("target_tab", "agenda")
                    putExtra("event_id", event.id)
                    putExtra("start_millis", event.startMillis)
                }
                row.setOnClickFillInIntent(cards[0], fillIn)
                items.addItem(index.toLong(), row)
            }
            val launch = android.content.Intent(context, com.example.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val template = android.app.PendingIntent.getActivity(context, appWidgetId, launch,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE)
            views.setPendingIntentTemplate(R.id.widget_week_events_list, template)
            views.setRemoteAdapter(R.id.widget_week_events_list, items.build())
            views.setViewVisibility(R.id.widget_week_events_list, if (eventsToShow.isEmpty()) View.GONE else View.VISIBLE)
        }
        val displayed = eventsToShow.size
        if (hasPermission && displayed < visibleEvents.size) {
            views.setTextViewText(R.id.widget_week_count, context.getString(R.string.widget_week_visible_count, displayed, visibleEvents.size))
        }
        return views
    }

    // --- 4. EXPRESS MONTH & AGENDA ---
    private fun buildMonth(
        context: Context,
        events: List<CalendarEvent>,
        day: Calendar,
        customization: WidgetCustomization,
        palette: WidgetPalette,
        metrics: WidgetMetrics,
        hasPermission: Boolean,
        eventDaysForMonth: Set<Int>,
        appWidgetId: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_month_layout)
        applyCommonBackground(views, palette)
        applyAgendaInset(views, R.id.widget_month_content_root, metrics)

        val pendingIntent = WidgetHelper.getLaunchAppPendingIntent(context)
        views.setOnClickPendingIntent(R.id.widget_month_root, pendingIntent)

        val locale = Locale.getDefault()
        val monthYear = SimpleDateFormat("MMMM yyyy", locale).format(day.time).uppercase(locale)

        views.setTextViewText(R.id.widget_month_title, monthYear)
        views.setTextColor(R.id.widget_month_title, palette.accentText)
        views.setTextViewTextSize(R.id.widget_month_title, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(WidgetTokens.TEXT_LABEL))

        views.setInt(R.id.widget_month_badge_bg, "setColorFilter", palette.card)
        views.setTextViewText(R.id.widget_month_badge, context.getString(R.string.events_today, events.size))
        views.setTextColor(R.id.widget_month_badge, palette.accentText)
        views.setTextViewTextSize(R.id.widget_month_badge, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(10.5f))

        // Weekday headers according to firstDayOfWeek
        val firstDayOfWeek = Calendar.getInstance(locale).firstDayOfWeek
        val shortWeekdays = DateFormatSymbols.getInstance(locale).shortWeekdays
        val compactMonth = metrics.box.widthDp <= 230f && metrics.box.heightDp <= 220f
        val expandedMonth = !compactMonth && android.os.Build.VERSION.SDK_INT >= 31
        val options = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
        } else {
            null
        }
        val columns = options?.getInt("semAppWidgetColumnSpan") ?: 0
        val rows = options?.getInt("semAppWidgetRowSpan") ?: 0
        val compactTiles = expandedMonth && if (columns > 0 && rows > 0) {
            columns in 3..4 && rows == 3
        } else {
            metrics.box.widthDp >= 260f && metrics.box.heightDp in 270f..<360f &&
                metrics.box.widthDp / metrics.box.heightDp in .85f..1.6f
        }
        if (compactMonth) {
            val density = context.resources.displayMetrics.density
            val firstLabel = shortWeekdays[firstDayOfWeek].take(1).uppercase(locale)
            val paint = android.graphics.Paint().apply {
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textSize = metrics.responsiveTextDp(10f) * density
            }
            val firstColumnCenter = (metrics.box.widthDp * density - metrics.agendaInsetPx * 2) / 14f
            val inset = (firstColumnCenter - paint.measureText(firstLabel) / 2f).toInt().coerceAtLeast(0)
            views.setViewPadding(R.id.widget_month_header, inset, 0, 0, 0)
        } else {
            val density = context.resources.displayMetrics.density
            views.setViewPadding(R.id.widget_month_header, (14 * density).toInt(), 0, (6 * density).toInt(), 0)
        }
        val weekdayIds = intArrayOf(
            R.id.widget_month_w1, R.id.widget_month_w2, R.id.widget_month_w3,
            R.id.widget_month_w4, R.id.widget_month_w5, R.id.widget_month_w6, R.id.widget_month_w7
        )

        for (i in 0..6) {
            val calDayIndex = (firstDayOfWeek - 1 + i) % 7 + 1
            val letter = shortWeekdays.getOrNull(calDayIndex)?.take(1)?.uppercase(locale) ?: "D"
            views.setTextViewText(weekdayIds[i], letter)
            views.setTextColor(weekdayIds[i], palette.onBgVariant)
            views.setTextViewTextSize(weekdayIds[i], TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(if (expandedMonth) 12f else 10f))
        }

        // Calendar grid cells
        val monthCal = day.clone() as Calendar
        monthCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeekInMonth = monthCal.get(Calendar.DAY_OF_WEEK)
        val offset = (firstDayOfWeekInMonth - firstDayOfWeek + 7) % 7
        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val totalWeeks = Math.ceil((offset + daysInMonth) / 7.0).toInt().coerceIn(4, 6)

        // Show/hide row 6
        if (totalWeeks < 6) {
            views.setViewVisibility(R.id.widget_month_row_6, View.GONE)
        } else {
            views.setViewVisibility(R.id.widget_month_row_6, View.VISIBLE)
        }

        val todayCal = Calendar.getInstance()
        val isCurrentMonth = day.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                day.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH)

        val cellIds = intArrayOf(
            R.id.cell_1, R.id.cell_2, R.id.cell_3, R.id.cell_4, R.id.cell_5, R.id.cell_6, R.id.cell_7,
            R.id.cell_8, R.id.cell_9, R.id.cell_10, R.id.cell_11, R.id.cell_12, R.id.cell_13, R.id.cell_14,
            R.id.cell_15, R.id.cell_16, R.id.cell_17, R.id.cell_18, R.id.cell_19, R.id.cell_20, R.id.cell_21,
            R.id.cell_22, R.id.cell_23, R.id.cell_24, R.id.cell_25, R.id.cell_26, R.id.cell_27, R.id.cell_28,
            R.id.cell_29, R.id.cell_30, R.id.cell_31, R.id.cell_32, R.id.cell_33, R.id.cell_34, R.id.cell_35,
            R.id.cell_36, R.id.cell_37, R.id.cell_38, R.id.cell_39, R.id.cell_40, R.id.cell_41, R.id.cell_42
        )

        val activeEvents = if (customization.hidePastEvents) events.filter { it.isUpcomingOrCurrent() } else events
        // Reserve space for full cards and readable rows independently of the selected day.
        val minimumEventCardHeight = if (compactTiles) 24f else 27f
        val gridBudget = metrics.box.heightDp - 2 * metrics.agendaInsetDp - 47f
        val expandedCapacity = ((gridBudget - totalWeeks * 20f - 6f) / (minimumEventCardHeight + 3f))
            .toInt().coerceIn(0, 3)
        val eventSectionHeight = if (expandedCapacity > 0) {
            6f + (minimumEventCardHeight + 3f) * expandedCapacity
        } else {
            26f
        }
        val expandedCellHeight = ((gridBudget - eventSectionHeight) / totalWeeks - 2f).coerceIn(16f, 56f)
        val visibleEventCount = if (hasPermission) minOf(activeEvents.size, expandedCapacity) else 0
        val eventCardHeight = if (!compactTiles && visibleEventCount > 0) {
            val remainingHeight = gridBudget - totalWeeks * (expandedCellHeight + 2f) - 6f
            (remainingHeight / visibleEventCount - 3f).coerceAtLeast(minimumEventCardHeight)
        } else {
            minimumEventCardHeight
        }
        views.setViewVisibility(R.id.widget_month_expanded_spacer, if (expandedMonth) View.VISIBLE else View.GONE)
        for (i in 0..41) {
            val cellId = cellIds[i]
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                views.setViewLayoutHeight(cellId, if (expandedMonth) expandedCellHeight else 16f, TypedValue.COMPLEX_UNIT_DIP)
            }
            if (i < offset || i >= offset + daysInMonth) {
                views.setTextViewText(cellId, "")
            } else {
                val dNum = i - offset + 1
                views.setTextViewText(cellId, dNum.toString())
                views.setTextViewTextSize(cellId, TypedValue.COMPLEX_UNIT_DIP, if (expandedMonth) minOf(metrics.responsiveTextDp(if (compactTiles) 17f else 16f), expandedCellHeight * .72f) else metrics.responsiveTextDp(10.5f))

                val isSelectedDay = dNum == day.get(Calendar.DAY_OF_MONTH) &&
                        monthCal.get(Calendar.MONTH) == day.get(Calendar.MONTH) &&
                        monthCal.get(Calendar.YEAR) == day.get(Calendar.YEAR)

                if (isSelectedDay) {
                    views.setTextColor(cellId, palette.accentText)
                } else if (eventDaysForMonth.contains(dNum)) {
                    views.setTextColor(cellId, palette.onBg)
                } else {
                    views.setTextColor(cellId, palette.onBgVariant)
                }

                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val selectIntent = android.content.Intent(context, ExpressMonthWidgetProvider::class.java).apply {
                        action = SelectedDateStore.ACTION_SELECT_DATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        putExtra(SelectedDateStore.EXTRA_YEAR, monthCal.get(Calendar.YEAR))
                        putExtra(SelectedDateStore.EXTRA_MONTH, monthCal.get(Calendar.MONTH))
                        putExtra(SelectedDateStore.EXTRA_DAY, dNum)
                    }
                    val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    } else {
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT
                    }
                    val pi = android.app.PendingIntent.getBroadcast(
                        context,
                        (appWidgetId * 1000) + i,
                        selectIntent,
                        flags
                    )
                    views.setOnClickPendingIntent(cellId, pi)
                }
            }
        }

        val cards = intArrayOf(R.id.widget_month_event_card_1, R.id.widget_month_event_card_2, R.id.widget_month_event_card_3, R.id.widget_month_event_card_4, R.id.widget_month_event_card_5, R.id.widget_month_event_card_6, R.id.widget_month_event_card_7, R.id.widget_month_event_card_8)
        val backgrounds = intArrayOf(R.id.widget_month_event_card_1_bg, R.id.widget_month_event_card_2_bg, R.id.widget_month_event_card_3_bg, R.id.widget_month_event_card_4_bg, R.id.widget_month_event_card_5_bg, R.id.widget_month_event_card_6_bg, R.id.widget_month_event_card_7_bg, R.id.widget_month_event_card_8_bg)
        val dots = intArrayOf(R.id.widget_month_event_1_dot, R.id.widget_month_event_2_dot, R.id.widget_month_event_3_dot, R.id.widget_month_event_4_dot, R.id.widget_month_event_5_dot, R.id.widget_month_event_6_dot, R.id.widget_month_event_7_dot, R.id.widget_month_event_8_dot)
        val teasers = intArrayOf(R.id.widget_month_event_1_teaser, R.id.widget_month_event_2_teaser, R.id.widget_month_event_3_teaser, R.id.widget_month_event_4_teaser, R.id.widget_month_event_5_teaser, R.id.widget_month_event_6_teaser, R.id.widget_month_event_7_teaser, R.id.widget_month_event_8_teaser)
        val hours = intArrayOf(R.id.widget_month_hours_1, R.id.widget_month_hours_2, R.id.widget_month_hours_3, R.id.widget_month_hours_4, R.id.widget_month_hours_5, R.id.widget_month_hours_6, R.id.widget_month_hours_7, R.id.widget_month_hours_8)
        val calendarHeight = maxOf(5, totalWeeks) * 18f + 40f + metrics.agendaInsetDp * 2
        val capacity = ((metrics.box.heightDp - calendarHeight) / 27f).toInt().coerceIn(0, cards.size)
        val visible = if (hasPermission && !compactMonth) activeEvents.take(if (expandedMonth) expandedCapacity else capacity) else emptyList()

        views.setViewVisibility(R.id.widget_month_events_container, if (visible.isEmpty()) View.GONE else View.VISIBLE)
        views.setViewVisibility(R.id.widget_month_empty_text, if (!compactMonth && visible.isEmpty()) View.VISIBLE else View.GONE)
        views.setTextViewText(R.id.widget_month_empty_text, context.getString(
            if (hasPermission) R.string.no_events_day else R.string.permission_required_widget))
        views.setTextColor(R.id.widget_month_empty_text, palette.onBgVariant)
        for (index in cards.indices) {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                views.setViewLayoutHeight(cards[index], if (expandedMonth) eventCardHeight else 24f, TypedValue.COMPLEX_UNIT_DIP)
            }
            val event = visible.getOrNull(index)
            views.setViewVisibility(cards[index], if (event != null) View.VISIBLE else View.GONE)
            if (event != null) {
                views.setOnClickPendingIntent(cards[index], WidgetHelper.getLaunchAppPendingIntent(context, "agenda", event.id, event.startMillis))
                views.setInt(backgrounds[index], "setColorFilter", eventSurface(palette, event))
                views.setInt(dots[index], "setColorFilter", WidgetPalette.ensureContrast(event.calendarColor, palette.bgRgb, 3.0))
                views.setTextViewText(teasers[index], event.title)
                views.setTextViewText(hours[index], event.formatTimeRange(context))
                views.setTextColor(hours[index], palette.onCard)
                views.setTextViewTextSize(hours[index], TypedValue.COMPLEX_UNIT_DIP, minOf(metrics.responsiveTextDp(10f), 12f))
                views.setTextColor(teasers[index], palette.onCard)
                views.setTextViewTextSize(teasers[index], TypedValue.COMPLEX_UNIT_DIP, minOf(metrics.responsiveTextDp(WidgetTokens.TEXT_BODY), 14f))
            }
        }


        return views
    }

    // --- 5. EXPRESS SPLIT DAY ---
    private fun buildSplit(
        context: Context,
        events: List<CalendarEvent>,
        day: Calendar,
        customization: WidgetCustomization,
        palette: WidgetPalette,
        metrics: WidgetMetrics,
        hasPermission: Boolean
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_split_layout)
        applyCommonBackground(views, palette)
        applyAgendaInset(views, R.id.widget_split_content_root, metrics)

        val pendingIntent = WidgetHelper.getLaunchAppPendingIntent(context)
        views.setOnClickPendingIntent(R.id.widget_split_root, pendingIntent)

        val locale = Locale.getDefault()
        views.setTextViewText(R.id.widget_split_header, SimpleDateFormat("EEEE", locale).format(day.time).uppercase(locale))
        views.setTextViewText(R.id.widget_split_day, SimpleDateFormat("d", locale).format(day.time))
        views.setTextViewText(R.id.widget_split_month, SimpleDateFormat("MMMM", locale).format(day.time))
        views.setTextColor(R.id.widget_split_header, palette.accentText)
        views.setTextColor(R.id.widget_split_day, palette.onBg)
        views.setTextColor(R.id.widget_split_month, palette.onBgVariant)
        views.setTextViewTextSize(R.id.widget_split_header, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(11f))
        views.setTextViewTextSize(R.id.widget_split_day, TypedValue.COMPLEX_UNIT_DIP, minOf(metrics.responsiveTextDp(36f), 42f, metrics.box.heightDp * .23f))
        views.setTextViewTextSize(R.id.widget_split_month, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(14f))

        val showWeather = WeatherRepository.showInWidgets(context)
        views.setViewVisibility(R.id.widget_split_weather_icon, View.GONE)
        views.setViewVisibility(R.id.widget_split_weather_temp, if (showWeather) View.VISIBLE else View.GONE)
        if (showWeather) {
            val weather = WeatherRepository.getWeather(context)
            views.setViewVisibility(R.id.widget_split_weather_icon, if (weather.isAvailable) View.VISIBLE else View.GONE)
            if (weather.isAvailable) {
                val icon = weatherIconResource(weather.condition)
                views.setImageViewResource(R.id.widget_split_weather_icon, icon)
            }
            views.setTextViewText(R.id.widget_split_weather_temp, if (weather.isAvailable) "${weather.tempC}°" else "—")
            views.setTextColor(R.id.widget_split_weather_temp, palette.onBg)
            views.setTextViewTextSize(R.id.widget_split_weather_temp, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(20f))
        }

        val noon = (day.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val now = System.currentTimeMillis()
        val eligible = if (!hasPermission) emptyList() else if (customization.hidePastEvents) {
            events.filter { it.isUpcomingOrCurrent(now) }
        } else events

        val cards = intArrayOf(R.id.widget_split_morning_col, R.id.widget_split_afternoon_col)
        val backgrounds = intArrayOf(R.id.widget_split_morning_bg, R.id.widget_split_afternoon_bg)
        val bars = intArrayOf(R.id.widget_split_morning_bar, R.id.widget_split_afternoon_bar)
        val labels = intArrayOf(R.id.widget_split_morning_title, R.id.widget_split_afternoon_title)
        val titles = intArrayOf(R.id.widget_split_morning_event, R.id.widget_split_afternoon_event)
        val times = intArrayOf(R.id.widget_split_morning_time, R.id.widget_split_afternoon_time)
        val emptyMessages = intArrayOf(R.string.no_morning_events, R.string.no_afternoon_events)
        val periods = eligible.sortedBy { it.startMillis }.partition { it.startMillis < noon }
        val extraContainers = intArrayOf(R.id.widget_split_morning_extra, R.id.widget_split_afternoon_extra)
        // Reserve the date, section label, first card, and outer padding before adding complete rows.
        val extraCapacity = ((metrics.box.heightDp - 2 * metrics.agendaInsetDp - 149f) / 54f).toInt().coerceIn(0, 6)
        listOf(periods.first, periods.second).forEachIndexed { index, period ->
            val event = period.firstOrNull()
            val surface = if (event != null) eventSurface(palette, event) else palette.card
            val accent = if (event != null) WidgetPalette.ensureContrast(event.calendarColor, surface, 4.5) else palette.accentText
            views.setInt(backgrounds[index], "setColorFilter", surface)
            views.setInt(bars[index], "setColorFilter", accent)
            views.setTextColor(labels[index], palette.onBgVariant)
            views.setTextViewTextSize(labels[index], TypedValue.COMPLEX_UNIT_DIP, minOf(metrics.responsiveTextDp(9f), 12f))
            views.setTextViewText(titles[index], event?.title ?: context.getString(
                if (hasPermission) emptyMessages[index] else R.string.permission_required_widget))
            views.setTextColor(titles[index], if (event == null) palette.onBgVariant else palette.onCard)
            views.setTextViewTextSize(titles[index], TypedValue.COMPLEX_UNIT_DIP, minOf(metrics.responsiveTextDp(13f), 16f))
            val location = if (event != null && customization.showLocation && !event.location.isNullOrBlank()) " · ${event.location}" else ""
            views.setTextViewText(times[index], event?.formatTimeRange(context).orEmpty() + location)
            views.setTextColor(times[index], accent)
            views.setTextViewTextSize(times[index], TypedValue.COMPLEX_UNIT_DIP, minOf(metrics.responsiveTextDp(11f), 13f))
            views.setOnClickPendingIntent(cards[index], WidgetHelper.getLaunchAppPendingIntent(context, "agenda", event?.id, event?.startMillis))

            val container = extraContainers[index]
            val extraEvents = period.drop(1).take(extraCapacity)
            views.removeAllViews(container)
            views.setViewVisibility(container, if (extraEvents.isEmpty()) View.GONE else View.VISIBLE)
            for (extra in extraEvents) {
                val extraSurface = eventSurface(palette, extra)
                val extraAccent = WidgetPalette.ensureContrast(extra.calendarColor, extraSurface, 4.5)
                val row = RemoteViews(context.packageName, R.layout.widget_event_row)
                row.setInt(R.id.event_card_bg, "setColorFilter", extraSurface)
                row.setInt(R.id.event_accent_bar, "setColorFilter", extraAccent)
                row.setTextViewText(R.id.event_title, extra.title)
                row.setTextColor(R.id.event_title, palette.onCard)
                row.setTextViewTextSize(R.id.event_title, TypedValue.COMPLEX_UNIT_DIP, minOf(metrics.responsiveTextDp(13f), 16f))
                val extraLocation = if (customization.showLocation && !extra.location.isNullOrBlank()) " · ${extra.location}" else ""
                row.setTextViewText(R.id.event_subtitle, extra.formatTimeRange(context) + extraLocation)
                row.setTextColor(R.id.event_subtitle, extraAccent)
                row.setTextViewTextSize(R.id.event_subtitle, TypedValue.COMPLEX_UNIT_DIP, minOf(metrics.responsiveTextDp(11f), 13f))
                row.setOnClickPendingIntent(R.id.event_row_root,
                    WidgetHelper.getLaunchAppPendingIntent(context, "agenda", extra.id, extra.startMillis))
                views.addView(container, row)
            }
        }


        return views
    }
}
