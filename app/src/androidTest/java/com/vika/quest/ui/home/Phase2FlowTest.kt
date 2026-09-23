package com.vika.quest.ui.home

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vika.quest.ai.AiQuestDraftValidator
import com.vika.quest.ai.FakeAiProvider
import com.vika.quest.data.local.QuestDatabase
import com.vika.quest.data.repository.GoalRepository
import com.vika.quest.data.repository.QuestRepository
import com.vika.quest.model.QuestStatus
import com.vika.quest.ui.quest.QuestDetailViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase2FlowTest {
    private lateinit var database: QuestDatabase
    private lateinit var goalRepository: GoalRepository
    private lateinit var questRepository: QuestRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            QuestDatabase::class.java,
        ).allowMainThreadQueries().build()
        goalRepository = GoalRepository(database.goalDao())
        questRepository = QuestRepository(database.questDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun repeatedGenerationTap_createsOneQuest_andStartPersistsActiveStatus() = runBlocking {
        goalRepository.createGoal(
            name = "提升商业能力",
            description = "提升商业能力",
        )
        val homeViewModel = HomeViewModel(
            goalRepository = goalRepository,
            questRepository = questRepository,
            aiProvider = FakeAiProvider(),
            validator = AiQuestDraftValidator(),
        )

        withTimeout(2_000) {
            homeViewModel.uiState.first { it.goal != null }
        }
        homeViewModel.generateQuest()
        homeViewModel.generateQuest()

        val questId = withTimeout(2_000) {
            homeViewModel.uiState.first { it.generatedQuestId != null }.generatedQuestId
        }
        assertEquals(1, questRepository.observeQuests().first().size)

        val detailViewModel = QuestDetailViewModel(
            questId = checkNotNull(questId),
            goalRepository = goalRepository,
            questRepository = questRepository,
        )
        withTimeout(2_000) {
            detailViewModel.uiState.first { !it.isLoading && it.quest != null }
        }
        detailViewModel.startQuest()

        val activeQuest = withTimeout(2_000) {
            questRepository.observeQuests().first { quests ->
                quests.singleOrNull()?.status == QuestStatus.ACTIVE
            }.single()
        }
        assertEquals(QuestStatus.ACTIVE, activeQuest.status)
    }
}
