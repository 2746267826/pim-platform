package com.pim.app.ui.search

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

data class SearchUiState(
    val query: String = "",
    val typeFilter: String? = null,
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    fun updateQuery(v: String) { _state.value = _state.value.copy(query = v) }

    fun filterByType(type: String?) {
        _state.value = _state.value.copy(typeFilter = type)
        search()
    }

    fun search() {
        val q = _state.value.query.trim()
        if (q.isBlank()) return
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val res = api.search(q, _state.value.typeFilter)
                if (res.code == 0) {
                    _state.value = _state.value.copy(results = res.data ?: emptyList(), isLoading = false)
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = res.message)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "搜索失败: ${e.message}")
            }
        }
    }
}
