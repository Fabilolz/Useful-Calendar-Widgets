package com.example.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import com.example.widget.WidgetHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

data class WeatherInfo(val tempC: Int, val condition: String, val iconEmoji: String, val cityName: String? = null, val isAvailable: Boolean = true, val outlook: WeatherOutlook? = null)

data class WeatherOutlook(val high: Int?, val low: Int?)

object WeatherRepository {
    private const val PREFS = "weather_location_v2"
    private const val EXPIRY = 60 * 60 * 1000L
    private val updating = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val unavailable = WeatherInfo(0, "Meteo non disponibile", "🌤️", isAvailable = false)
    private val state = MutableStateFlow(unavailable)
    val weather = state.asStateFlow()

    private fun preferences(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun showInWidgets(context: Context): Boolean = preferences(context).getBoolean("show_in_widgets", true)

    fun setShowInWidgets(context: Context, show: Boolean) {
        preferences(context).edit().putBoolean("show_in_widgets", show).apply()
        WidgetHelper.updateAllWidgets(context)
    }
    fun selectedCity(context: Context): String? = preferences(context).getString("selected_city", null)

    @Suppress("DEPRECATION")
    suspend fun selectCity(context: Context, query: String): String? {
        val app = context.applicationContext
        val address = withContext(Dispatchers.IO) {
            try {
                android.location.Geocoder(app).getFromLocationName(query, 5)
                    ?.firstOrNull { it.hasLatitude() && it.hasLongitude() }
            } catch (_: java.io.IOException) {
                null
            }
        } ?: return null
        val city = listOfNotNull(address.locality ?: address.subAdminArea ?: query, address.adminArea, address.countryName)
            .distinct().joinToString(", ")
        preferences(app).edit().putString("selected_city", city)
            .putString("city_latitude", address.latitude.toString())
            .putString("city_longitude", address.longitude.toString()).remove("timestamp").apply()
        state.value = unavailable
        fetch(app, address.latitude, address.longitude, city)
        WidgetHelper.updateAllWidgets(app)
        return city
    }

    fun useDeviceLocation(context: Context) {
        val prefs = preferences(context)
        if (prefs.contains("selected_city")) {
            prefs.edit().remove("selected_city").remove("timestamp").apply()
            state.value = unavailable
        }
        refreshLocation(context)
        WidgetHelper.updateAllWidgets(context)
    }

    fun getWeather(context: Context): WeatherInfo {
        val app = context.applicationContext
        val city = selectedCity(app)
        if (city == null && ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            state.value = unavailable
            return unavailable
        }
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val timestamp = prefs.getLong("timestamp", 0)
        val result = if (timestamp > 0 && System.currentTimeMillis() - timestamp < EXPIRY) {
            WeatherInfo(prefs.getInt("temp", 0), prefs.getString("condition", "") ?: "", prefs.getString("icon", "") ?: "", city,
                outlook = prefs.getString("outlook", null)?.let { runCatching { parseOutlook(JSONObject(it)) }.getOrNull() })
        } else unavailable
        state.value = result
        if (result == unavailable || !prefs.contains("outlook")) {
            if (city != null) {
                val lat = prefs.getString("city_latitude", null)?.toDoubleOrNull()
                val lon = prefs.getString("city_longitude", null)?.toDoubleOrNull()
                if (lat != null && lon != null) fetch(app, lat, lon, city)
            } else if (prefs.contains("latitude")) {
                fetch(app, java.lang.Float.intBitsToFloat(prefs.getInt("latitude", 0)).toDouble(), java.lang.Float.intBitsToFloat(prefs.getInt("longitude", 0)).toDouble())
            }
        }
        return result
    }

    fun refreshLocation(context: Context) {
        val app = context.applicationContext
        if (selectedCity(app) != null) {
            getWeather(app)
            return
        }
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val manager = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            val location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (Build.VERSION.SDK_INT >= 30 && manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                manager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, CancellationSignal(), ContextCompat.getMainExecutor(app)) { current ->
                    val resolved = current ?: location
                    if (resolved != null) fetch(app, resolved.latitude, resolved.longitude)
                }
            } else if (location != null) {
                fetch(app, location.latitude, location.longitude)
            } else if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                @Suppress("DEPRECATION")
                manager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, object : android.location.LocationListener {
                    override fun onLocationChanged(location: android.location.Location) { fetch(app, location.latitude, location.longitude) }
                    @Deprecated("Deprecated in Android") override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                }, android.os.Looper.getMainLooper())
            }
        } catch (_: SecurityException) { state.value = unavailable }
    }

    private fun fetch(context: Context, lat: Double, lon: Double, city: String? = null) {
        val source = city.orEmpty()
        if (!updating.add(source)) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true" +
                    "&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1"
                val conn = URL(url).openConnection() as HttpURLConnection
                try {
                    conn.connectTimeout = 8000
                    conn.readTimeout = 8000
                    if (conn.responseCode != 200) return@launch
                    val response = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    val current = response.getJSONObject("current_weather")
                    val (condition, icon) = parseWmoCode(current.getInt("weathercode"))
                    val result = WeatherInfo(current.getDouble("temperature").roundToInt(), condition, icon, city,
                        outlook = parseOutlook(response))
                    if (selectedCity(context) != city) return@launch
                    preferences(context).edit().apply {
                        if (city == null) {
                            putInt("latitude", java.lang.Float.floatToIntBits(lat.toFloat()))
                            putInt("longitude", java.lang.Float.floatToIntBits(lon.toFloat()))
                        }
                    }
                        .putInt("temp", result.tempC).putString("condition", condition).putString("icon", icon)
                        .putString("outlook", response.toString()).putLong("timestamp", System.currentTimeMillis()).apply()
                    state.value = result
                    WidgetHelper.updateAllWidgets(context)
                } finally { conn.disconnect() }
            } catch (_: Exception) {
                // Leave unavailable until a future refresh succeeds.
            } finally { updating.remove(source) }
        }
    }

    private fun parseOutlook(response: JSONObject): WeatherOutlook {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.ROOT).apply {
            timeZone = java.util.TimeZone.getTimeZone(response.optString("timezone", "GMT"))
        }
        val now = format.format(java.util.Date())
        val daily = response.optJSONObject("daily")
        val days = daily?.optJSONArray("time")
        val today = (0 until (days?.length() ?: 0)).firstOrNull { days?.optString(it) == now.take(10) }
        fun dailyTemperature(key: String) = today?.let { daily?.optJSONArray(key)?.optDouble(it) }
            ?.takeIf { it.isFinite() }?.roundToInt()
        return WeatherOutlook(dailyTemperature("temperature_2m_max"), dailyTemperature("temperature_2m_min"))
    }

    internal fun parseWmoCode(code: Int): Pair<String, String> = when (code) {
        0 -> "Sereno" to "☀️"
        1, 2 -> "Poco nuvoloso" to "🌤️"
        3 -> "Nuvoloso" to "☁️"
        45, 48 -> "Nebbia" to "🌫️"
        51, 53, 55, 56, 57, 61, 63, 65, 66, 67 -> "Pioggia" to "🌧️"
        71, 73, 75, 77, 85, 86 -> "Neve" to "❄️"
        80, 81, 82 -> "Rovesci" to "🌦️"
        95, 96, 99 -> "Temporale" to "⛈️"
        else -> "Meteo sconosciuto" to "🌤️"
    }
}
