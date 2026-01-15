package app.lusk.client.presentation.issue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lusk.client.domain.model.Issue
import app.lusk.client.domain.model.IssueCount
import app.lusk.client.domain.model.IssueStatus
import app.lusk.client.domain.repository.IssueRepository
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
    
    init {
        loadIssues()
        loadIssueCounts()
    }
    
    fun loadIssues(filter: String = _selectedFilter.value) {
        viewModelScope.launch {
            _selectedFilter.value = filter
            _uiState.value = IssueListState.Loading
            
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
                    _uiState.value = IssueListState.Error(
                        error.message ?: "Failed to load issues"
                    )
                }
            )
        }
    }
    
    fun loadIssueCounts() {
        viewModelScope.launch {
            issueRepository.getIssueCounts().fold(
                onSuccess = { counts ->
                    _issueCounts.value = counts
                },
                onFailure = { /* Silently fail for counts */ }
            )
        }
    }
    
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            loadIssues(_selectedFilter.value)
            loadIssueCounts()
            _isRefreshing.value = false
        }
    }
    
    fun resolveIssue(issueId: Int) {
        viewModelScope.launch {
            issueRepository.resolveIssue(issueId).fold(
                onSuccess = { 
                    refresh()
                },
                onFailure = { /* Handle error */ }
            )
        }
    }
    
    fun reopenIssue(issueId: Int) {
        viewModelScope.launch {
            issueRepository.reopenIssue(issueId).fold(
                onSuccess = { 
                    refresh()
                },
                onFailure = { /* Handle error */ }
            )
        }
    }
    
    fun deleteIssue(issueId: Int) {
        viewModelScope.launch {
            issueRepository.deleteIssue(issueId).fold(
                onSuccess = { 
                    refresh()
                },
                onFailure = { /* Handle error */ }
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
