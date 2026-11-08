package com.example.widget

import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe store for per-widget selected dates.
 * Enables interactive date picking in Week and Month widgets.
 */
object SelectedDateStore {
    const val ACTION_SELECT_DATE = "com.example.widget.ACTION_SELECT_DATE"
    const val EXTRA_YEAR = "extra_year"
    const val EXTRA_MONTH = "extra_month"
    const val EXTRA_DAY = "extra_day"

    private val widgetDateMap = ConcurrentHashMap<Int, Calendar>()

    fun getSelectedDate(appWidgetId: Int): Calendar {
        return widgetDateMap[appWidgetId]?.clone() as? Calendar ?: Calendar.getInstance()
    }

    fun setSelectedDate(appWidgetId: Int, year: Int, month: Int, dayOfMonth: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        widgetDateMap[appWidgetId] = cal
    }
}
