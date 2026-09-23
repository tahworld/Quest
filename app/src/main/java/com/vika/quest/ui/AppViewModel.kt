package com.vika.quest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vika.quest.data.repository.GoalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppViewModel(
    goalRepository: GoalRepository,
) : ViewModel() {
    private val _startRoute = MutableStateFlow<String?>(null)
    val startRoute: StateFlow<String?> = _startRoute.asStateFlow()

    init {
        viewModelScope.launch {
            val hasGoal = goalRepository.observeGoals().first().isNotEmpty()
            _startRoute.value = if (hasGoal) Routes.HOME else Routes.ONBOARDING
        }
    }
}

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val QUEST = "quest/{questId}"
    const val RESULT = "result/{questId}"
    const val PROJECTS = "projects"
    const val SETTINGS = "settings"

    fun quest(questId: String): String = "quest/$questId"
    fun result(questId: String): String = "result/$questId"
}
