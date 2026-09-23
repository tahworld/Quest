package com.vika.quest.ui.project

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vika.quest.data.local.entity.ProjectEntity
import com.vika.quest.data.repository.ProjectRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProjectUiState(val projects: List<ProjectEntity> = emptyList(), val editingId: String? = null, val name: String = "", val description: String = "", val currentState: String = "", val error: String? = null)
class ProjectViewModel(private val repository: ProjectRepository) : ViewModel() {
    private val _state = MutableStateFlow(ProjectUiState()); val state = _state.asStateFlow()
    init { viewModelScope.launch { repository.observeProjects().collect { list -> _state.update { it.copy(projects = list) } } } }
    fun edit(project: ProjectEntity?) = _state.update { it.copy(editingId = project?.id, name = project?.name.orEmpty(), description = project?.description.orEmpty(), currentState = project?.currentState.orEmpty(), error = null) }
    fun name(value: String) = _state.update { it.copy(name = value) }; fun description(value: String) = _state.update { it.copy(description = value) }; fun currentState(value: String) = _state.update { it.copy(currentState = value) }
    fun save() { val s = _state.value; viewModelScope.launch { runCatching { repository.save(s.editingId, s.name, s.description, s.currentState) }.onSuccess { edit(null) }.onFailure { e -> _state.update { it.copy(error = e.message) } } } }
}
