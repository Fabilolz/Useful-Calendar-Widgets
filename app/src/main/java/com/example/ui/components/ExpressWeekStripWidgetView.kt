package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
 * Express 7-Day Timeline Strip Widget with dynamic font scaling, Surface borders and theme support
 */
@Composable
fun ExpressWeekStripWidgetView(
    events: List<CalendarEvent>,
    selectedDay: Calendar,
    onDaySelect: (Calendar) -> Unit = {},
    onEventClick: (CalendarEvent) -> Unit = {},
    customization: WidgetCustomization = WidgetCustomization(),
    modifier: Modifier = Modifier
) {
    val accentColor = customization.getPrimaryAccent()
    val secondaryColor = customization.getSecondaryAccent()
    val textColor = customization.getPrimaryTextColor()
    val subTextColor = customization.getSecondaryTextColor()
    val shape = RoundedCornerShape(customization.cornerRadiusDp.dp)

    val fontScale = customization.fontScale

    // Build the 7 days of the current week (starting Monday)
    val startOfWeek = selectedDay.clone() as Calendar
    val dayOfWeek = startOfWeek.get(Calendar.DAY_OF_WEEK)
    val daysFromMonday = (dayOfWeek + 5) % 7
    startOfWeek.add(Calendar.DAY_OF_MONTH, -daysFromMonday)

    val weekDays = (0..6).map { i ->
        val c = startOfWeek.clone() as Calendar
        c.add(Calendar.DAY_OF_MONTH, i)
        c
    }

    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val selectedDayNum = selectedDay.get(Calendar.DAY_OF_YEAR)

    val now = System.currentTimeMillis()
    val activeEvents = if (customization.hidePastEvents) {
        events.filter { it.endMillis >= now }
    } else {
        events
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .testTag("express_week_strip_widget"),
        shape = shape,
        color = customization.getResolvedBackgroundColor(),
        border = BorderStroke(1.2.dp, customization.getResolvedBorderColor())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "THIS WEEK'S TIMELINE",
                    color = accentColor,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = (12f * fontScale).sp,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "${activeEvents.size} items",
                    color = secondaryColor,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (11.5f * fontScale).sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7 Days Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDays.forEach { cal ->
                    val isSelected = cal.get(Calendar.DAY_OF_YEAR) == selectedDayNum
                    val dayLabel = dayFormat.format(cal.time).uppercase(Locale.getDefault()).take(2)
                    val dateNum = cal.get(Calendar.DAY_OF_MONTH).toString()

                    val pillBg = when {
                        isSelected -> accentColor
                        customization.darkTheme -> Color(0xFF232328)
                        else -> Color(0xFFEFF1F5)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(pillBg)
                            .clickable { onDaySelect(cal) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayLabel,
                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else subTextColor,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = (10f * fontScale).sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = dateNum,
                            color = if (isSelected) Color.White else textColor,
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = (14f * fontScale).sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selected day's schedule items
            if (activeEvents.isEmpty()) {
                Text(
                    text = "Free day! No upcoming events.",
                    color = subTextColor,
                    fontFamily = PlusJakartaSans,
                    fontSize = (12f * fontScale).sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                activeEvents.take(2).forEachIndexed { idx, ev ->
                    ExpressEventCard(
                        event = ev,
                        cardBg = customization.getEventCardBg(),
                        accentBarColor = secondaryColor,
                        showLocation = customization.showLocation,
                        fontScale = fontScale,
                        isDark = customization.darkTheme,
                        onClick = { onEventClick(ev) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (idx == 0 && activeEvents.size > 1) 6.dp else 0.dp)
                    )
                }
            }
        }
    }
}
