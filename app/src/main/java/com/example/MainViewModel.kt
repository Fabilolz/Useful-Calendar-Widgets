package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CalendarRepository
import com.example.data.PreferencesManager
import com.example.model.CalendarAccount
import com.example.model.CalendarEvent
import com.example.model.WidgetCustomization
import com.example.widget.WidgetHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

data class MainUiState(
    val calendars: List<CalendarAccount> = emptyList(),
    val selectedCalendarIds: Set<Long> = emptySet(),
    val events: List<CalendarEvent> = emptyList(),
    val selectedDay: Calendar = Calendar.getInstance(),
    val customization: WidgetCustomization = WidgetCustomization(),
    val customizations: Map<com.example.model.WidgetType, WidgetCustomization> = emptyMap(),
    val hasCalendarPermission: Boolean = false,
    val selectedEventForDetail: CalendarEvent? = null,
    val highlightedEventId: Long? = null,
    val isAddEventSheetVisible: Boolean = false,
    val currentTab: NavigationTab = NavigationTab.WIDGETS
)

enum class NavigationTab {
    WIDGETS,
    CALENDARS,
    AGENDA
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CalendarRepository()
    private val prefs = PreferencesManager(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private var observerRegistered = false
    private val sourceReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            val state = _uiState.value
            if (repository.hasCalendarPermission(context)) {
                refreshEvents(state.calendars, state.selectedCalendarIds, state.selectedDay)
            }
        }
    }

    private val calendarObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            val state = _uiState.value
            if (state.hasCalendarPermission) {
                refreshEvents(state.calendars, state.selectedCalendarIds, state.selectedDay)
                WidgetHelper.updateAllWidgets(getApplication())
            }
        }
    }

    init {
        androidx.core.content.ContextCompat.registerReceiver(application, sourceReceiver,
            android.content.IntentFilter(com.example.data.PublicCalendarTimes.ACTION_UPDATED),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
        registerCalendarObserver()
        loadInitialData()
        startPeriodicDetection()
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(sourceReceiver)
        if (observerRegistered) getApplication<Application>().contentResolver.unregisterContentObserver(calendarObserver)
        super.onCleared()
    }

    fun requestCalendarSync() {
        val context = getApplication<Application>()
        if (!repository.hasCalendarPermission(context)) return
        repository.getCalendars(context).filter { it.accountType == "com.google" }
            .map { android.accounts.Account(it.accountName, it.accountType) }.distinct().forEach { account ->
                val extras = android.os.Bundle().apply {
                    putBoolean(android.content.ContentResolver.SYNC_EXTRAS_MANUAL, true)
                    putBoolean(android.content.ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                }
                android.content.ContentResolver.requestSync(account, android.provider.CalendarContract.AUTHORITY, extras)
            }
    }

    fun registerCalendarObserver() {
        val context = getApplication<Application>()
        if (observerRegistered || !repository.hasCalendarPermission(context)) return
        try {
            context.contentResolver.registerContentObserver(android.provider.CalendarContract.Events.CONTENT_URI, true, calendarObserver)
            observerRegistered = true
        } catch (_: SecurityException) {
            // Permission may be revoked between checking and registering.
        }
    }

    private fun startPeriodicDetection() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30_000)
                val state = _uiState.value
                val events = repository.getEventsForDay(
                    context = getApplication(),
                    selectedCalendarIds = state.selectedCalendarIds,
                    targetCalendar = state.selectedDay,
                    hidePastEvents = false
                )
                _uiState.value = _uiState.value.copy(events = events)
                WidgetHelper.updateAllWidgets(getApplication())
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val savedSelectedIds = prefs.selectedCalendarIdsFlow.first()
            val savedCustomization = prefs.widgetCustomizationFlow.first()
            val allCustomizations = prefs.allCustomizationsFlow.first()
            val hasPerm = repository.hasCalendarPermission(getApplication())

            val calendars = repository.getCalendars(getApplication())
            val effectiveSelectedIds = savedSelectedIds ?: calendars.map { it.id }.toSet()

            val selectedDay = _uiState.value.selectedDay
            val events = repository.getEventsForDay(
                context = getApplication(),
                selectedCalendarIds = effectiveSelectedIds,
                targetCalendar = selectedDay,
                hidePastEvents = false
            )

            _uiState.value = _uiState.value.copy(
                calendars = calendars,
                selectedCalendarIds = effectiveSelectedIds,
                events = events,
                customization = savedCustomization,
                customizations = allCustomizations,
                hasCalendarPermission = hasPerm
            )
        }
    }

    fun onPermissionResult(granted: Boolean) {
        if (granted) registerCalendarObserver()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(hasCalendarPermission = granted)
            val calendars = repository.getCalendars(getApplication())
            val selected = _uiState.value.selectedCalendarIds.ifEmpty {
                calendars.map { it.id }.toSet()
            }
            refreshEvents(calendars, selected, _uiState.value.selectedDay)
        }
    }

    fun setTab(tab: NavigationTab) {
        _uiState.value = _uiState.value.copy(currentTab = tab)
    }

    fun toggleCalendarSelection(calendarId: Long) {
        viewModelScope.launch {
            val current = _uiState.value.selectedCalendarIds.toMutableSet()
            if (current.contains(calendarId)) {
                current.remove(calendarId)
            } else {
                current.add(calendarId)
            }
            prefs.saveSelectedCalendarIds(current)
            _uiState.value = _uiState.value.copy(selectedCalendarIds = current)
            refreshEvents(
                _uiState.value.calendars,
                current,
                _uiState.value.selectedDay
            )
            WidgetHelper.updateAllWidgets(getApplication())
        }
    }

    fun selectAllCalendars() {
        viewModelScope.launch {
            val cals = if (_uiState.value.calendars.isEmpty()) repository.getCalendars(getApplication()) else _uiState.value.calendars
            val allIds = cals.map { it.id }.toSet()
            prefs.saveSelectedCalendarIds(allIds)
            _uiState.value = _uiState.value.copy(calendars = cals, selectedCalendarIds = allIds)
            refreshEvents(
                cals,
                allIds,
                _uiState.value.selectedDay
            )
            WidgetHelper.updateAllWidgets(getApplication())
        }
    }

    fun deselectAllCalendars() {
        viewModelScope.launch {
            val empty = emptySet<Long>()
            prefs.saveSelectedCalendarIds(empty)
            _uiState.value = _uiState.value.copy(selectedCalendarIds = empty)
            refreshEvents(
                _uiState.value.calendars,
                empty,
                _uiState.value.selectedDay
            )
            WidgetHelper.updateAllWidgets(getApplication())
        }
    }

    fun updateCustomizationForType(type: com.example.model.WidgetType, customization: WidgetCustomization) {
        viewModelScope.launch {
            prefs.updateWidgetCustomizationForType(type, customization)
            val updatedMap = prefs.allCustomizationsFlow.first()
            _uiState.value = _uiState.value.copy(
                customizations = updatedMap,
                customization = customization
            )
            refreshEvents(
                _uiState.value.calendars,
                _uiState.value.selectedCalendarIds,
                _uiState.value.selectedDay
            )
            WidgetHelper.updateAllWidgets(getApplication())
        }
    }

    fun updateCustomization(customization: WidgetCustomization) {
        viewModelScope.launch {
            prefs.updateWidgetCustomization(customization)
            _uiState.value = _uiState.value.copy(customization = customization)
            refreshEvents(
                _uiState.value.calendars,
                _uiState.value.selectedCalendarIds,
                _uiState.value.selectedDay
            )
            WidgetHelper.updateAllWidgets(getApplication())
        }
    }

    fun selectDay(cal: Calendar) {
        _uiState.value = _uiState.value.copy(selectedDay = cal)
        refreshEvents(
            _uiState.value.calendars,
            _uiState.value.selectedCalendarIds,
            cal
        )
    }

    fun openEventDetail(event: CalendarEvent) {
        _uiState.value = _uiState.value.copy(selectedEventForDetail = event)
    }

    fun highlightEvent(eventId: Long) {
        _uiState.value = _uiState.value.copy(highlightedEventId = eventId)
    }

    fun dismissEventDetail() {
        _uiState.value = _uiState.value.copy(selectedEventForDetail = null)
    }

    fun showAddEventSheet() {
        _uiState.value = _uiState.value.copy(isAddEventSheetVisible = true)
    }

    fun dismissAddEventSheet() {
        _uiState.value = _uiState.value.copy(isAddEventSheetVisible = false)
    }

    fun addNewEvent(
        calendarId: Long,
        title: String,
        location: String?,
        description: String?,
        startMillis: Long,
        endMillis: Long,
        isAllDay: Boolean
    ) {
        viewModelScope.launch {
            repository.insertEvent(
                context = getApplication(),
                calendarId = calendarId,
                title = title,
                location = location,
                description = description,
                startMillis = startMillis,
                endMillis = endMillis,
                isAllDay = isAllDay
            )

            refreshEvents(
                _uiState.value.calendars,
                _uiState.value.selectedCalendarIds,
                _uiState.value.selectedDay
            )
            WidgetHelper.updateAllWidgets(getApplication())
        }
    }

    fun openCalendarAtDate(timeMillis: Long) {
        repository.openCalendarAtDate(getApplication(), timeMillis)
    }

    fun openCalendarEvent(event: CalendarEvent) {
        repository.openCalendarEvent(getApplication(), event.id, event.startMillis)
    }

    private fun refreshEvents(
        calendars: List<CalendarAccount>,
        selectedIds: Set<Long>,
        targetDay: Calendar
    ) {
        val events = repository.getEventsForDay(
            context = getApplication(),
            selectedCalendarIds = selectedIds,
            targetCalendar = targetDay,
            hidePastEvents = false
        )
        _uiState.value = _uiState.value.copy(
            calendars = calendars,
            events = events
        )
    }
}
