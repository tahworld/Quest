package com.vika.quest.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vika.quest.ai.*
import com.vika.quest.data.local.QuestDatabase
import com.vika.quest.data.repository.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Phase3GenerationTest {
    private lateinit var db: QuestDatabase
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, QuestDatabase::class.java).allowMainThreadQueries().build() }
    @After fun close() = db.close()
    @Test fun rapidTapsCreateOneQuest() = runBlocking {
        val goals = GoalRepository(db.goalDao()); goals.createGoal("商业能力", "发现需求")
        val quests = QuestRepository(db)
        val builder = ContextBuilder(goals, ProjectRepository(db.projectDao()), MemoryRepository(db.memoryDao()), quests, UserPreferenceRepository(db.userPreferenceDao()))
        val vm = HomeViewModel(SavedStateHandle(), builder, quests, FakeAiProvider(), AiQuestDraftValidator(), AiClarificationValidator())
        vm.setIntention("研究 AI 产品如何获客"); vm.generateQuest(); vm.generateQuest(); vm.generateQuest()
        withTimeout(2_000) { vm.uiState.first { it.generatedQuestId != null } }
        assertEquals(1, quests.observeQuests().first().size)
    }

    @Test fun clarificationSurvivesRecreationAndGeneratesOneQuest() = runBlocking {
        val goals = GoalRepository(db.goalDao()); goals.createGoal("商业能力", "发现需求")
        val quests = QuestRepository(db)
        val builder = ContextBuilder(goals, ProjectRepository(db.projectDao()), MemoryRepository(db.memoryDao()), quests, UserPreferenceRepository(db.userPreferenceDao()))
        val handle = SavedStateHandle()
        val firstVm = HomeViewModel(handle, builder, quests, FakeAiProvider(), AiQuestDraftValidator(), AiClarificationValidator())
        firstVm.setIntention("研究 AI 产品如何获客")
        firstVm.startClarification()
        val firstQuestion = withTimeout(2_000) { firstVm.uiState.first { it.clarificationQuestion != null } }
        firstVm.setClarificationAnswer(firstQuestion.clarificationOptions.first())
        firstVm.submitClarificationAnswer()
        withTimeout(2_000) { firstVm.uiState.first { it.clarificationHistory.size == 1 && it.clarificationQuestion != null } }

        val restored = HomeViewModel(handle, builder, quests, FakeAiProvider(), AiQuestDraftValidator(), AiClarificationValidator())
        assertEquals(1, restored.uiState.value.clarificationHistory.size)
        restored.setClarificationAnswer(restored.uiState.value.clarificationOptions.first())
        restored.submitClarificationAnswer()
        withTimeout(2_000) { restored.uiState.first { it.generatedQuestId != null } }
        assertEquals(1, quests.observeQuests().first().size)
    }

    @Test fun clarificationStopsAtThreeQuestionsAndIgnoresRapidTaps() = runBlocking {
        val goals = GoalRepository(db.goalDao()); goals.createGoal("商业能力", "发现需求")
        val quests = QuestRepository(db)
        val builder = ContextBuilder(goals, ProjectRepository(db.projectDao()), MemoryRepository(db.memoryDao()), quests, UserPreferenceRepository(db.userPreferenceDao()))
        val provider = AlwaysAskProvider()
        val vm = HomeViewModel(SavedStateHandle(), builder, quests, provider, AiQuestDraftValidator(), AiClarificationValidator())
        vm.setIntention("研究 AI 产品如何获客")
        vm.startClarification(); vm.startClarification(); vm.startClarification()
        repeat(3) { index ->
            val state = withTimeout(2_000) { vm.uiState.first { it.clarificationQuestion != null && it.clarificationHistory.size == index } }
            vm.setClarificationAnswer(state.clarificationOptions.first())
            vm.submitClarificationAnswer()
        }
        withTimeout(2_000) { vm.uiState.first { it.generatedQuestId != null } }
        assertEquals(3, provider.clarificationCalls)
        assertEquals(1, quests.observeQuests().first().size)
    }

    private class AlwaysAskProvider : AiProvider {
        var clarificationCalls = 0
        private val fake = FakeAiProvider()
        override suspend fun clarifyQuest(context: QuestClarificationContext): AiClarificationTurn {
            clarificationCalls += 1
            return AiClarificationTurn(ClarificationStatus.ASK, "第 ${context.history.size + 1} 个关键选择是什么？", listOf("保留当前方向", "缩小行动范围"), true, null)
        }
        override suspend fun continueMentorConversation(context: MentorConversationContext) = fake.continueMentorConversation(context)
        override suspend fun generateQuest(context: QuestGenerationContext) = fake.generateQuest(context)
        override suspend fun analyzeQuestResult(context: QuestResultAnalysisContext) = fake.analyzeQuestResult(context)
        override suspend fun testConnection(settings: AiConnectionSettings) = AiConnectionResult(true, "测试")
    }
}
