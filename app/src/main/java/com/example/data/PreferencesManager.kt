package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.WidgetCustomization
import com.example.model.WidgetType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "express_calendar_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val SELECTED_CALENDAR_IDS = stringSetPreferencesKey("selected_calendar_ids")
        val WIDGET_DARK_THEME = booleanPreferencesKey("widget_dark_theme")
        val WIDGET_CORNER_RADIUS = intPreferencesKey("widget_corner_radius")
        val WIDGET_SHOW_LOCATION = booleanPreferencesKey("widget_show_location")
        val WIDGET_HIDE_PAST_EVENTS = booleanPreferencesKey("widget_hide_past_events")
        val WIDGET_ACCENT_INDEX = intPreferencesKey("widget_accent_index")
        val WIDGET_FONT_SCALE = floatPreferencesKey("widget_font_scale")
        val WIDGET_BG_OPACITY = floatPreferencesKey("widget_bg_opacity")
        val WIDGET_CUSTOM_BG_HEX = stringPreferencesKey("widget_custom_bg_hex")
    }

    val selectedCalendarIdsFlow: Flow<Set<Long>?> = context.dataStore.data.map { prefs ->
        val stringSet = prefs[SELECTED_CALENDAR_IDS]
        stringSet?.mapNotNull { it.toLongOrNull() }?.toSet()
    }

    val widgetCustomizationFlow: Flow<WidgetCustomization> = context.dataStore.data.map { prefs ->
        WidgetCustomization(
            darkTheme = prefs[WIDGET_DARK_THEME] ?: true,
            cornerRadiusDp = prefs[WIDGET_CORNER_RADIUS] ?: 26,
            showLocation = prefs[WIDGET_SHOW_LOCATION] ?: true,
            hidePastEvents = prefs[WIDGET_HIDE_PAST_EVENTS] ?: true,
            fontScale = prefs[WIDGET_FONT_SCALE] ?: 1.0f,
            backgroundOpacity = prefs[WIDGET_BG_OPACITY] ?: 1.0f,
            customBgHex = prefs[WIDGET_CUSTOM_BG_HEX],
            accentColorIndex = prefs[WIDGET_ACCENT_INDEX] ?: 0
        )
    }

    val allCustomizationsFlow: Flow<Map<WidgetType, WidgetCustomization>> = context.dataStore.data.map { prefs ->
        val globalDarkTheme = prefs[WIDGET_DARK_THEME] ?: true
        val globalCornerRadius = prefs[WIDGET_CORNER_RADIUS] ?: 26
        val globalShowLocation = prefs[WIDGET_SHOW_LOCATION] ?: true
        val globalHidePastEvents = prefs[WIDGET_HIDE_PAST_EVENTS] ?: true
        val globalFontScale = prefs[WIDGET_FONT_SCALE] ?: 1.0f
        val globalBgOpacity = prefs[WIDGET_BG_OPACITY] ?: 1.0f
        val globalCustomBgHex = prefs[WIDGET_CUSTOM_BG_HEX]
        val globalAccentIndex = prefs[WIDGET_ACCENT_INDEX] ?: 0

        WidgetType.values().associateWith { type ->
            val prefix = "widget_${type.id}_"
            val themePrefix = "widget_${WidgetType.EXPRESS_AGENDA.id}_"
            val darkKey = booleanPreferencesKey("${themePrefix}dark_theme")
            val radiusKey = intPreferencesKey("${themePrefix}corner_radius")
            val locKey = booleanPreferencesKey("${prefix}show_location")
            val hideKey = booleanPreferencesKey("${prefix}hide_past_events")
            val fontKey = floatPreferencesKey("${themePrefix}font_scale")
            val opacityKey = floatPreferencesKey("${themePrefix}bg_opacity")
            val bgKey = stringPreferencesKey("${themePrefix}custom_bg_hex")
            val accentKey = intPreferencesKey("${themePrefix}accent_index")

            WidgetCustomization(
                darkTheme = prefs[darkKey] ?: globalDarkTheme,
                cornerRadiusDp = prefs[radiusKey] ?: globalCornerRadius,
                showLocation = prefs[locKey] ?: globalShowLocation,
                hidePastEvents = prefs[hideKey] ?: globalHidePastEvents,
                fontScale = prefs[fontKey] ?: globalFontScale,
                backgroundOpacity = prefs[opacityKey] ?: globalBgOpacity,
                customBgHex = if (prefs[darkKey] != null) prefs[bgKey] else globalCustomBgHex,
                accentColorIndex = prefs[accentKey] ?: globalAccentIndex
            )
        }
    }

    suspend fun getCustomization(): WidgetCustomization {
        return widgetCustomizationFlow.first()
    }

    suspend fun getCustomizationForType(type: WidgetType): WidgetCustomization {
        val map = allCustomizationsFlow.first()
        return map[type] ?: getCustomization()
    }

    suspend fun saveSelectedCalendarIds(ids: Set<Long>) {
        context.dataStore.edit { prefs ->
            prefs[SELECTED_CALENDAR_IDS] = ids.map { it.toString() }.toSet()
        }
    }

    suspend fun updateWidgetCustomization(customization: WidgetCustomization) {
        context.dataStore.edit { prefs ->
            prefs[WIDGET_DARK_THEME] = customization.darkTheme
            prefs[WIDGET_CORNER_RADIUS] = customization.cornerRadiusDp
            prefs[WIDGET_SHOW_LOCATION] = customization.showLocation
            prefs[WIDGET_HIDE_PAST_EVENTS] = customization.hidePastEvents
            prefs[WIDGET_FONT_SCALE] = customization.fontScale
            prefs[WIDGET_BG_OPACITY] = customization.backgroundOpacity
            if (customization.customBgHex != null) {
                prefs[WIDGET_CUSTOM_BG_HEX] = customization.customBgHex
            } else {
                prefs.remove(WIDGET_CUSTOM_BG_HEX)
            }
            prefs[WIDGET_ACCENT_INDEX] = customization.accentColorIndex
        }
    }

    suspend fun updateWidgetCustomizationForType(type: WidgetType, customization: WidgetCustomization) {
        context.dataStore.edit { prefs ->
            val prefix = "widget_${type.id}_"
            val themePrefix = "widget_${WidgetType.EXPRESS_AGENDA.id}_"
            prefs[booleanPreferencesKey("${themePrefix}dark_theme")] = customization.darkTheme
            prefs[intPreferencesKey("${themePrefix}corner_radius")] = customization.cornerRadiusDp
            prefs[booleanPreferencesKey("${prefix}show_location")] = customization.showLocation
            prefs[booleanPreferencesKey("${prefix}hide_past_events")] = customization.hidePastEvents
            prefs[floatPreferencesKey("${themePrefix}font_scale")] = customization.fontScale
            prefs[floatPreferencesKey("${themePrefix}bg_opacity")] = customization.backgroundOpacity
            if (customization.customBgHex != null) {
                prefs[stringPreferencesKey("${themePrefix}custom_bg_hex")] = customization.customBgHex
            } else {
                prefs.remove(stringPreferencesKey("${themePrefix}custom_bg_hex"))
            }
            prefs[intPreferencesKey("${themePrefix}accent_index")] = customization.accentColorIndex
        }
    }
}
