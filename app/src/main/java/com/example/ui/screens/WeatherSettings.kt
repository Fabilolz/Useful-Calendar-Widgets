package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.R
import com.example.data.WeatherRepository
import kotlinx.coroutines.launch

@Composable
internal fun WeatherSettings(showWeather: Boolean, onShowWeatherChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var city by remember { mutableStateOf(WeatherRepository.selectedCity(context)) }
    var query by remember { mutableStateOf("") }
    var open by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            WeatherRepository.useDeviceLocation(context)
            city = null
            open = false
        }
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { open = true }
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.weather_location), style = MaterialTheme.typography.titleSmall)
                Text(city ?: stringResource(R.string.weather_device_location), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            Modifier.fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val label = stringResource(R.string.weather_show_in_widgets)
            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Switch(
                checked = showWeather,
                onCheckedChange = onShowWeatherChange,
                modifier = Modifier.semantics { contentDescription = label }
            )
        }
    }
    if (!open) return
    AlertDialog(
        onDismissRequest = { if (!searching) open = false },
        title = { Text(stringResource(R.string.weather_location_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.weather_city_help))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; error = false },
                    enabled = !searching,
                    label = { Text(stringResource(R.string.weather_city)) },
                    placeholder = { Text(stringResource(R.string.weather_city_example)) },
                    singleLine = true,
                    isError = error,
                    supportingText = if (error) ({ Text(stringResource(R.string.weather_city_error)) }) else null
                )
                TextButton(enabled = !searching, onClick = {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        WeatherRepository.useDeviceLocation(context)
                        city = null
                        open = false
                    } else {
                        permission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                }) { Text(stringResource(R.string.weather_use_location)) }
            }
        },
        confirmButton = {
            Button(enabled = query.isNotBlank() && !searching, onClick = {
                searching = true
                scope.launch {
                    try {
                        val result = WeatherRepository.selectCity(context, query.trim())
                        if (result == null) error = true else {
                            city = result
                            open = false
                        }
                    } finally {
                        searching = false
                    }
                }
            }) { Text(if (searching) stringResource(R.string.weather_searching) else stringResource(R.string.weather_save_city)) }
        },
        dismissButton = { TextButton(enabled = !searching, onClick = { open = false }) { Text(stringResource(R.string.weather_cancel)) } }
    )
}
