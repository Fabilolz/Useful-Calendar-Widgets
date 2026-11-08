package com.example.widget

import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import com.example.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Measure the actual localized text before choosing how the launcher lays it out. */
internal object AgendaAdaptiveLayout {
    private val cards = intArrayOf(R.id.widget_event_1, R.id.widget_event_2, R.id.widget_event_3)
    private val contents = intArrayOf(R.id.widget_event_1_content, R.id.widget_event_2_content, R.id.widget_event_3_content)
    private val titles = intArrayOf(R.id.widget_event_1_title, R.id.widget_event_2_title, R.id.widget_event_3_title)
    private val locations = intArrayOf(R.id.widget_event_1_location, R.id.widget_event_2_location, R.id.widget_event_3_location)

    fun fit(context: Context, base: RemoteViews, metrics: WidgetMetrics, day: Calendar,
            eventCount: Int, palette: WidgetPalette): RemoteViews {
        val density = context.resources.displayMetrics.density
        val width = (metrics.box.widthDp * density).toInt()
        val height = (metrics.box.heightDp * density).toInt()
        var fallback = base
        val maxCards = if (metrics.box.widthDp <= 230f && metrics.box.heightDp <= 230f) 2 else 3
        for (mode in 0..3) {
            for (count in eventCount.coerceAtMost(maxCards) downTo if (eventCount > 0) 1 else 0) {
                if (mode == 3 && count > 1) continue
                val views = RemoteViews(base)
                val referencePair = android.os.Build.VERSION.SDK_INT >= 31 && count == 2 &&
                    maxCards == 2 && metrics.box.heightDp >= 190f
                val padding = when (mode) { 0 -> metrics.agendaInsetDp; 1 -> 10f; 2 -> 12f; else -> 10f }
                val padPx = (padding * density).toInt()
                val inset = metrics.agendaInsetPx
                views.setViewPadding(R.id.widget_content_root, inset, padPx, inset, padPx)
                if (eventCount == 0 && mode == 0 && maxCards == 2 && metrics.box.heightDp >= 190f) {
                    val bottom = (height * 26f / 330f).toInt()
                    views.setViewPadding(R.id.widget_content_root, padPx, padPx, padPx, bottom)
                }
                val inline = mode >= 2
                views.setViewVisibility(R.id.widget_day_name, if (inline) View.GONE else View.VISIBLE)
                views.setViewVisibility(R.id.widget_date_row, if (inline) View.GONE else View.VISIBLE)
                views.setViewVisibility(R.id.widget_compact_date, if (inline) View.VISIBLE else View.GONE)
                val singleEvent = count == 1 && eventCount == 1 && !inline
                views.setViewVisibility(R.id.widget_day_subtext, if (singleEvent) View.GONE else View.VISIBLE)
                views.setViewVisibility(R.id.widget_month_below, if (singleEvent) View.VISIBLE else View.GONE)
                if (count == 1) views.setInt(R.id.widget_event_1_title, "setMaxLines", 1)
                // Decorative date never grows at the expense of the requested event text size.
                val dateSize = if (mode == 0) minOf(metrics.responsiveTextDp(54f), metrics.box.heightDp * if (eventCount == 0) .22f else .23f) else 28f
                views.setTextViewTextSize(R.id.widget_day_number, TypedValue.COMPLEX_UNIT_DIP, dateSize)
                views.setTextViewText(R.id.widget_compact_date, SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(day.time))
                views.setTextColor(R.id.widget_compact_date, palette.accentText)
                views.setTextViewTextSize(R.id.widget_compact_date, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(12f))
                cards.forEachIndexed { index, id -> views.setViewVisibility(id, if (index < count) View.VISIBLE else View.GONE) }
                if (count >= 2) titles.take(count).forEach { views.setInt(it, "setMaxLines", 1) }
                views.setViewVisibility(R.id.widget_more_events, View.GONE)
                val temperatureSize = when (mode) { 0 -> if (maxCards == 2) 56f else 74f; 1 -> 50f; 2 -> 38f; else -> 28f }
                views.setTextViewTextSize(R.id.widget_empty_weather_temp, TypedValue.COMPLEX_UNIT_DIP, metrics.responsiveTextDp(temperatureSize))
                if (eventCount == 0) {
                    val labelSize = metrics.responsiveTextDp(if (mode == 0) 16f else 13f)
                    val rangeSize = metrics.responsiveTextDp(if (mode == 0) 13f else 11f)
                    views.setTextViewTextSize(R.id.widget_empty_weather_desc, TypedValue.COMPLEX_UNIT_DIP, labelSize)
                    views.setTextViewTextSize(R.id.widget_weather_low, TypedValue.COMPLEX_UNIT_DIP, rangeSize)
                    views.setTextViewTextSize(R.id.widget_weather_high, TypedValue.COMPLEX_UNIT_DIP, rangeSize)
                }
                if (mode == 0 && count == 2 && metrics.box.heightDp <= 230f) {
                    // A compact two-event layout keeps the same title and time sizes and the full date.
                    val outer = metrics.agendaInsetPx
                    val bottom = if (referencePair) (height * 26f / 330f).toInt() else outer
                    views.setViewPadding(R.id.widget_content_root, outer, outer, outer, bottom)
                    val horizontal = (8 * density).toInt()
                    val vertical = (8 * density).toInt()
                    contents.take(2).forEach { views.setViewPadding(it, horizontal, vertical, horizontal, vertical) }
                    views.setTextViewTextSize(R.id.widget_day_number, TypedValue.COMPLEX_UNIT_DIP,
                        minOf(metrics.responsiveTextDp(31f), metrics.box.heightDp * .17f))
                }
                if (mode == 1) {
                    // Keep the full date on compact widgets by reducing card padding first.
                    val cardPadding = (4 * density).toInt()
                    contents.forEach { views.setViewPadding(it, cardPadding, cardPadding, cardPadding, cardPadding) }
                    if (count >= 2) {
                        val outer = (12 * density).toInt()
                        val compactPadding = (4 * density).toInt()
                        views.setViewPadding(R.id.widget_content_root, inset, outer, inset, outer)
                        contents.take(count).forEach { views.setViewPadding(it, cardPadding, compactPadding, cardPadding, compactPadding) }
                        views.setTextViewTextSize(R.id.widget_day_number, TypedValue.COMPLEX_UNIT_DIP, 30f)
                        (titles + locations + intArrayOf(R.id.widget_event_1_time, R.id.widget_event_2_time, R.id.widget_event_3_time)).forEach {
                            views.setTextViewTextSize(it, TypedValue.COMPLEX_UNIT_DIP, 11f)
                        }
                    }
                }
                if (mode == 3) {
                    // Reclaim decoration and padding, retaining event title, location and time.
                    val cardPadding = (4 * density).toInt()
                    contents.forEach { views.setViewPadding(it, cardPadding, cardPadding, cardPadding, cardPadding) }
                    views.setViewVisibility(R.id.widget_more_events, View.GONE)
                }
                if (android.os.Build.VERSION.SDK_INT >= 31) {
                    // The reference tiles each occupy 83 of the widget's 330 units.
                    val cardHeight = if (referencePair && mode == 0) height * 83f / 330f else -2f
                    contents.take(2).forEach { views.setViewLayoutHeight(it, cardHeight, TypedValue.COMPLEX_UNIT_PX) }
                }
                val root = views.apply(context, FrameLayout(context))
                if (count == 2 && metrics.box.heightDp <= 230f) {
                    val times = intArrayOf(R.id.widget_event_1_time, R.id.widget_event_2_time)
                    for (index in 0..1) {
                        val location = root.findViewById<android.widget.TextView>(locations[index])
                        val time = root.findViewById<android.widget.TextView>(times[index])
                        val timeText = time.text.toString()
                        val metadata = listOf(timeText, location.text.toString().takeIf { location.visibility == View.VISIBLE })
                            .filterNotNull().filter { it.isNotBlank() }.joinToString(" · ")
                        val styledMetadata = android.text.SpannableString(metadata).apply {
                            if (metadata.length > timeText.length) setSpan(android.text.style.ForegroundColorSpan(palette.onBgVariant),
                                timeText.length, metadata.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                        views.setTextViewText(times[index], styledMetadata)
                        views.setInt(times[index], "setMaxLines", 1)
                        views.setContentDescription(times[index], metadata)
                        views.setViewVisibility(locations[index], View.GONE)
                        time.text = metadata
                        time.maxLines = 1
                        location.visibility = View.GONE
                    }
                }
                if (count >= 2 && locations.take(count).any { root.findViewById<View>(it).visibility == View.VISIBLE }) {
                    locations.take(count).forEach { id ->
                        if (root.findViewById<View>(id).visibility == View.GONE) {
                            views.setViewVisibility(id, View.INVISIBLE)
                            root.findViewById<View>(id).visibility = View.INVISIBLE
                        }
                    }
                }
                // This pair has a fixed height budget and a weighted spacer below the date.
                if (referencePair && mode == 0) return views
                root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                if (mode == 0 && count == 1 && root.measuredHeight > height - density * 2) {
                    val date = root.findViewById<android.widget.TextView>(R.id.widget_day_number)
                    val overflow = (root.measuredHeight - height + density * 2) / density
                    val currentDateSize = date.textSize / density
                    val fittedDateSize = minOf(currentDateSize, maxOf(35f, currentDateSize - overflow * 1.1f))
                    views.setTextViewTextSize(R.id.widget_day_number, TypedValue.COMPLEX_UNIT_DIP, fittedDateSize)
                    date.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fittedDateSize)
                    val cardPad = (6 * density).toInt()
                    views.setViewPadding(contents[0], cardPad, cardPad, cardPad, cardPad)
                    root.findViewById<View>(contents[0]).setPadding(cardPad, cardPad, cardPad, cardPad)
                    root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                }
                if (eventCount == 0) {
                    val temperature = root.findViewById<android.widget.TextView>(R.id.widget_empty_weather_temp)
                    val textWidth = temperature.paint.measureText(temperature.text.toString())
                    if (textWidth > temperature.measuredWidth && temperature.measuredWidth > 0) {
                        val fittingSize = temperature.textSize * temperature.measuredWidth / textWidth / density
                        views.setTextViewTextSize(R.id.widget_empty_weather_temp, TypedValue.COMPLEX_UNIT_DIP, fittingSize)
                        temperature.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fittingSize)
                        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                    }
                    // Preserve the reference's large date by trimming surplus temperature height first.
                    repeat(3) {
                        if (root.measuredHeight > height - density * 2 && temperature.textSize / density > 28f) {
                            val overflowDp = (root.measuredHeight - height + density * 2) / density
                            val fittingSize = maxOf(28f, temperature.textSize / density - overflowDp * 1.1f)
                            views.setTextViewTextSize(R.id.widget_empty_weather_temp, TypedValue.COMPLEX_UNIT_DIP, fittingSize)
                            temperature.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fittingSize)
                            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                        }
                    }
                }
                // Only at the physical minimum: keep all fields and rebalance typography,
                // rather than dropping location/time or allowing a card to run off-screen.
                if (mode == 3 && root.measuredHeight > height - density * 2) {
                    val textIds = intArrayOf(R.id.widget_compact_date, R.id.widget_event_1_title,
                        R.id.widget_event_1_location, R.id.widget_event_1_time, R.id.widget_empty_weather_desc)
                    repeat(8) {
                        if (root.measuredHeight > height - density * 2) {
                            textIds.forEach { id ->
                                val text = root.findViewById<android.widget.TextView>(id)
                                val size = maxOf(11f, text.textSize / density * .9f)
                                views.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_DIP, size)
                                text.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
                            }
                            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
                        }
                    }
                }
                fallback = views
                if (root.measuredHeight <= height - density * 2) {
                    // Try the reduced-padding full-date layout before dropping the second event.
                    if (mode == 0 && count == 1 && eventCount >= 2 && metrics.box.heightDp <= 230f) continue
                    return views
                }
            }
        }
        return fallback
    }
}
