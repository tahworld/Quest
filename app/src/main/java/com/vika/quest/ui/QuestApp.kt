package com.vika.quest.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vika.quest.di.AppContainer
import com.vika.quest.ui.home.HomeScreen
import com.vika.quest.ui.home.HomeViewModel
import com.vika.quest.ui.onboarding.OnboardingScreen
import com.vika.quest.ui.onboarding.OnboardingViewModel
import com.vika.quest.ui.quest.QuestDetailScreen
import com.vika.quest.ui.quest.QuestDetailViewModel

@Composable
fun QuestApp(container: AppContainer) {
    val appViewModel: AppViewModel = viewModel(
        factory = remember(container) {
            ViewModelFactory { AppViewModel(container.goalRepository) }
        },
    )
    val startRoute by appViewModel.startRoute.collectAsStateWithLifecycle()

    if (startRoute == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = checkNotNull(startRoute),
    ) {
        composable(Routes.ONBOARDING) {
            val onboardingViewModel: OnboardingViewModel = viewModel(
                factory = remember(container) {
                    ViewModelFactory { OnboardingViewModel(container.goalRepository) }
                },
            )
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = remember(container) {
                    ViewModelFactory {
                        HomeViewModel(
                            goalRepository = container.goalRepository,
                            questRepository = container.questRepository,
                            aiProvider = container.aiProvider,
                            validator = container.aiQuestDraftValidator,
                        )
                    }
                },
            )
            HomeScreen(
                viewModel = homeViewModel,
                onQuestGenerated = { questId ->
                    navController.navigate(Routes.quest(questId))
                    homeViewModel.consumeGeneratedQuest(questId)
                },
            )
        }

        composable(
            route = Routes.QUEST,
            arguments = listOf(navArgument("questId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val questId = checkNotNull(backStackEntry.arguments?.getString("questId"))
            val questViewModel: QuestDetailViewModel = viewModel(
                factory = remember(container, questId) {
                    ViewModelFactory {
                        QuestDetailViewModel(
                            questId = questId,
                            goalRepository = container.goalRepository,
                            questRepository = container.questRepository,
                        )
                    }
                },
            )
            QuestDetailScreen(
                viewModel = questViewModel,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
