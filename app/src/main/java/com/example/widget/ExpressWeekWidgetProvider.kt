package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.example.data.CalendarRepository
import com.example.data.PreferencesManager
import com.example.model.WidgetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpressWeekWidgetProvider : AppWidgetProvider() {

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: android.content.Intent) {
        super.onReceive(context, intent)
        if (intent.action == SelectedDateStore.ACTION_SELECT_DATE) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            val year = intent.getIntExtra(SelectedDateStore.EXTRA_YEAR, -1)
            val month = intent.getIntExtra(SelectedDateStore.EXTRA_MONTH, -1)
            val day = intent.getIntExtra(SelectedDateStore.EXTRA_DAY, -1)

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && year != -1 && month != -1 && day != -1) {
                SelectedDateStore.setSelectedDate(appWidgetId, year, month, day)
                val appWidgetManager = AppWidgetManager.getInstance(context)
                onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = CalendarRepository()
                val prefs = PreferencesManager(context)
                val customization = prefs.getCustomizationForType(WidgetType.EXPRESS_WEEK_STRIP)
                val hasPerm = repository.hasCalendarPermission(context)
                val selectedCalendarIds = prefs.selectedCalendarIdsFlow.first()

                for (appWidgetId in appWidgetIds) {
                    val targetDay = SelectedDateStore.getSelectedDate(appWidgetId)
                    val events = repository.getEventsForDay(
                        context = context,
                        selectedCalendarIds = selectedCalendarIds,
                        targetCalendar = targetDay,
                        hidePastEvents = false
                    )

                    val box = widgetBox(context, appWidgetManager, appWidgetId, WidgetBox(300f, 235f))
                    val views = WidgetRenderer.build(
                        context = context,
                        type = WidgetType.EXPRESS_WEEK_STRIP,
                        events = events,
                        day = targetDay,
                        customization = customization,
                        box = box,
                        hasPermission = hasPerm,
                        appWidgetId = appWidgetId
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                android.util.Log.e("ExpressWeekWidgetProvider", "Widget update failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}
