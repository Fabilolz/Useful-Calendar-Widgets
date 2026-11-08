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

class ExpressSplitWidgetProvider : AppWidgetProvider() {

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
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
                val today = Calendar.getInstance()
                val prefs = PreferencesManager(context)
                val customization = prefs.getCustomizationForType(WidgetType.EXPRESS_SPLIT_DAY)
                val hasPerm = repository.hasCalendarPermission(context)
                val selectedCalendarIds = prefs.selectedCalendarIdsFlow.first()

                val events = repository.getEventsForDay(
                    context = context,
                    selectedCalendarIds = selectedCalendarIds,
                    targetCalendar = today,
                    hidePastEvents = false
                )

                for (appWidgetId in appWidgetIds) {
                    val box = widgetBox(context, appWidgetManager, appWidgetId, WidgetBox(220f, 160f))
                    val views = WidgetRenderer.build(
                        context = context,
                        type = WidgetType.EXPRESS_SPLIT_DAY,
                        events = events,
                        day = today,
                        customization = customization,
                        box = box,
                        hasPermission = hasPerm
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                android.util.Log.e("ExpressSplitWidgetProvider", "Widget update failed", e)
            } finally {
                pending.finish()
            }
        }
    }
}
