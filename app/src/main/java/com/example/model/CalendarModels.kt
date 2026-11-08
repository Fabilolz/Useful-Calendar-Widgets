package com.example.model

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Represents a calendar account synced to the device (e.g., Google Calendar, Work, Holidays).
 */
data class CalendarAccount(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val accountType: String,
    val color: Int,
    val isPrimary: Boolean = false,
    val isSelected: Boolean = true
) {
    val isGoogleAccount: Boolean
        get() = accountType.contains("google", ignoreCase = true) || accountName.contains("@gmail.com")
}

/**
 * Represents a calendar event retrieved from device or demo store.
 */
data class CalendarEvent(
    val id: Long,
    val calendarId: Long,
    val calendarName: String,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    val startMillis: Long,
    val endMillis: Long,
    val isAllDay: Boolean = false,
    val color: Int? = null,
    val calendarColor: Int = 0xFFE040FB.toInt()
) {
    /**
     * Resolves end timestamp safely, properly managing events that cross midnight
     * or events that end at 00:00 / have end <= start.
     */
    val effectiveEndMillis: Long
        get() = when {
            isAllDay -> endMillis
            endMillis <= startMillis -> startMillis + 1
            else -> endMillis
        }

    val durationMillis: Long
        get() = effectiveEndMillis - startMillis

    fun getEffectiveColor(): Color {
        return if (color != null && color != 0) {
            Color(color)
        } else if (calendarColor != 0) {
            Color(calendarColor)
        } else {
            Color(0xFFE040FB)
        }
    }

    fun formatTimeRange(context: android.content.Context? = null): String {
        if (isAllDay) {
            return context?.getString(com.example.R.string.all_day) ?: "All Day"
        }
        val tf = if (context != null) {
            android.text.format.DateFormat.getTimeFormat(context)
        } else {
            SimpleDateFormat("h:mm a", Locale.getDefault())
        }
        val startStr = tf.format(Date(startMillis))
        val endStr = tf.format(Date(effectiveEndMillis))
        return "$startStr–$endStr"
    }

    fun isPast(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return effectiveEndMillis < nowMillis
    }

    fun isHappeningNow(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return nowMillis in startMillis..effectiveEndMillis
    }

    fun isUpcoming(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return startMillis > nowMillis
    }

    fun isUpcomingOrCurrent(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return effectiveEndMillis >= nowMillis
    }

    fun isImminent(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val diff = startMillis - nowMillis
        return diff in 0..(15 * 60 * 1000L)
    }

    fun getRelativeTimeText(nowMillis: Long): String = getRelativeTimeText(null, nowMillis)

    fun getRelativeTimeText(context: android.content.Context? = null, nowMillis: Long = System.currentTimeMillis()): String {
        if (isAllDay) {
            return context?.getString(com.example.R.string.all_day)?.lowercase(Locale.getDefault()) ?: "all day"
        }
        if (isHappeningNow(nowMillis)) {
            return context?.getString(com.example.R.string.happening_now)?.lowercase(Locale.getDefault()) ?: "happening now"
        }
        val diff = startMillis - nowMillis
        if (diff > 0) {
            val minutes = (diff + 59_999) / (60 * 1000)
            val hours = minutes / 60
            return when {
                minutes <= 1 -> context?.getString(com.example.R.string.in_minute) ?: "in 1m"
                minutes < 60 -> context?.getString(com.example.R.string.in_minutes, minutes) ?: "in ${minutes}m"
                hours < 24 -> context?.getString(com.example.R.string.in_hours_minutes, hours, minutes % 60) ?: "in ${hours}h ${minutes % 60}m"
                else -> {
                    val days = hours / 24
                    context?.getString(com.example.R.string.in_days, days) ?: "in ${days}d"
                }
            }
        } else {
            return context?.getString(com.example.R.string.ended) ?: "ended"
        }
    }
}

/**
 * Supported widget styles in the gallery & home screen
 */
