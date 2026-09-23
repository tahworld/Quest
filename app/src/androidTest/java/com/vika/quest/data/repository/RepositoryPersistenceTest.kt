package com.vika.quest.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vika.quest.data.local.QuestDatabase
import com.vika.quest.model.NewQuest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryPersistenceTest {
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
    fun goalAndQuest_arePersistedAndObservable() = runBlocking {
        val goal = goalRepository.createGoal(
            name = "建立商业能力",
            description = "学习发现真实需求",
            createdAt = 1L,
        )
        questRepository.createQuest(
            newQuest = NewQuest(
                goalId = goal.id,
                title = "收集三个用户抱怨",
                instruction = "搜索三个独立用户对现有产品的明确抱怨并记录证据。",
                estimatedMinutes = 15,
                completionCriteria = listOf("找到三个独立用户", "保存至少一项证据"),
                difficulty = 2,
            ),
            createdAt = 2L,
        )

        assertEquals(1, goalRepository.observeGoals().first().size)
        assertEquals(1, questRepository.observeQuests().first().size)
        assertEquals(goal.id, questRepository.observeQuests().first().single().goalId)
    }
}
