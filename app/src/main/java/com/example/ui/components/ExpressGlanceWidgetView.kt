package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Compact "Up Next / Focus Pill" widget with dynamic font scaling, flawless Surface borders,
 * dynamic opacity, custom background colors, and strict past-event filtering.
 */
@Composable
fun ExpressGlanceWidgetView(
    events: List<CalendarEvent>,
    targetDay: Calendar = Calendar.getInstance(),
    customization: WidgetCustomization = WidgetCustomization(),
    onEventClick: (CalendarEvent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dayShortFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dayNumberFormat = SimpleDateFormat("d", Locale.getDefault())

    val dayName = dayShortFormat.format(targetDay.time).uppercase(Locale.getDefault())
    val dayNumber = dayNumberFormat.format(targetDay.time)

    val fontScale = customization.fontScale

    val accentColor = customization.getPrimaryAccent()
    val secondaryColor = customization.getSecondaryAccent()
    val dateTextColor = customization.getPrimaryTextColor()
    val subTextColor = customization.getSecondaryTextColor()
    val shape = RoundedCornerShape(customization.cornerRadiusDp.dp)

    val now = System.currentTimeMillis()
    val activeEvents = events.filter { it.effectiveEndMillis >= now }
    // Next event MUST be currently active or upcoming, never a past event
    val nextEvent = activeEvents.firstOrNull { it.isUpcoming(now) || it.isHappeningNow(now) }

    Surface(
        modifier = modifier
            .clip(shape)
            .testTag("express_glance_widget")
            .clickable(enabled = nextEvent != null) {
                nextEvent?.let { onEventClick(it) }
            },
        shape = shape,
        color = customization.getResolvedBackgroundColor(),
        border = BorderStroke(1.2.dp, customization.getResolvedBorderColor())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Date Pill
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (customization.isLightBackground()) Color(0xFFEFF1F5) else Color(0xFF26262B)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dayName,
                        color = accentColor,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = (10.5f * fontScale).sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = dayNumber,
                        color = dateTextColor,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = (20f * fontScale).sp,
                        lineHeight = (22f * fontScale).sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Right Event Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (nextEvent == null) "ALL CLEAR" else if (nextEvent.isHappeningNow()) "HAPPENING NOW" else "UP NEXT",
                        color = secondaryColor,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = (10f * fontScale).sp,
                        letterSpacing = 0.8.sp
                    )

                    if (nextEvent != null) {
                        val badgeText = if (nextEvent.isHappeningNow(now)) {
                            val remainingMillis = nextEvent.effectiveEndMillis - now
                            if (remainingMillis > 0) {
                                val mins = (remainingMillis + 59_999) / (60 * 1000)
                                val hours = mins / 60
                                if (hours > 0) "ends in ${hours}h ${mins % 60}m" else "ends in ${mins}m"
                            } else "ends now"
                        } else {
                            nextEvent.getRelativeTimeText(now)
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(secondaryColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                color = secondaryColor,
                                fontSize = (9.5f * fontScale).sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = PlusJakartaSans
                            )
                        }
                    }
                }

                if (nextEvent != null) {
                    Text(
                        text = nextEvent.title,
                        color = dateTextColor,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = (14.5f * fontScale).sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    val locText = if (customization.showLocation && !nextEvent.location.isNullOrBlank()) {
                        " • ${nextEvent.location}"
                    } else ""
                    Text(
                        text = "${nextEvent.formatTimeRange(androidx.compose.ui.platform.LocalContext.current)}$locText",
                        color = subTextColor,
                        fontFamily = PlusJakartaSans,
                        fontSize = (11.5f * fontScale).sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else {
                    Text(
                        text = "All clear today",
                        color = dateTextColor,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = (14.5f * fontScale).sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "No upcoming events scheduled",
                        color = subTextColor,
                        fontFamily = PlusJakartaSans,
                        fontSize = (11.5f * fontScale).sp
                    )
                }
            }
        }
    }
}
