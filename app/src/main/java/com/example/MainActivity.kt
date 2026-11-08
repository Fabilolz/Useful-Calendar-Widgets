package com.example

import com.example.R
import androidx.compose.ui.res.stringResource

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AddEventBottomSheet
import com.example.ui.components.EventDetailDialog
import com.example.ui.screens.AgendaScreen
import com.example.ui.screens.CalendarFilterScreen
import com.example.ui.screens.WidgetGalleryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PlusJakartaSans
import com.example.ui.theme.PrimaryExpressive

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleWidgetIntent(intent)

        setContent {
            MyApplicationTheme(darkTheme = true) {
                ExpressCalApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        DailyIconReceiver.update(this)
        viewModel.registerCalendarObserver()
        val prefs = getSharedPreferences("calendar_sync", MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong("last_request", 0L) > 60 * 60 * 1000L) {
            viewModel.requestCalendarSync()
            prefs.edit().putLong("last_request", now).apply()
        }
    }

    private fun handleWidgetIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val targetTab = intent.getStringExtra("target_tab")
        if (targetTab == "agenda") {
            viewModel.setTab(NavigationTab.AGENDA)
            val startMillis = intent.getLongExtra("start_millis", -1L)
            if (startMillis > 0) {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = startMillis }
                viewModel.selectDay(cal)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpressCalApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.onPermissionResult(
            androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
        if (results[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            com.example.data.WeatherRepository.refreshLocation(context)
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        if (!uiState.hasCalendarPermission) {
            permissions.add(Manifest.permission.READ_CALENDAR)
            permissions.add(Manifest.permission.WRITE_CALENDAR)
        }
        com.example.data.WeatherRepository.refreshLocation(context)
        if (permissions.isNotEmpty()) permissionLauncher.launch(permissions.toTypedArray())
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = androidx.compose.ui.graphics.Color(0xFF121214),
                tonalElevation = 0.dp,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = uiState.currentTab == NavigationTab.WIDGETS,
                    onClick = { viewModel.setTab(NavigationTab.WIDGETS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == NavigationTab.WIDGETS) Icons.Filled.Widgets else Icons.Outlined.Widgets,
                            contentDescription = "Widget",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Widget",
                            fontFamily = PlusJakartaSans,
                            fontWeight = if (uiState.currentTab == NavigationTab.WIDGETS) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryExpressive,
                        selectedTextColor = PrimaryExpressive,
                        indicatorColor = PrimaryExpressive.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_item_widgets")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == NavigationTab.CALENDARS,
                    onClick = { viewModel.setTab(NavigationTab.CALENDARS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == NavigationTab.CALENDARS) Icons.Filled.FilterList else Icons.Outlined.FilterList,
                            contentDescription = stringResource(R.string.calendars),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.calendars),
                            fontFamily = PlusJakartaSans,
                            fontWeight = if (uiState.currentTab == NavigationTab.CALENDARS) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryExpressive,
                        selectedTextColor = PrimaryExpressive,
                        indicatorColor = PrimaryExpressive.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_item_calendars")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == NavigationTab.AGENDA,
                    onClick = { viewModel.setTab(NavigationTab.AGENDA) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.currentTab == NavigationTab.AGENDA) Icons.Filled.Schedule else Icons.Outlined.Schedule,
                            contentDescription = "Agenda",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Agenda",
                            fontFamily = PlusJakartaSans,
                            fontWeight = if (uiState.currentTab == NavigationTab.AGENDA) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryExpressive,
                        selectedTextColor = PrimaryExpressive,
                        indicatorColor = PrimaryExpressive.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_item_agenda")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = uiState.currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_tab_anim"
            ) { tab ->
                when (tab) {
                    NavigationTab.WIDGETS -> {
                        WidgetGalleryScreen(
                            events = uiState.events,
                            selectedDay = uiState.selectedDay,
                            selectedCalendarIds = uiState.selectedCalendarIds,
                            customization = uiState.customization,
                            customizations = uiState.customizations,
                            onDaySelect = { viewModel.selectDay(it) },
                            onEventClick = { viewModel.openEventDetail(it) },
                            onUpdateCustomization = { viewModel.updateCustomization(it) },
                            onUpdateCustomizationForType = { type, cust -> viewModel.updateCustomizationForType(type, cust) }
                        )
                    }
                    NavigationTab.CALENDARS -> {
                        CalendarFilterScreen(
                            calendars = uiState.calendars,
                            selectedCalendarIds = uiState.selectedCalendarIds,
                            hasCalendarPermission = uiState.hasCalendarPermission,
                            onRequestPermission = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_CALENDAR,
                                        Manifest.permission.WRITE_CALENDAR
                                    )
                                )
                            },
                            onToggleCalendar = { viewModel.toggleCalendarSelection(it) },
                            onSelectAll = { viewModel.selectAllCalendars() },
                            onDeselectAll = { viewModel.deselectAllCalendars() }
                        )
                    }
                    NavigationTab.AGENDA -> {
                        AgendaScreen(
                            events = uiState.events,
                            selectedDay = uiState.selectedDay,
                            highlightedEventId = uiState.highlightedEventId,
                            onDayChange = { viewModel.selectDay(it) },
                            onEventClick = { viewModel.openCalendarEvent(it) },
                            onAddNewEvent = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                                    data = android.provider.CalendarContract.Events.CONTENT_URI
                                    putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, uiState.selectedDay.timeInMillis)
                                }
                                try { context.startActivity(intent) } catch (_: android.content.ActivityNotFoundException) {
                                    android.widget.Toast.makeText(context, "Nessuna app calendario disponibile", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // Event Details Dialog
            if (uiState.selectedEventForDetail != null) {
                EventDetailDialog(
                    event = uiState.selectedEventForDetail,
                    onDismiss = { viewModel.dismissEventDetail() },
                    onOpenInCalendar = { ev ->
                        viewModel.openCalendarAtDate(ev.startMillis)
                    }
                )
            }

            // Add Event Bottom Sheet
            if (uiState.isAddEventSheetVisible) {
                AddEventBottomSheet(
                    calendars = uiState.calendars,
                    targetDay = uiState.selectedDay,
                    onDismiss = { viewModel.dismissAddEventSheet() },
                    onSaveEvent = { calId, title, loc, desc, start, end, allDay ->
                        viewModel.addNewEvent(calId, title, loc, desc, start, end, allDay)
                    }
                )
            }
        }
    }
}
