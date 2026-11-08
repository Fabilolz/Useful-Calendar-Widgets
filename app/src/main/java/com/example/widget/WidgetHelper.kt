package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.model.WidgetType

object WidgetHelper {

    fun previewHostScale(context: Context, type: WidgetType): Float {
        val provider = when (type) {
            WidgetType.EXPRESS_AGENDA -> ExpressAgendaWidgetProvider::class.java
            WidgetType.EXPRESS_UP_NEXT -> ExpressGlanceWidgetProvider::class.java
            WidgetType.EXPRESS_MONTH -> ExpressMonthWidgetProvider::class.java
            WidgetType.EXPRESS_WEEK_STRIP -> ExpressWeekWidgetProvider::class.java
            WidgetType.EXPRESS_SPLIT_DAY -> ExpressSplitWidgetProvider::class.java
        }
        val manager = AppWidgetManager.getInstance(context)
        val id = manager.getAppWidgetIds(ComponentName(context, provider)).minOrNull() ?: return 1f
        val ratio = manager.getAppWidgetOptions(id).getFloat("hsResizeRatio", 1f)
        return if (ratio in 0.5f..1f) ratio else 1f
    }

    fun previewBox(context: Context, type: WidgetType, fallback: WidgetBox): WidgetBox {
        val provider = when (type) {
            WidgetType.EXPRESS_AGENDA -> ExpressAgendaWidgetProvider::class.java
            WidgetType.EXPRESS_UP_NEXT -> ExpressGlanceWidgetProvider::class.java
            WidgetType.EXPRESS_MONTH -> ExpressMonthWidgetProvider::class.java
            WidgetType.EXPRESS_WEEK_STRIP -> ExpressWeekWidgetProvider::class.java
            WidgetType.EXPRESS_SPLIT_DAY -> ExpressSplitWidgetProvider::class.java
        }
        val manager = AppWidgetManager.getInstance(context)
        // The first ID gives a stable reference if several copies have different sizes.
        val id = manager.getAppWidgetIds(ComponentName(context, provider)).minOrNull() ?: return fallback
        return widgetBox(context, manager, id, fallback)
    }

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val providerClasses = listOf(
            ExpressAgendaWidgetProvider::class.java,
            ExpressGlanceWidgetProvider::class.java,
            ExpressMonthWidgetProvider::class.java,
            ExpressWeekWidgetProvider::class.java,
            ExpressSplitWidgetProvider::class.java
        )

        for (cls in providerClasses) {
            val component = ComponentName(context, cls)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(context, cls).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    /**
     * Request launcher pinning of widget directly to Home Screen
     */
    fun pinWidgetToHomeScreen(context: Context, widgetType: WidgetType, preview: android.widget.RemoteViews, box: WidgetBox): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            if (!appWidgetManager.isRequestPinAppWidgetSupported) {
                return false
            }

            val providerClass = when (widgetType) {
                WidgetType.EXPRESS_AGENDA -> ExpressAgendaWidgetProvider::class.java
                WidgetType.EXPRESS_UP_NEXT -> ExpressGlanceWidgetProvider::class.java
                WidgetType.EXPRESS_MONTH -> ExpressMonthWidgetProvider::class.java
                WidgetType.EXPRESS_WEEK_STRIP -> ExpressWeekWidgetProvider::class.java
                WidgetType.EXPRESS_SPLIT_DAY -> ExpressSplitWidgetProvider::class.java
            }

            val myProvider = ComponentName(context, providerClass)
            val scaledPreview = runCatching {
                val density = context.resources.displayMetrics.density
                val width = (box.widthDp * density).toInt().coerceAtLeast(1)
                val height = (box.heightDp * density).toInt().coerceAtLeast(1)
                val view = preview.apply(context, android.widget.FrameLayout(context))
                view.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(width, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(height, android.view.View.MeasureSpec.EXACTLY)
                )
                view.layout(0, 0, width, height)
                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                view.draw(android.graphics.Canvas(bitmap))
                android.widget.RemoteViews(context.packageName, com.example.R.layout.widget_pin_preview).apply {
                    setImageViewBitmap(com.example.R.id.widget_pin_image, bitmap)
                }
            }.getOrDefault(preview)
            val extras = android.os.Bundle().apply {
                putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, scaledPreview)
            }
            return appWidgetManager.requestPinAppWidget(myProvider, extras, null)
        }
        return false
    }

    fun getLaunchAppPendingIntent(
        context: Context,
        targetTab: String? = null,
        eventId: Long? = null,
        startMillis: Long? = null
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (targetTab != null) putExtra("target_tab", targetTab)
            if (eventId != null) putExtra("event_id", eventId)
            if (startMillis != null) putExtra("start_millis", startMillis)
            if (eventId != null && startMillis != null) {
                data = android.net.Uri.parse("expresscal://agenda/event/$eventId/$startMillis")
            }
        }
        val requestCode = (eventId?.toInt() ?: 0) * 31 + (targetTab?.hashCode() ?: 0)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }
}
