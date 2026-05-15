package com.pim.app.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pim.core.models.*
import com.pim.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val events: List<EventResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditorOpen: Boolean = false,
    val selectedEvent: EventResponse? = null,
    val editorTitle: String = "",
    val editorDescription: String = "",
    val editorLocation: String = "",
    val editorStart: String = "",
    val editorEnd: String = "",
    val isImporting: Boolean = false,
    val icsInput: String = ""
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    init { loadEvents() }

    fun loadEvents() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        val month = _state.value.currentMonth
        val start = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC).toString()
        val end = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC).toString()
        viewModelScope.launch {
            try {
                val res = api.getEvents(start, end)
                if (res.code == 0) {
                    _state.value = _state.value.copy(events = res.data ?: emptyList(), isLoading = false)
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = res.message)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "加载失败: ${e.message}")
            }
        }
    }

    fun previousMonth() {
        _state.value = _state.value.copy(currentMonth = _state.value.currentMonth.minusMonths(1))
        loadEvents()
    }

    fun nextMonth() {
        _state.value = _state.value.copy(currentMonth = _state.value.currentMonth.plusMonths(1))
        loadEvents()
    }

    fun openCreateEditor(date: LocalDate? = null) {
        val d = date ?: LocalDate.now()
        val start = d.atTime(9, 0).format(fmt)
        val end = d.atTime(10, 0).format(fmt)
        _state.value = _state.value.copy(
            isEditorOpen = true, selectedEvent = null,
            editorTitle = "", editorDescription = "", editorLocation = "",
            editorStart = start, editorEnd = end
        )
    }

    fun openEditEditor(event: EventResponse) {
        _state.value = _state.value.copy(
            isEditorOpen = true, selectedEvent = event,
            editorTitle = event.title, editorDescription = event.description ?: "",
            editorLocation = event.location ?: "",
            editorStart = event.dtStart, editorEnd = event.dtEnd
        )
    }

    fun closeEditor() { _state.value = _state.value.copy(isEditorOpen = false) }

    fun updateEditorTitle(v: String) { _state.value = _state.value.copy(editorTitle = v) }
    fun updateEditorDescription(v: String) { _state.value = _state.value.copy(editorDescription = v) }
    fun updateEditorLocation(v: String) { _state.value = _state.value.copy(editorLocation = v) }
    fun updateEditorStart(v: String) { _state.value = _state.value.copy(editorStart = v) }
    fun updateEditorEnd(v: String) { _state.value = _state.value.copy(editorEnd = v) }

    fun saveEvent() {
        val s = _state.value
        if (s.editorTitle.isBlank()) return
        _state.value = _state.value.copy(isLoading = true, error = null)
        val body = CreateEventRequest(
            calendarId = "",
            title = s.editorTitle,
            description = s.editorDescription.ifBlank { null },
            location = s.editorLocation.ifBlank { null },
            dtStart = s.editorStart,
            dtEnd = s.editorEnd
        )
        viewModelScope.launch {
            try {
                if (s.selectedEvent != null) {
                    api.updateEvent(s.selectedEvent.id, body)
                } else {
                    val cals = api.getCalendars()
                    val calId = cals.data?.firstOrNull()?.id ?: run {
                        _state.value = _state.value.copy(isLoading = false, error = "请先创建日历")
                        return@launch
                    }
                    api.createEvent(body.copy(calendarId = calId))
                }
                _state.value = _state.value.copy(isLoading = false, isEditorOpen = false)
                loadEvents()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "保存失败: ${e.message}")
            }
        }
    }

    fun deleteEvent(id: String) {
        viewModelScope.launch {
            try {
                api.deleteEvent(id)
                loadEvents()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "删除失败: ${e.message}")
            }
        }
    }

    fun getEventsForDay(day: Int): List<EventResponse> {
        val month = _state.value.currentMonth
        val date = month.atDay(day)
        return _state.value.events.filter {
            try {
                val start = LocalDate.parse(it.dtStart.substring(0, 10))
                val end = LocalDate.parse(it.dtEnd.substring(0, 10))
                !date.isBefore(start) && !date.isAfter(end)
            } catch (_: Exception) { false }
        }
    }
}
