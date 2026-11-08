package com.example.ui.screens

import androidx.compose.runtime.collectAsState

import com.example.R
import androidx.compose.ui.res.stringResource

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RoundedCorner
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BackgroundPreset
import com.example.model.CalendarEvent
import com.example.model.WidgetCustomization
import com.example.model.WidgetType
import com.example.ui.components.RemoteViewsPreview
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.PrimaryExpressive
import com.example.ui.theme.SecondaryExpressive
import com.example.widget.WidgetBox
import com.example.widget.WidgetHelper
import com.example.widget.WidgetRenderer
import java.util.Calendar

@Composable
private fun widgetName(type: WidgetType, short: Boolean = false): String = stringResource(when (type) {
    WidgetType.EXPRESS_AGENDA -> if (short) R.string.widget_agenda_name else R.string.gallery_agenda
    WidgetType.EXPRESS_UP_NEXT -> if (short) R.string.gallery_next_short else R.string.gallery_next
    WidgetType.EXPRESS_MONTH -> if (short) R.string.gallery_month_short else R.string.gallery_month
    WidgetType.EXPRESS_WEEK_STRIP -> R.string.gallery_week
    WidgetType.EXPRESS_SPLIT_DAY -> if (short) R.string.gallery_day_short else R.string.gallery_day
})

@Composable
private fun presetName(preset: BackgroundPreset): String = stringResource(when (preset) {
    BackgroundPreset.EXPRESSIVE_DARK -> R.string.preset_dark
    BackgroundPreset.EXPRESSIVE_LIGHT -> R.string.preset_light
    BackgroundPreset.AMOLED_BLACK -> R.string.preset_black
    BackgroundPreset.MIDNIGHT_NAVY -> R.string.preset_navy
    BackgroundPreset.WARM_CREAM -> R.string.preset_cream
    BackgroundPreset.DEEP_PLUM -> R.string.preset_plum
})

