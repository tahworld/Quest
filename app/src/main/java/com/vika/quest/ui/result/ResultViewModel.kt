package com.vika.quest.ui.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vika.quest.data.local.entity.QuestResultEntity
import com.vika.quest.data.repository.QuestRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ResultUiState(val result: QuestResultEntity? = null, val loading: Boolean = true)
class ResultViewModel(questId: String, repository: QuestRepository) : ViewModel() { private val _state = MutableStateFlow(ResultUiState()); val state = _state.asStateFlow(); init { viewModelScope.launch { repository.observeResult(questId).collect { _state.value = ResultUiState(it, false) } } } }
