package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalendarEvent
import com.example.model.WidgetCustomization
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.WidgetAccentMagenta
import com.example.ui.theme.WidgetCoral
import com.example.ui.theme.WidgetEventLocation
import com.example.ui.theme.WidgetEventTime
import com.example.ui.theme.WidgetEventTitle
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Pixel-accurate recreation of the user screenshot widget with flawless Surface borders,
 * dynamic transparency, custom background color, and font scaling.
 */
@Composable
fun ExpressAgendaWidgetView(
    events: List<CalendarEvent>,
    targetDay: Calendar = Calendar.getInstance(),
    customization: WidgetCustomization = WidgetCustomization(),
    onEventClick: (CalendarEvent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dayNameFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    val daySubtextFormat = SimpleDateFormat("MMMM", Locale.getDefault())

    val dayName = dayNameFormat.format(targetDay.time).uppercase(Locale.getDefault())
    val daySubtext = daySubtextFormat.format(targetDay.time)

    val fontScale = customization.fontScale
    val isDark = customization.darkTheme

    val dayTextColor = customization.getPrimaryAccent()
    val dateTextColor = customization.getPrimaryTextColor()
    val cardBg = customization.getEventCardBg()
    val accentBarColor = customization.getSecondaryAccent()

    val shape = RoundedCornerShape(customization.cornerRadiusDp.dp)

    Surface(
        modifier = modifier
            .clip(shape)
            .testTag("express_agenda_widget"),
        shape = shape,
        color = customization.getResolvedBackgroundColor(),
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Sleek 2-line header
            Text(
                text = dayName,
                color = dayTextColor,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = (12f * fontScale).sp,
                letterSpacing = 1.sp,
                modifier = Modifier.testTag("widget_day_label")
            )

            Row(verticalAlignment = Alignment.Bottom) {
                Text(targetDay.get(Calendar.DAY_OF_MONTH).toString(), color = dateTextColor,
                    fontWeight = FontWeight.Light, fontSize = (60f * fontScale).sp)
                Text(daySubtext, color = customization.getSecondaryTextColor(),
                    fontSize = (15f * fontScale).sp,
                    modifier = Modifier.padding(start = 7.dp, bottom = 12.dp).testTag("widget_day_subtext"))
            }
            Spacer(Modifier.height(44.dp))

            // Event List or Weather Insight Card
            if (events.isEmpty()) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val weather by com.example.data.WeatherRepository.weather.collectAsState()
                androidx.compose.runtime.LaunchedEffect(context) { com.example.data.WeatherRepository.getWeather(context) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("widget_empty_weather_box")
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.material3.Icon(
                                painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.weather_cloud_outline),
                                contentDescription = null, tint = customization.getSecondaryTextColor(),
                                modifier = Modifier.width(18.dp).height(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (weather.isAvailable) "${weather.tempC}°" else "",
                                color = dateTextColor,
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val cityStr = if (!weather.cityName.isNullOrBlank()) " (${weather.cityName})" else ""
                            Text(
                                text = "${weather.condition}$cityStr",
                                color = customization.getSecondaryTextColor(),
                                fontFamily = PlusJakartaSans,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.12f))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nessun evento",
                            color = dateTextColor,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                        Text(
                            text = "Giornata libera",
                            color = customization.getSecondaryTextColor(),
                            fontFamily = PlusJakartaSans,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            } else {
                val displayEvents = events.take(3)
                Column {
                    displayEvents.forEachIndexed { index, event ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(spring()) + slideInVertically(spring())
                        ) {
                            ExpressEventCard(
                                event = event,
                                cardBg = cardBg,
                                accentBarColor = if (customization.useDynamicEventColors) {
                                    event.getEffectiveColor()
                                } else {
                                    accentBarColor
                                },
                                showLocation = customization.showLocation,
                                fontScale = fontScale,
                                isDark = isDark,
                                onClick = { onEventClick(event) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = if (index < displayEvents.size - 1) 9.dp else 0.dp)
                            )
                        }
                    }

                    if (events.size > 3) {
                        Text(
                            text = "+${events.size - 3} more today",
                            color = dayTextColor,
                            fontSize = (11f * fontScale).sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = PlusJakartaSans,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpressEventCard(
    event: CalendarEvent,
    cardBg: Color,
    accentBarColor: Color,
    showLocation: Boolean,
    fontScale: Float = 1.0f,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(10.dp)
    val titleColor = if (isDark) Color.White else Color(0xFF202024)
    val locationColor = if (isDark) Color(0xFFAAA4AA) else Color(0xFF615B63)
    val timeColor = accentBarColor

    Row(
        modifier = modifier
            .testTag("event_card_${event.id}")
            .clip(cardShape)
            .background(cardBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 8.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical bright neon accent bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(accentBarColor)
        )

        Spacer(modifier = Modifier.width(9.dp))

        // Event text content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                color = titleColor,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold,
                fontSize = (13.5f * fontScale).sp,
                maxLines = 2
            )

            if (showLocation && !event.location.isNullOrBlank()) {
                Text(
                    text = event.location,
                    color = locationColor,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Normal,
                    fontSize = (11f * fontScale).sp,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            Text(
                text = event.formatTimeRange(androidx.compose.ui.platform.LocalContext.current),
                color = timeColor,
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium,
                fontSize = (11f * fontScale).sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