enum class WidgetType(
    val id: String,
    val title: String,
    val shortTitle: String,
    val subtitle: String,
    val previewSize: String
) {
    EXPRESS_AGENDA(
        id = "express_agenda",
        title = "Agenda giornaliera",
        shortTitle = "Agenda",
        subtitle = "Data e appuntamenti del giorno",
        previewSize = "2 × 2"
    ),
    EXPRESS_UP_NEXT(
        id = "express_up_next",
        title = "Prossimo evento",
        shortTitle = "Prossimo",
        subtitle = "Il prossimo appuntamento e il tempo rimanente",
        previewSize = "3 × 1"
    ),
    EXPRESS_MONTH(
        id = "express_month",
        title = "Calendario mensile",
        shortTitle = "Mese",
        subtitle = "Vista mensile con gli eventi del giorno",
        previewSize = "4 × 3"
    ),
    EXPRESS_WEEK_STRIP(
        id = "express_week_strip",
        title = "Settimana",
        shortTitle = "Settimana",
        subtitle = "Sette giorni e i relativi appuntamenti",
        previewSize = "4 × 3"
    ),
    EXPRESS_SPLIT_DAY(
        id = "express_split_day",
        title = "La giornata",
        shortTitle = "Giornata",
        subtitle = "Eventi divisi tra mattina e pomeriggio",
        previewSize = "3 × 3"
    )
}

/**
 * Preset background color schemes
 */
enum class BackgroundPreset(
    val title: String,
    val hex: String,
    val isDark: Boolean,
    val description: String
) {
    EXPRESSIVE_DARK("Dark Charcoal", "#18181A", true, "Default dark slate"),
    EXPRESSIVE_LIGHT("Pure White", "#FFFFFF", false, "Pure bright white"),
    AMOLED_BLACK("OLED Black", "#000000", true, "Pitch black for OLED"),
    MIDNIGHT_NAVY("Midnight Navy", "#101622", true, "Deep atmospheric navy"),
    WARM_CREAM("Warm Cream", "#F9F7F2", false, "Soft warm paper"),
    DEEP_PLUM("Deep Plum", "#221124", true, "Expressive plum violet")
}

/**
 * Customization settings for the widget previews & home screen
 */
