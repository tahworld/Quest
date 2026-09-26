package com.vika.quest.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.vika.quest.di.AppContainer
import com.vika.quest.ui.home.*
import com.vika.quest.ui.onboarding.*
import com.vika.quest.ui.persona.*
import com.vika.quest.ui.project.*
import com.vika.quest.ui.quest.*
import com.vika.quest.ui.result.*
import com.vika.quest.ui.settings.*

@Composable fun QuestApp(container: AppContainer) {
    val appVm: AppViewModel = viewModel(factory = remember(container) { ViewModelFactory { _ -> AppViewModel(container.goalRepository, container.userPreferenceRepository) } })
    val start by appVm.startRoute.collectAsStateWithLifecycle()
    if (start == null) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = checkNotNull(start)) {
        composable(Routes.ONBOARDING) {
            val vm: OnboardingViewModel = viewModel(factory = remember(container) { ViewModelFactory { _ -> OnboardingViewModel(container.goalRepository) } })
            OnboardingScreen(vm) { nav.navigate(Routes.PERSONA_SETUP) { popUpTo(Routes.ONBOARDING) { inclusive = true }; launchSingleTop = true } }
        }
        composable(Routes.PERSONA_SETUP) {
            val vm: PersonaViewModel = viewModel(factory = remember(container) { ViewModelFactory { _ -> PersonaViewModel(container.userPreferenceRepository) } })
            PersonaScreen(vm, isEditing = false, onComplete = { nav.navigate(Routes.HOME) { popUpTo(Routes.PERSONA_SETUP) { inclusive = true }; launchSingleTop = true } }, onBack = {})
        }
        composable(Routes.PERSONA_EDIT) {
            val vm: PersonaViewModel = viewModel(factory = remember(container) { ViewModelFactory { _ -> PersonaViewModel(container.userPreferenceRepository) } })
            PersonaScreen(vm, isEditing = true, onComplete = { nav.popBackStack() }, onBack = { nav.popBackStack() })
        }
        composable(Routes.HOME) { entry ->
            val vm: HomeViewModel = viewModel(factory = remember(container) { ViewModelFactory { extras -> HomeViewModel(extras.createSavedStateHandle(), container.contextBuilder, container.questRepository, container.aiProvider, container.aiQuestDraftValidator, container.aiClarificationValidator) } })
            val transfer by entry.savedStateHandle.getStateFlow("conditions_nonce", 0L).collectAsStateWithLifecycle()
            LaunchedEffect(transfer) { if (transfer > 0) vm.applyConditions(entry.savedStateHandle[HomeViewModel.INTENTION] ?: "", entry.savedStateHandle[HomeViewModel.MINUTES] ?: 15, entry.savedStateHandle[HomeViewModel.ENERGY] ?: 3, entry.savedStateHandle.get<ArrayList<String>>(HomeViewModel.RESOURCES).orEmpty()) }
            HomeScreen(vm, onQuestGenerated = { id -> nav.navigate(Routes.quest(id)); vm.consumeGeneratedQuest(id) }, onProjects = { nav.navigate(Routes.PROJECTS) }, onPersona = { nav.navigate(Routes.PERSONA_EDIT) }, onSettings = { nav.navigate(Routes.SETTINGS) })
        }
        composable(Routes.QUEST, arguments = listOf(navArgument("questId") { type = NavType.StringType })) { entry ->
            val id = checkNotNull(entry.arguments?.getString("questId"))
            val vm: QuestDetailViewModel = viewModel(factory = remember(container, id) { ViewModelFactory { _ -> QuestDetailViewModel(id, container.goalRepository, container.projectRepository, container.memoryRepository, container.questRepository, container.contextBuilder, container.aiProvider, container.aiQuestDraftValidator, container.aiQuestResultValidator) } })
            QuestDetailScreen(vm, onBack = { nav.popBackStack() }, onAdjusted = { intention, minutes, energy, resources -> transferToHome(nav, intention, minutes, energy, resources) }, onReplaced = { newId -> nav.popBackStack(); nav.navigate(Routes.quest(newId)) }, onResult = { nav.navigate(Routes.result(it)) }, onAbandoned = { nav.popBackStack() })
        }
        composable(Routes.RESULT, arguments = listOf(navArgument("questId") { type = NavType.StringType })) { entry ->
            val id = checkNotNull(entry.arguments?.getString("questId")); val vm: ResultViewModel = viewModel(factory = remember(container, id) { ViewModelFactory { _ -> ResultViewModel(id, container.questRepository) } })
            ResultScreen(vm, onDone = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME); launchSingleTop = true } }, onContinue = { next -> transferToHome(nav, next, 15, 3, listOf("PHONE")) })
        }
        composable(Routes.PROJECTS) { val vm: ProjectViewModel = viewModel(factory = remember(container) { ViewModelFactory { _ -> ProjectViewModel(container.projectRepository) } }); ProjectScreen(vm) { nav.popBackStack() } }
        composable(Routes.SETTINGS) { val vm: AiSettingsViewModel = viewModel(factory = remember(container) { ViewModelFactory { _ -> AiSettingsViewModel(container.aiSettingsStore, container.aiProvider) } }); AiSettingsScreen(vm) { nav.popBackStack() } }
    }
}

private fun transferToHome(nav: androidx.navigation.NavHostController, intention: String, minutes: Int, energy: Int, resources: List<String>) {
    val entry = runCatching { nav.getBackStackEntry(Routes.HOME) }.getOrNull()
    if (entry != null) {
        entry.savedStateHandle[HomeViewModel.INTENTION] = intention; entry.savedStateHandle[HomeViewModel.MINUTES] = minutes; entry.savedStateHandle[HomeViewModel.ENERGY] = energy; entry.savedStateHandle[HomeViewModel.RESOURCES] = ArrayList(resources); entry.savedStateHandle["conditions_nonce"] = System.nanoTime()
        nav.navigate(Routes.HOME) { popUpTo(Routes.HOME); launchSingleTop = true }
    } else nav.navigate(Routes.HOME)
}
