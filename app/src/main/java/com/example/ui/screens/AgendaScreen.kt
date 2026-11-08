package com.example.ui.screens

import com.example.R
import androidx.compose.ui.res.stringResource

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalendarEvent
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.PrimaryExpressive
import com.example.ui.theme.SecondaryExpressive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendaScreen(
    events: List<CalendarEvent>,
    selectedDay: Calendar,
    highlightedEventId: Long? = null,
    onDayChange: (Calendar) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    onAddNewEvent: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val isToday = remember(selectedDay) {
        val today = Calendar.getInstance()
        today.get(Calendar.DAY_OF_YEAR) == selectedDay.get(Calendar.DAY_OF_YEAR) &&
                today.get(Calendar.YEAR) == selectedDay.get(Calendar.YEAR)
    }

    val filteredEvents = remember(events, searchQuery) {
        if (searchQuery.isBlank()) events else events.filter { event ->
            event.title.contains(searchQuery, ignoreCase = true) ||
                event.location?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    // Scroll to & highlight specific event when deep-linked from widget
    LaunchedEffect(highlightedEventId, filteredEvents) {
        if (highlightedEventId != null && highlightedEventId > 0) {
            val targetIndex = filteredEvents.indexOfFirst { it.id == highlightedEventId }
            if (targetIndex >= 0) {
                // Header takes index 0, search takes index 0 inside items or +1 item offset
                listState.animateScrollToItem(targetIndex + 1)
            }
        }
    }

    // Material 3 Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDay.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance().apply { timeInMillis = millis }
                            onDayChange(cal)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel), fontFamily = PlusJakartaSans)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(bottom = 72.dp)
                .testTag("agenda_screen")
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                // Date Navigation Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = (if (isToday) stringResource(R.string.today) + " · " else "") +
                            SimpleDateFormat("EEEE", Locale.getDefault()).format(selectedDay.time),
                        modifier = Modifier.weight(1f),
                        color = PrimaryExpressive,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val prev = selectedDay.clone() as Calendar
                                prev.add(Calendar.DAY_OF_MONTH, -1)
                                onDayChange(prev)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.previous_day)
                            )
                        }

                        if (!isToday) {
                            TextButton(
                                onClick = { onDayChange(Calendar.getInstance()) }
                            ) {
                                Text(stringResource(R.string.today), fontFamily = PlusJakartaSans, fontWeight = FontWeight.Bold)
                            }
                        }

                        IconButton(
                            onClick = {
                                val next = selectedDay.clone() as Calendar
                                next.add(Calendar.DAY_OF_MONTH, 1)
                                onDayChange(next)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = stringResource(R.string.next_day)
                            )
                        }
                    }
                }

                Text(
                    text = SimpleDateFormat("d MMMM", Locale.getDefault()).format(selectedDay.time),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    fontFamily = PlusJakartaSans,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_events), fontFamily = PlusJakartaSans) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_events_input"),
                    shape = RoundedCornerShape(50),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedBorderColor = Color.Transparent)
                )

                    IconButton(onClick = { showDatePicker = true },
                        modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primaryContainer)) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = stringResource(R.string.select_date), tint = PrimaryExpressive)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (filteredEvents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.EventNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isBlank()) stringResource(R.string.no_events_day) else stringResource(R.string.no_matching_events),
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp
                            )
                            Text(
                                text = stringResource(R.string.add_event_hint),
                                fontFamily = PlusJakartaSans,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(filteredEvents, key = { it.id }) { event ->
                    val isHighlighted = event.id == highlightedEventId
                    AnimatedContent(
                        targetState = event,
                        transitionSpec = {
                            fadeIn(tween(250)) + slideInVertically { 20 } togetherWith fadeOut(tween(200)) + slideOutVertically { -20 }
                        },
                        label = "event_item_anim"
                    ) { ev ->
                        EventTimelineCard(
                            event = ev,
                            isHighlighted = isHighlighted,
                            onClick = { onEventClick(ev) }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // FAB to add new event positioned properly per M3 guidelines (16dp end, 16dp bottom)
        FloatingActionButton(
            onClick = onAddNewEvent,
            containerColor = PrimaryExpressive,
            contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp)
                .testTag("add_event_fab")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.add_event))
        }
    }
}

@Composable
fun EventTimelineCard(
    event: CalendarEvent,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("timeline_event_${event.id}"),
        shape = RoundedCornerShape(18.dp),
        border = if (isHighlighted) BorderStroke(2.dp, PrimaryExpressive) else null,
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.background, Color(event.calendarColor), if (isHighlighted) 0.38f else 0.26f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored Vertical Stripe
            Box(
                modifier = Modifier
                    .width(if (isHighlighted) 6.dp else 4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isHighlighted) PrimaryExpressive else event.getEffectiveColor())
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isHighlighted) PrimaryExpressive else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(event.getEffectiveColor().copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = event.calendarName,
                            color = event.getEffectiveColor(),
                            fontFamily = PlusJakartaSans,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 90.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = event.formatTimeRange(androidx.compose.ui.platform.LocalContext.current),
                        fontFamily = PlusJakartaSans,
                        fontSize = 12.sp,
                        color = event.getEffectiveColor()
                    )

                    if (!event.location.isNullOrBlank()) {
                        Text(" · ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = event.location,
                            fontFamily = PlusJakartaSans,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }
        }
    }
}
