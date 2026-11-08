package com.example

import android.Manifest
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.data.WeatherRepository
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WeatherRepositoryTest {
    @Test fun `saved city weather works without location permission`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        shadowOf(context as android.app.Application).denyPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        context.getSharedPreferences("weather_location_v2", Context.MODE_PRIVATE).edit()
            .putString("selected_city", "Torino, Italia").putInt("temp", 18)
            .putLong("timestamp", System.currentTimeMillis()).commit()

        val weather = WeatherRepository.getWeather(context)
        assertTrue(weather.isAvailable)
        assertEquals("Torino, Italia", weather.cityName)
        assertEquals(18, weather.tempC)
    }
    @Test fun `weather is unavailable without location permission even with old cache`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        shadowOf(context as android.app.Application).denyPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
        context.getSharedPreferences("weather_location_v2", Context.MODE_PRIVATE).edit()
            .putInt("temp", 30).putLong("timestamp", System.currentTimeMillis()).commit()
        assertFalse(WeatherRepository.getWeather(context).isAvailable)
    }

    @Test fun `open meteo precipitation codes are mapped`() {
        assertEquals("Neve", WeatherRepository.parseWmoCode(85).first)
        assertEquals("Pioggia", WeatherRepository.parseWmoCode(66).first)
        assertEquals("Meteo sconosciuto", WeatherRepository.parseWmoCode(-1).first)
    }
}
