package com.vika.quest.ui.mentor

import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vika.quest.ai.AiMentorReplyValidator
import com.vika.quest.ai.AiQuestDraftValidator
import com.vika.quest.ai.ContextBuilder
import com.vika.quest.ai.FakeAiProvider
import com.vika.quest.data.local.QuestDatabase
import com.vika.quest.data.repository.GoalRepository
import com.vika.quest.data.repository.MemoryRepository
import com.vika.quest.data.repository.ProjectRepository
import com.vika.quest.data.repository.QuestRepository
import com.vika.quest.data.repository.UserPreferenceRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MentorChatViewModelTest {
    private lateinit var db: QuestDatabase

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, QuestDatabase::class.java)
            .allowMainThreadQueries().build()
    }

    @After fun close() = db.close()

    @Test fun conversationSurvivesRecreationAndRapidGenerateCreatesOneQuest() = runBlocking {
        val goals = GoalRepository(db.goalDao())
        goals.createGoal("做产品", "验证行动规划产品")
        val quests = QuestRepository(db)
        val builder = ContextBuilder(goals, ProjectRepository(db.projectDao()), MemoryRepository(db.memoryDao()), quests, UserPreferenceRepository(db.userPreferenceDao()))
        val handle = SavedStateHandle()
        val first = viewModel(handle, builder, quests)
        withTimeout(2_000) { first.uiState.first { it.messages.isNotEmpty() && !it.isLoading } }
        first.setInput("我想先验证真实需求")
        first.send(); first.send(); first.send()
        val answered = withTimeout(2_000) { first.uiState.first { it.userTurns == 1 && !it.isLoading } }
        assertEquals(1, answered.userTurns)

        val restored = viewModel(handle, builder, quests)
        assertEquals(answered.messages, restored.uiState.value.messages)
        assertFalse(restored.uiState.value.isLoading)
        restored.generateQuest(); restored.generateQuest(); restored.generateQuest()
        withTimeout(2_000) { restored.uiState.first { it.generatedQuestId != null } }
        assertEquals(1, quests.getRecentQuests(10).size)
    }

    private fun viewModel(handle: SavedStateHandle, builder: ContextBuilder, quests: QuestRepository) = MentorChatViewModel(
        handle, builder, quests, FakeAiProvider(), AiMentorReplyValidator(), AiQuestDraftValidator(),
        "验证 Quest 产品需求", 15, 3, listOf("PHONE"),
    )
}
