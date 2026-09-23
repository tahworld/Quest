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
import org.junit.*
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
        val vm = HomeViewModel(SavedStateHandle(), builder, quests, FakeAiProvider(), AiQuestDraftValidator())
        vm.setIntention("研究 AI 产品如何获客"); vm.generateQuest(); vm.generateQuest(); vm.generateQuest()
        withTimeout(2_000) { vm.uiState.first { it.generatedQuestId != null } }
        assertEquals(1, quests.observeQuests().first().size)
    }
}
