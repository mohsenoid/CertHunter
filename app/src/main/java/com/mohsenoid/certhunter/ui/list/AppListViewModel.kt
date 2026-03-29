package com.mohsenoid.certhunter.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohsenoid.certhunter.domain.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppListViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AppListUiModel())
    val uiState: StateFlow<AppListUiModel> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = repository.getInstalledApps()
            _uiState.update { it.copy(allApps = apps, isLoadingApps = false) }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onToggleSystemApps() {
        _uiState.update { it.copy(showSystemApps = !it.showSystemApps) }
    }
}
