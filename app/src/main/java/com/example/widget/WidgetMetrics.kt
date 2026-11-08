package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration

data class WidgetBox(val widthDp: Float, val heightDp: Float)

fun widgetBox(context: Context, mgr: AppWidgetManager, id: Int, fallback: WidgetBox): WidgetBox {
    val o = mgr.getAppWidgetOptions(id) ?: return fallback
    val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val w = o.getInt(
        if (landscape) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
        else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0
    )
    val h = o.getInt(
        if (landscape) AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
        else AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0
    )
    return if (w > 0 && h > 0) WidgetBox(w.toFloat(), h.toFloat()) else fallback
}

class WidgetMetrics(
    val box: WidgetBox,
    private val ref: WidgetBox,
    private val density: Float,
    systemFontScale: Float,
    private val userFontScale: Float = 1.0f,
    private val fontToDp: ((Float) -> Float)? = null
) {
    val s: Float = minOf(box.widthDp / ref.widthDp, box.heightDp / ref.heightDp).coerceIn(0.85f, 1.35f)
    val agendaInsetDp: Float get() = (box.widthDp * .085f).coerceIn(12f, 18f)
    val agendaInsetPx: Int get() = (agendaInsetDp * density).toInt()
    private val sys: Float = systemFontScale.coerceIn(1.0f, 1.15f)

    fun dp(v: Float): Float = v * s
    fun px(v: Float): Int = (v * s * density).toInt()
    fun textDp(base: Float): Float = maxOf(base * s * userFontScale * sys, 10f)

    // Resolve system font scaling once, using Android's density/font conversion (including nonlinear scaling).
    fun responsiveTextDp(base: Float): Float = maxOf(fontToDp?.invoke(base * s * userFontScale)
        ?: (base * s * userFontScale * sys), 11f)

    companion object {
        fun defaultFor(
            context: Context,
            box: WidgetBox,
            ref: WidgetBox,
            userFontScale: Float = 1.0f
        ): WidgetMetrics {
            val dm = context.resources.displayMetrics
            val fontScale = context.resources.configuration.fontScale
            return WidgetMetrics(
                box = box,
                ref = ref,
                density = dm.density,
                systemFontScale = fontScale,
                userFontScale = userFontScale,
                fontToDp = { value -> android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_SP, value, dm) / dm.density }
            )
        }
    }
}
