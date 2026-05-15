package com.pim.app.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pim.core.models.*
import com.pim.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskListUiState(
    val tasks: List<TaskResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditorOpen: Boolean = false,
    val editorTitle: String = "",
    val editorDescription: String = "",
    val editorPriority: Int = 0,
    val showInboxOnly: Boolean = false
)

@HiltViewModel
class TaskListViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _state = MutableStateFlow(TaskListUiState())
    val state: StateFlow<TaskListUiState> = _state.asStateFlow()

    init { loadTasks() }

    fun loadTasks() {
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val inbox = if (_state.value.showInboxOnly) true else null
                val res = api.getTasks(inbox)
                if (res.code == 0) {
                    _state.value = _state.value.copy(tasks = res.data ?: emptyList(), isLoading = false)
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = res.message)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "加载失败: ${e.message}")
            }
        }
    }

    fun openCreateEditor() {
        _state.value = _state.value.copy(
            isEditorOpen = true, editorTitle = "", editorDescription = "", editorPriority = 0
        )
    }

    fun closeEditor() { _state.value = _state.value.copy(isEditorOpen = false) }
    fun updateEditorTitle(v: String) { _state.value = _state.value.copy(editorTitle = v) }
    fun updateEditorDescription(v: String) { _state.value = _state.value.copy(editorDescription = v) }
    fun updateEditorPriority(v: Int) { _state.value = _state.value.copy(editorPriority = v) }

    fun saveTask() {
        val s = _state.value
        if (s.editorTitle.isBlank()) return
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                api.createTask(CreateTaskRequest(
                    title = s.editorTitle,
                    description = s.editorDescription.ifBlank { null },
                    priority = s.editorPriority
                ))
                _state.value = _state.value.copy(isLoading = false, isEditorOpen = false)
                loadTasks()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "保存失败: ${e.message}")
            }
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            try {
                api.deleteTask(id)
                loadTasks()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "删除失败: ${e.message}")
            }
        }
    }

    fun toggleInbox() {
        _state.value = _state.value.copy(showInboxOnly = !_state.value.showInboxOnly)
        loadTasks()
    }
}
