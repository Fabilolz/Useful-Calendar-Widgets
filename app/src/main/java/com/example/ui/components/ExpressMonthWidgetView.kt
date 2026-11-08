package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalendarEvent
import com.example.model.WidgetCustomization
import com.example.ui.theme.PlusJakartaSans
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Express Month & Day Dots Widget with dynamic font scaling, Surface borders and theme support
 */
@Composable
fun ExpressMonthWidgetView(
    events: List<CalendarEvent>,
    selectedDay: Calendar,
    onDaySelect: (Calendar) -> Unit = {},
    onEventClick: (CalendarEvent) -> Unit = {},
    customization: WidgetCustomization = WidgetCustomization(),
    modifier: Modifier = Modifier
) {
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val monthYearTitle = monthYearFormat.format(selectedDay.time).uppercase(Locale.getDefault())

    val fontScale = customization.fontScale

    val accentColor = customization.getPrimaryAccent()
    val secondaryColor = customization.getSecondaryAccent()
    val textColor = customization.getPrimaryTextColor()
    val subTextColor = customization.getSecondaryTextColor()
    val shape = RoundedCornerShape(customization.cornerRadiusDp.dp)

    // Compute month grid
    val cal = selectedDay.clone() as Calendar
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val offset = (firstDayOfWeek + 5) % 7
    val todayCal = Calendar.getInstance()
    val isCurrentMonth = todayCal.get(Calendar.MONTH) == selectedDay.get(Calendar.MONTH) &&
            todayCal.get(Calendar.YEAR) == selectedDay.get(Calendar.YEAR)
    val todayDate = if (isCurrentMonth) todayCal.get(Calendar.DAY_OF_MONTH) else -1
    val selectedDate = selectedDay.get(Calendar.DAY_OF_MONTH)

    val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")

    val now = System.currentTimeMillis()
    val activeEvents = if (customization.hidePastEvents) {
        events.filter { it.endMillis >= now }
    } else {
        events
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .testTag("express_month_widget"),
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
                    text = monthYearTitle,
                    color = accentColor,
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = (12f * fontScale).sp,
                    letterSpacing = 1.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (customization.darkTheme) Color(0xFF2A2A2E) else Color(0xFFEFF1F5))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${activeEvents.size} upcoming",
                        color = secondaryColor,
                        fontFamily = PlusJakartaSans,
                        fontSize = (11f * fontScale).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weekday headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekdays.forEach { dayLetter ->
                    Text(
                        text = dayLetter,
                        color = subTextColor,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (11f * fontScale).sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Days Grid
            val totalCells = ((offset + daysInMonth + 6) / 7) * 7
            val weeks = totalCells / 7

            Column {
                for (week in 0 until minOf(weeks, 5)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0..6) {
                            val cellIndex = week * 7 + col
                            val dayNum = cellIndex - offset + 1

                            if (dayNum in 1..daysInMonth) {
                                val isSelected = dayNum == selectedDate
                                val isToday = dayNum == todayDate

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> accentColor
                                                isToday -> secondaryColor.copy(alpha = 0.25f)
                                                else -> Color.Transparent
                                            }
                                        )
                                        .clickable {
                                            val newCal = selectedDay.clone() as Calendar
                                            newCal.set(Calendar.DAY_OF_MONTH, dayNum)
                                            onDaySelect(newCal)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNum.toString(),
                                            color = when {
                                                isSelected -> Color.White
                                                isToday -> secondaryColor
                                                else -> textColor
                                            },
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = (11f * fontScale).sp
                                        )

                                        if (dayNum in listOf(14, 15, 17, 20, 22) || (isToday && activeEvents.isNotEmpty())) {
                                            Box(
                                                modifier = Modifier
                                                    .size(3.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) Color.White else secondaryColor
                                                    )
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom top event teaser for selected day
            val topEvent = activeEvents.firstOrNull()
            if (topEvent != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (customization.darkTheme) Color(0xFF241C2B) else Color(0xFFF6ECF5))
                        .clickable { onEventClick(topEvent) }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(secondaryColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${topEvent.title} (${topEvent.formatTimeRange(androidx.compose.ui.platform.LocalContext.current)})",
                        color = if (customization.darkTheme) Color(0xFFFF80AB) else Color(0xFF880E4F),
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (11f * fontScale).sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
