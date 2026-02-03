package app.lusk.underseerr.presentation.issue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lusk.underseerr.domain.model.Issue
import app.lusk.underseerr.domain.model.IssueCount
import app.lusk.underseerr.domain.model.IssueStatus
import app.lusk.underseerr.domain.repository.IssueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Issues screen.
 */
class IssueViewModel(
    private val issueRepository: IssueRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<IssueListState>(IssueListState.Loading)
    val uiState: StateFlow<IssueListState> = _uiState.asStateFlow()
    
    private val _issueCounts = MutableStateFlow<IssueCount?>(null)
    val issueCounts: StateFlow<IssueCount?> = _issueCounts.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow("open")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadIssues()
        loadIssueCounts()
    }
    
    fun loadIssues(filter: String = _selectedFilter.value, isRefresh: Boolean = false) {
        viewModelScope.launch {
            _selectedFilter.value = filter
            
            // Only show loading if we don't have any data yet
            if (_uiState.value !is IssueListState.Success && _uiState.value !is IssueListState.Empty) {
                _uiState.value = IssueListState.Loading
            }
            
            fetchIssues(filter)
        }
    }
    
    private suspend fun fetchIssues(filter: String) {
        _error.value = null
        issueRepository.getIssues(
            take = 50,
            skip = 0,
            filter = filter
        ).fold(
            onSuccess = { issues ->
                _uiState.value = if (issues.isEmpty()) {
                    IssueListState.Empty
                } else {
                    IssueListState.Success(issues)
                }
            },
            onFailure = { error ->
                // If we have data, keep it and just set the error
                if (_uiState.value is IssueListState.Success) {
                    _error.value = error.message ?: "Failed to load issues"
                } else {
                    _uiState.value = IssueListState.Error(
                        error.message ?: "Failed to load issues"
                    )
                }
            }
        )
    }
    
    fun loadIssueCounts() {
        viewModelScope.launch {
            fetchIssueCounts()
        }
    }
    
    private suspend fun fetchIssueCounts() {
        issueRepository.getIssueCounts().fold(
            onSuccess = { counts ->
                _issueCounts.value = counts
            },
            onFailure = { /* Silently fail for counts */ }
        )
    }
    
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _error.value = null // Clear previous error on refresh
            fetchIssues(_selectedFilter.value)
            fetchIssueCounts()
            _isRefreshing.value = false
        }
    }
    
    fun resolveIssue(issueId: Int) {
        viewModelScope.launch {
            issueRepository.resolveIssue(issueId).fold(
                onSuccess = { 
                    refresh()
                },
                onFailure = { 
                    _error.value = it.message ?: "Failed to resolve issue"
                }
            )
        }
    }
    
    fun reopenIssue(issueId: Int) {
        viewModelScope.launch {
            issueRepository.reopenIssue(issueId).fold(
                onSuccess = { 
                    refresh()
                },
                onFailure = { 
                    _error.value = it.message ?: "Failed to reopen issue"
                }
            )
        }
    }
    
    fun deleteIssue(issueId: Int) {
        viewModelScope.launch {
            issueRepository.deleteIssue(issueId).fold(
                onSuccess = { 
                    refresh()
                },
                onFailure = { 
                   _error.value = it.message ?: "Failed to delete issue"
                }
            )
        }
    }
}

/**
 * UI State for the Issues list.
 */
sealed class IssueListState {
    data object Loading : IssueListState()
    data object Empty : IssueListState()
    data class Success(val issues: List<Issue>) : IssueListState()
    data class Error(val message: String) : IssueListState()
}
