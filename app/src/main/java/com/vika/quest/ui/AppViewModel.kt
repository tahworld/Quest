package com.vika.quest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vika.quest.data.repository.GoalRepository
import com.vika.quest.data.repository.UserPreferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppViewModel(
    goalRepository: GoalRepository,
    userPreferenceRepository: UserPreferenceRepository,
) : ViewModel() {
    private val _startRoute = MutableStateFlow<String?>(null)
    val startRoute: StateFlow<String?> = _startRoute.asStateFlow()

    init {
        viewModelScope.launch {
            val hasGoal = goalRepository.observeGoals().first().isNotEmpty()
            _startRoute.value = when {
                !hasGoal -> Routes.ONBOARDING
                !userPreferenceRepository.isPersonaComplete() -> Routes.PERSONA_SETUP
                else -> Routes.HOME
            }
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
    const val PERSONA_SETUP = "persona_setup"
    const val PERSONA_EDIT = "persona_edit"

    fun quest(questId: String): String = "quest/$questId"
    fun result(questId: String): String = "result/$questId"
}
