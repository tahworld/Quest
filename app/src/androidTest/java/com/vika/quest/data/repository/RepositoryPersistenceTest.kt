package com.vika.quest.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vika.quest.data.local.QuestDatabase
import com.vika.quest.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryPersistenceTest {
    private lateinit var db: QuestDatabase; private lateinit var goals: GoalRepository; private lateinit var quests: QuestRepository
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, QuestDatabase::class.java).allowMainThreadQueries().build(); goals = GoalRepository(db.goalDao()); quests = QuestRepository(db) }
    @After fun close() = db.close()
    @Test fun goalQuestResultAndRerollPersist() = runBlocking {
        val goal = goals.createGoal("建立商业能力", "发现真实需求", createdAt = 1)
        val first = quests.createQuest(newQuest(goal.id, "记录三个用户抱怨"), 2)
        val second = quests.reroll(first.id, RejectionReason.LOW_VALUE, null, newQuest(goal.id, "比较三个竞品定价"), 3)
        assertEquals(QuestStatus.ABANDONED, quests.getQuest(first.id)?.status); assertEquals(1, quests.getRecentRejections(5).size)
        quests.startQuest(second.id); quests.completeQuest(second.id, NewQuestResult("保存了一份价格对比表", 12, DifficultyRating.APPROPRIATE, 4), 4)
        assertEquals(QuestStatus.COMPLETED, quests.getQuest(second.id)?.status); assertEquals("保存了一份价格对比表", quests.getResult(second.id)?.resultText); assertEquals(2, quests.observeQuests().first().size)
    }
    private fun newQuest(goal: String, title: String) = NewQuest(goal, title = title, reason = "测试", steps = listOf("打开记录表", "写入三项证据"), instruction = "打开记录表并写入三项证据", estimatedMinutes = 15, completionCriteria = listOf("三项均已记录"), expectedOutput = "一份三项记录", sourceIntention = "研究市场", sourceAvailableMinutes = 15, sourceEnergy = 3, sourceResources = listOf("PHONE"))
}