@Composable
fun WidgetGalleryScreen(
    events: List<CalendarEvent>,
    selectedDay: Calendar,
    selectedCalendarIds: Set<Long>,
    customization: WidgetCustomization,
    customizations: Map<WidgetType, WidgetCustomization> = emptyMap(),
    onDaySelect: (Calendar) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onUpdateCustomization: (WidgetCustomization) -> Unit,
    onUpdateCustomizationForType: ((WidgetType, WidgetCustomization) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedWidgetType by remember { mutableStateOf(WidgetType.EXPRESS_AGENDA) }
    var activeInputDialog by remember { mutableStateOf<InputDialogType?>(null) }
    var previewRevision by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) previewRevision++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val activeCustomization = customizations[selectedWidgetType] ?: customization
    val weather by com.example.data.WeatherRepository.weather.collectAsState()
    var showWeather by remember { mutableStateOf(com.example.data.WeatherRepository.showInWidgets(context)) }

    fun saveCustomization(updated: WidgetCustomization) {
        if (onUpdateCustomizationForType != null) {
            onUpdateCustomizationForType(selectedWidgetType, updated)
        } else {
            onUpdateCustomization(updated)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 18.dp)
            .padding(bottom = 20.dp)
            .testTag("widget_gallery_screen")
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(stringResource(R.string.widget_customize), color = PrimaryExpressive, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(
            text = "Widget",
            fontSize = 38.sp,
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = PlusJakartaSans,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Widget Selector Horizontal Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WidgetType.values().forEach { widget ->
                val isSelected = widget == selectedWidgetType
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedWidgetType = widget },
                    label = {
                        Text(
                            text = widgetName(widget, short = true),
                            fontFamily = PlusJakartaSans,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = PrimaryExpressive,
                        selectedLabelColor = Color.White
                    ),
                    border = null,
                    shape = CircleShape,
                    modifier = Modifier.height(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1:1 Live Preview Canvas Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("widget_preview_container"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Size badge & widget title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = widgetName(selectedWidgetType).uppercase(),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        letterSpacing = 0.8.sp,
                        color = Color(0xFFB0B3BC)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF232630))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = selectedWidgetType.previewSize,
                            fontFamily = PlusJakartaSans,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD4D8E2)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Build exact 1:1 RemoteViews for preview
                val defaultPreviewBox = when (selectedWidgetType) {
                    WidgetType.EXPRESS_AGENDA -> WidgetBox(220f, 220f)
                    WidgetType.EXPRESS_UP_NEXT -> WidgetBox(300f, 88f)
                    WidgetType.EXPRESS_MONTH -> WidgetBox(300f, 255f)
                    WidgetType.EXPRESS_WEEK_STRIP -> WidgetBox(300f, 235f)
                    WidgetType.EXPRESS_SPLIT_DAY -> WidgetBox(300f, 180f)
                }
                val previewBox = remember(selectedWidgetType, previewRevision) {
                    WidgetHelper.previewBox(context, selectedWidgetType, defaultPreviewBox)
                }
                val previewHostScale = remember(selectedWidgetType, previewRevision) {
                    WidgetHelper.previewHostScale(context, selectedWidgetType)
                }
                val calendarRepository = remember { com.example.data.CalendarRepository() }
                val hasCalendarPermission = calendarRepository.hasCalendarPermission(context)
                val showsToday = selectedWidgetType == WidgetType.EXPRESS_AGENDA || selectedWidgetType == WidgetType.EXPRESS_SPLIT_DAY
                val previewDay = if (showsToday) Calendar.getInstance() else selectedDay
                val previewEvents = remember(selectedWidgetType, events, selectedCalendarIds, previewDay.get(Calendar.DAY_OF_YEAR), hasCalendarPermission) {
                    if (showsToday || selectedWidgetType == WidgetType.EXPRESS_WEEK_STRIP) {
                        calendarRepository.getEventsForDay(context, selectedCalendarIds, previewDay, hidePastEvents = false)
                    } else events
                }

                val remoteViews = remember(selectedWidgetType, previewEvents, previewDay.get(Calendar.DAY_OF_YEAR), activeCustomization, previewBox, hasCalendarPermission, weather, showWeather) {
                    WidgetRenderer.build(
                        context = context,
                        type = selectedWidgetType,
                        events = previewEvents,
                        day = previewDay,
                        customization = activeCustomization,
                        box = previewBox,
                        hasPermission = hasCalendarPermission
                    )
                }

                val previewWidth = previewBox.widthDp.dp
                val previewHeight = previewBox.heightDp.dp

                // RemoteViewsPreview: Exact 1:1 match with Home Screen!
                RemoteViewsPreview(
                    views = remoteViews,
                    widthDp = previewWidth,
                    heightDp = previewHeight,
                    hostScale = if (selectedWidgetType == WidgetType.EXPRESS_AGENDA) {
                        androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp * .586f / previewBox.widthDp
                    } else previewHostScale,
                    cornerRadius = 24.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pin To Home Screen Button
                Button(
                    onClick = {
                        val success = WidgetHelper.pinWidgetToHomeScreen(context, selectedWidgetType, remoteViews, previewBox)
                        if (success) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.pin_supported),
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.pin_manual),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("pin_widget_button"),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryExpressive
                    )
                ) {
                    Text(
                        text = stringResource(R.string.add_to_home),
                        maxLines = 1,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Column(Modifier.testTag("customizer_card"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    GalleryKicker(stringResource(R.string.appearance).uppercase(), PrimaryExpressive)
                    GalleryCaption(stringResource(R.string.appearance_desc))
                }
                GalleryValue(widgetName(selectedWidgetType, short = true).uppercase()) {}
            }

            GalleryPanel(R.string.background, R.string.background_desc) {
                GalleryKicker(stringResource(R.string.theme))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(true, false).forEach { dark ->
                        val selected = activeCustomization.darkTheme == dark
                        val label = stringResource(if (dark) R.string.dark else R.string.light)
                        GalleryChoice(if (selected) "$label ✓" else label, selected, Modifier.weight(1f)) {
                            saveCustomization(activeCustomization.copy(darkTheme = dark, customBgHex = null))
                        }
                    }
                }

                GalleryColorHeading(R.string.background_colors) { activeInputDialog = InputDialogType.CUSTOM_COLOR }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    BackgroundPreset.values().forEach { preset ->
                        val color = android.graphics.Color.parseColor(preset.hex)
                        val selected = activeCustomization.getResolvedBackgroundColorInt() == color
                        Column(
                            Modifier.weight(1f).clickable {
                                saveCustomization(activeCustomization.copy(darkTheme = preset.isDark, customBgHex = preset.hex))
                            },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(Modifier.size(29.dp).clip(CircleShape).background(Color(color)).border(
                                if (selected) 2.dp else 1.dp,
                                if (selected) PrimaryExpressive else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            ))
                            Spacer(Modifier.height(4.dp))
                            Text(presetName(preset), fontSize = 9.sp, maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                GallerySettingLabel(R.string.background_opacity, activeCustomization.backgroundOpacity) {
                    activeInputDialog = InputDialogType.OPACITY
                }
                GallerySlider(activeCustomization.backgroundOpacity, 0f..1f, Modifier.testTag("opacity_slider")) {
                    saveCustomization(activeCustomization.copy(backgroundOpacity = it))
                }
            }

            GalleryPanel(R.string.colors_fonts, R.string.colors_fonts_desc) {
                GalleryColorHeading(R.string.accent_color_header) { activeInputDialog = InputDialogType.CUSTOM_ACCENT }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val names = listOf("Coral", "Cyan", "Emerald", "Amber")
                    com.example.widget.WidgetPalette.PRESET_ACCENTS.forEachIndexed { index, accent ->
                        val selected = activeCustomization.accentColorIndex == index || activeCustomization.accentColorIndex == accent
                        GalleryChoice(names[index], selected, Modifier.weight(1f), Color(accent)) {
                            saveCustomization(activeCustomization.copy(accentColorIndex = index))
                        }
                    }
                }
                GallerySettingLabel(R.string.font_size, activeCustomization.fontScale) {
                    activeInputDialog = InputDialogType.FONT_SCALE
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("−", Modifier.clickable {
                        saveCustomization(activeCustomization.copy(fontScale = (activeCustomization.fontScale - .05f).coerceIn(.70f, 1.50f)))
                    }.padding(end = 8.dp))
                    GallerySlider(activeCustomization.fontScale, .70f..1.50f, Modifier.weight(1f).testTag("font_scale_slider")) {
                        saveCustomization(activeCustomization.copy(fontScale = it))
                    }
                    Text("+", Modifier.clickable {
                        saveCustomization(activeCustomization.copy(fontScale = (activeCustomization.fontScale + .05f).coerceIn(.70f, 1.50f)))
                    }.padding(start = 8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(85, 100, 115, 130).forEach { percent ->
                        val scale = percent / 100f
                        GalleryChoice("$percent%", kotlin.math.abs(activeCustomization.fontScale - scale) < .02f, Modifier.weight(1f)) {
                            saveCustomization(activeCustomization.copy(fontScale = scale))
                        }
                    }
                }
            }

            GalleryPanel(R.string.contents, R.string.contents_desc) {
                GallerySwitch(R.string.hide_past, R.string.hide_past_desc, activeCustomization.hidePastEvents) {
                    saveCustomization(activeCustomization.copy(hidePastEvents = it))
                }
                GallerySwitch(R.string.show_location, R.string.show_location_desc, activeCustomization.showLocation) {
                    saveCustomization(activeCustomization.copy(showLocation = it))
                }
            }

            Spacer(Modifier.height(4.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                GalleryKicker("METEO", PrimaryExpressive)
                GalleryCaption("Mostrato nei widget compatibili")
            }
            WeatherSettings(showWeather) { show ->
                com.example.data.WeatherRepository.setShowInWidgets(context, show)
                showWeather = show
            }
        }
    }


    // Interactive Dialogs for manual number entry & custom color picker
    when (activeInputDialog) {
        InputDialogType.OPACITY -> {
            NumberInputDialog(
                title = stringResource(R.string.number_opacity),
                initialValue = "${(activeCustomization.backgroundOpacity * 100).toInt()}",
                unitSuffix = "%",
                onDismiss = { activeInputDialog = null },
                onConfirm = { value ->
                    val rounded = (value / 100f).coerceIn(0.0f, 1.0f)
                    saveCustomization(activeCustomization.copy(backgroundOpacity = (rounded * 100).toInt() / 100f))
                }
            )
        }
        InputDialogType.FONT_SCALE -> {
            NumberInputDialog(
                title = stringResource(R.string.font_size),
                initialValue = "${(activeCustomization.fontScale * 100).toInt()}",
                unitSuffix = "%",
                onDismiss = { activeInputDialog = null },
                onConfirm = { value ->
                    val scale = (value / 100f).coerceIn(0.50f, 2.0f)
                    saveCustomization(activeCustomization.copy(fontScale = (scale * 100).toInt() / 100f))
                }
            )
        }
        InputDialogType.CUSTOM_ACCENT -> {
            ColorPickerDialog(
                title = stringResource(R.string.accent_color),
                initialHex = String.format("#%06X", (if (activeCustomization.accentColorIndex < 0) activeCustomization.accentColorIndex else com.example.widget.WidgetPalette.PRESET_ACCENTS.getOrElse(activeCustomization.accentColorIndex) { com.example.widget.WidgetPalette.PRESET_ACCENTS[0] }) and 0xFFFFFF),
                onDismiss = { activeInputDialog = null },
                onConfirm = { hex -> saveCustomization(activeCustomization.copy(accentColorIndex = android.graphics.Color.parseColor(hex))) }
            )
        }
        InputDialogType.CUSTOM_COLOR -> {
            ColorPickerDialog(
                initialHex = String.format("#%06X", activeCustomization.getResolvedBackgroundColorInt() and 0xFFFFFF),
                onDismiss = { activeInputDialog = null },
                onConfirm = { hex ->
                    val isLight = try {
                        val c = android.graphics.Color.parseColor(hex)
                        val red = android.graphics.Color.red(c)
                        val green = android.graphics.Color.green(c)
                        val blue = android.graphics.Color.blue(c)
                        (0.299 * red + 0.587 * green + 0.114 * blue) > 180
                    } catch (e: Exception) {
                        false
                    }
                    saveCustomization(
                        activeCustomization.copy(
                            darkTheme = !isLight,
                            customBgHex = hex
                        )
                    )
                }
            )
        }
        null -> {}
    }
}

@Composable
private fun GalleryKicker(label: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
}

@Composable
private fun GalleryCaption(label: String) {
    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 14.sp)
}

@Composable
private fun GalleryPanel(title: Int, caption: Int, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                Text(stringResource(title), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                GalleryCaption(stringResource(caption))
            }
            content()
        }
    }
}

@Composable
private fun GalleryColorHeading(title: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        GalleryKicker(stringResource(title))
        Icon(Icons.Default.Palette, contentDescription = stringResource(R.string.custom_color),
            modifier = Modifier.size(20.dp).clickable(onClick = onClick), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GalleryValue(label: String, onClick: () -> Unit) {
    Text(label, color = PrimaryExpressive, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 4.dp))
}

@Composable
private fun GallerySettingLabel(title: Int, fraction: Float, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(title), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        GalleryValue("${(fraction * 100).toInt()}%", onClick)
    }
}

@Composable
private fun GalleryChoice(label: String, selected: Boolean, modifier: Modifier, swatch: Color? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier.height(if (swatch == null) 34.dp else 46.dp).clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
            .then(if (selected) Modifier.border(1.dp, PrimaryExpressive, shape) else Modifier)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (swatch != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(Modifier.size(4.dp).background(swatch, CircleShape))
                Box(Modifier.size(4.dp).background(swatch.copy(alpha = .55f), CircleShape))
            }
            Spacer(Modifier.height(3.dp))
        }
        Text(label, fontSize = 11.sp, maxLines = 1,
            color = if (selected) PrimaryExpressive else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun GallerySlider(value: Float, range: ClosedFloatingPointRange<Float>, modifier: Modifier, onChange: (Float) -> Unit) {
    Slider(value = value.coerceIn(range.start, range.endInclusive), onValueChange = onChange, valueRange = range,
        modifier = modifier.height(25.dp),
        track = { state ->
            val progress = ((state.value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(7.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                Box(Modifier.fillMaxWidth(progress).height(7.dp).background(PrimaryExpressive, CircleShape))
            }
        },
        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = PrimaryExpressive,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant))
}

@Composable
private fun GallerySwitch(title: Int, caption: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            GalleryCaption(stringResource(caption))
        }
        Switch(checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryExpressive))
    }
}

enum class InputDialogType {
    OPACITY,
    FONT_SCALE,
    CUSTOM_COLOR,
    CUSTOM_ACCENT
}

@Composable
fun ColorPickerDialog(
    initialHex: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    title: String = ""
) {
    val context = LocalContext.current
    var hexInput by remember { mutableStateOf(initialHex) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val quickSwatches = listOf(
        "#18181A", "#FFFFFF", "#000000", "#101622", "#F9F7F2", "#221124",
        "#FF5252", "#00E5FF", "#00E676", "#FFAB00", "#673AB7", "#E91E63"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (title.isEmpty()) stringResource(R.string.background_color) else title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.color_instructions),
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                val previewColor = runCatching { Color(android.graphics.Color.parseColor(hexInput)) }.getOrDefault(Color.Gray)
                Box(Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(previewColor).border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)))
                Spacer(modifier = Modifier.height(14.dp))

                // Swatch Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val rows = quickSwatches.chunked(4)
                    for (row in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (hex in row) {
                                val c = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = hexInput.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) PrimaryExpressive else Color.Gray.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            hexInput = hex
                                            errorMsg = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (hex == "#FFFFFF" || hex == "#F9F7F2") Color.Black else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val parsed = runCatching { android.graphics.Color.parseColor(hexInput) }.getOrNull()
                if (parsed != null && hexInput.matches(Regex("#[0-9a-fA-F]{6}"))) {
                    listOf("R", "G", "B").forEachIndexed { index, channel ->
                        val components = intArrayOf(android.graphics.Color.red(parsed), android.graphics.Color.green(parsed), android.graphics.Color.blue(parsed))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(channel, modifier = Modifier.width(20.dp), fontFamily = PlusJakartaSans)
                            Slider(value = components[index].toFloat(), onValueChange = { value ->
                                components[index] = value.toInt()
                                hexInput = String.format("#%02X%02X%02X", components[0], components[1], components[2])
                                errorMsg = null
                            }, valueRange = 0f..255f, modifier = Modifier.weight(1f))
                            Text(components[index].toString(), modifier = Modifier.width(32.dp), fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = hexInput,
                    onValueChange = {
                        hexInput = it.trim()
                        errorMsg = null
                    },
                    label = { Text(stringResource(R.string.hex_code)) },
                    singleLine = true,
                    isError = errorMsg != null,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formatted = if (hexInput.startsWith("#")) hexInput else "#$hexInput"
                    try {
                        require(formatted.matches(Regex("#[0-9a-fA-F]{6}")))
                        android.graphics.Color.parseColor(formatted)
                        onConfirm(formatted)
                        onDismiss()
                    } catch (e: Exception) {
                        errorMsg = context.getString(R.string.invalid_color)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryExpressive)
            ) {
                Text(stringResource(R.string.apply), fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), fontFamily = PlusJakartaSans)
            }
        }
    )
}

@Composable
fun NumberInputDialog(
    title: String,
    initialValue: String,
    unitSuffix: String,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var textValue by remember { mutableStateOf(initialValue) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.enter_value, unitSuffix),
                    fontFamily = PlusJakartaSans,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = {
                        textValue = it
                        errorMsg = null
                    },
                    label = { Text(stringResource(R.string.value_label, unitSuffix)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorMsg != null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = textValue.toFloatOrNull()
                    if (parsed != null) {
                        onConfirm(parsed)
                        onDismiss()
                    } else {
                        errorMsg = "Please enter a valid number"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryExpressive)
            ) {
                Text(stringResource(R.string.confirm), fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), fontFamily = PlusJakartaSans)
            }
        }
    )
}