data class WidgetCustomization(
    val darkTheme: Boolean = true,
    val cornerRadiusDp: Int = 26,
    val showLocation: Boolean = true,
    val hidePastEvents: Boolean = true,
    val fontScale: Float = 1.0f,
    val backgroundOpacity: Float = 1.0f, // 0.20f to 1.0f
    val customBgHex: String? = null,
    val accentColorIndex: Int = 0, // 0: Coral/Magenta, 1: Cyan/Blue, 2: Emerald/Teal, 3: Amber/Gold
    val useDynamicEventColors: Boolean = false
) {
    fun isLightBackground(): Boolean {
        if (!darkTheme) return true
        if (customBgHex != null) {
            return try {
                val parsed = android.graphics.Color.parseColor(customBgHex)
                val luminance = (0.299 * android.graphics.Color.red(parsed) +
                        0.587 * android.graphics.Color.green(parsed) +
                        0.114 * android.graphics.Color.blue(parsed)) / 255.0
                luminance > 0.5
            } catch (e: Exception) {
                false
            }
        }
        return false
    }

    fun getResolvedBackgroundColor(): Color {
        val baseColor = when {
            customBgHex != null -> {
                try {
                    Color(android.graphics.Color.parseColor(customBgHex))
                } catch (e: Exception) {
                    if (darkTheme) Color(0xFF18181A) else Color(0xFFFFFFFF)
                }
            }
            !darkTheme -> Color(0xFFFFFFFF)
            else -> Color(0xFF18181A)
        }
        return baseColor.copy(alpha = backgroundOpacity.coerceIn(0.0f, 1.0f))
    }

    fun getResolvedBackgroundColorInt(): Int {
        val color = getResolvedBackgroundColor()
        val a = (color.alpha * 255).toInt().coerceIn(0, 255)
        val r = (color.red * 255).toInt().coerceIn(0, 255)
        val g = (color.green * 255).toInt().coerceIn(0, 255)
        val b = (color.blue * 255).toInt().coerceIn(0, 255)
        return android.graphics.Color.argb(a, r, g, b)
    }

    fun getResolvedBorderColor(): Color {
        return Color.Transparent
    }

    fun getPrimaryTextColor(): Color {
        return if (isLightBackground()) Color(0xFF18191C) else Color(0xFFFFFFFF)
    }

    fun getSecondaryTextColor(): Color {
        return if (isLightBackground()) Color(0xFF5E6573) else Color(0xFFB0BEC5)
    }

    fun getPrimaryAccent(): Color {
        if (accentColorIndex < 0) return Color(accentColorIndex)
        return when (accentColorIndex) {
            1 -> Color(0xFF00E5FF)
            2 -> Color(0xFF00E676)
            3 -> Color(0xFFFFAB00)
            else -> Color(0xFFFF5252) // Default Screenshot Coral
        }
    }

    fun getSecondaryAccent(): Color {
        if (accentColorIndex < 0) return Color(accentColorIndex)
        return when (accentColorIndex) {
            1 -> Color(0xFF2979FF)
            2 -> Color(0xFF1DE9B6)
            3 -> Color(0xFFFF6D00)
            else -> Color(0xFFE040FB) // Default Screenshot Magenta
        }
    }

    fun getEventCardBg(): Color {
        val isLight = isLightBackground()
        return if (!isLight) {
            when (accentColorIndex) {
                1 -> Color(0xFF10283A)
                2 -> Color(0xFF112E24)
                3 -> Color(0xFF38260F)
                else -> Color(0xFF481B43) // Default Plum
            }
        } else {
            when (accentColorIndex) {
                1 -> Color(0xFFE1F5FE)
                2 -> Color(0xFFE8F5E9)
                3 -> Color(0xFFFFF3E0)
                else -> Color(0xFFF6ECF5) // Soft light lavender/plum
            }
        }
    }

    fun getWidgetBackgroundDrawableRes(): Int {
        val hex = customBgHex?.uppercase() ?: if (darkTheme) "#18181A" else "#FFFFFF"
        val isTranslucent = backgroundOpacity < 0.85f

        if (isTranslucent) {
            return if (isLightBackground()) {
                com.example.R.drawable.widget_bg_light_translucent
            } else {
                com.example.R.drawable.widget_bg_dark_translucent
            }
        }

        return when (hex) {
            "#000000" -> com.example.R.drawable.widget_bg_amoled
            "#101622" -> com.example.R.drawable.widget_bg_navy
            "#F9F7F2" -> com.example.R.drawable.widget_bg_cream
            "#221124" -> com.example.R.drawable.widget_bg_plum
            "#FF5252" -> com.example.R.drawable.widget_bg_coral
            "#00E5FF" -> com.example.R.drawable.widget_bg_cyan
            "#00E676" -> com.example.R.drawable.widget_bg_emerald
            "#FFAB00" -> com.example.R.drawable.widget_bg_amber
            "#673AB7" -> com.example.R.drawable.widget_bg_purple
            "#E91E63" -> com.example.R.drawable.widget_bg_pink
            "#FFFFFF" -> com.example.R.drawable.widget_bg_light
            "#18181A" -> com.example.R.drawable.widget_bg_dark
            else -> if (isLightBackground()) com.example.R.drawable.widget_bg_light else com.example.R.drawable.widget_bg_dark
        }
    }

    fun getPrimaryTextColorInt(): Int = if (isLightBackground()) 0xFF18191C.toInt() else 0xFFFFFFFF.toInt()
    fun getSecondaryTextColorInt(): Int = if (isLightBackground()) 0xFF5E6573.toInt() else 0xFFB0BEC5.toInt()

    fun getPrimaryAccentColorInt(): Int {
        if (accentColorIndex < 0) return accentColorIndex
        return when (accentColorIndex) {
            1 -> 0xFF00E5FF.toInt()
            2 -> 0xFF00E676.toInt()
            3 -> 0xFFFFAB00.toInt()
            else -> 0xFFFF5252.toInt()
        }
    }

    fun getSecondaryAccentColorInt(): Int {
        if (accentColorIndex < 0) return accentColorIndex
        return when (accentColorIndex) {
            1 -> 0xFF2979FF.toInt()
            2 -> 0xFF1DE9B6.toInt()
            3 -> 0xFFFF6D00.toInt()
            else -> 0xFFE040FB.toInt()
        }
    }
}
