package com.vika.quest.ui.quest

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vika.quest.ai.*
import com.vika.quest.data.local.QuestDatabase
import com.vika.quest.data.repository.*
import com.vika.quest.model.NewQuest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActionReviewFlowTest {
    private lateinit var db: QuestDatabase
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, QuestDatabase::class.java).allowMainThreadQueries().build() }
    @After fun close() = db.close()

    @Test fun answerRequiresConfirmationToChangeQuestAndSurvivesViewModelRecreation() = runBlocking {
        val goals = GoalRepository(db.goalDao())
        val goal = goals.createGoal("读手头教材", "阅读/学习：读手头教材")
        val quests = QuestRepository(db)
        val quest = quests.createQuest(NewQuest(goal.id, title = "从教材复述一个观点", reason = "承接阅读", steps = listOf("打开教材并复述一段"),
            instruction = "打开教材并复述一段", estimatedMinutes = 5, completionCriteria = listOf("留下一个观点"),
            expectedOutput = "一条观点笔记", sourceIntention = "读手头教材", sourceAvailableMinutes = 5, sourceEnergy = 3,
            sourceResources = listOf("PHONE")))
        val provider = CountingProvider()
        val handle = SavedStateHandle()
        val first = viewModel(quest.id, goals, quests, provider, handle)
        withTimeout(2_000) { first.uiState.first { it.quest != null } }
        first.openReview()
        first.setReviewQuestion("现在没时间，能缩小吗？")
        first.askReviewQuestion(); first.askReviewQuestion()
        val answered = withTimeout(2_000) { first.uiState.first { it.reviewReply?.proposedQuest != null } }
        assertEquals(1, provider.reviewCalls)
        assertEquals("从教材复述一个观点", quests.getQuest(quest.id)?.title)
        assertEquals(1, answered.reviewHistory.size)

        val restored = viewModel(quest.id, goals, quests, provider, handle)
        assertTrue(restored.uiState.value.reviewOpen)
        assertEquals(1, restored.uiState.value.reviewHistory.size)
        assertNotNull(restored.uiState.value.reviewReply?.proposedQuest)
        withTimeout(2_000) { restored.uiState.first { it.quest != null } }
        restored.acceptReviewAdjustment(); restored.acceptReviewAdjustment()
        withTimeout(2_000) { restored.uiState.first { it.quest?.title?.startsWith("把") == true } }
        assertEquals(1, quests.observeQuests().first().size)
    }

    private fun viewModel(id: String, goals: GoalRepository, quests: QuestRepository, provider: AiProvider, handle: SavedStateHandle): QuestDetailViewModel {
        val projects = ProjectRepository(db.projectDao())
        val memories = MemoryRepository(db.memoryDao())
        val builder = ContextBuilder(goals, projects, memories, quests, UserPreferenceRepository(db.userPreferenceDao()))
        val validator = AiQuestDraftValidator()
        return QuestDetailViewModel(id, goals, projects, memories, quests, builder, provider, validator, AiQuestResultValidator(), AiActionReviewValidator(validator), handle)
    }

    private class CountingProvider : AiProvider by FakeAiProvider() {
        var reviewCalls = 0
        override suspend fun reviewQuest(context: QuestReviewContext): AiActionReviewReply {
            reviewCalls++
            return FakeAiProvider().reviewQuest(context)
        }
    }
}
