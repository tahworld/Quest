package com.vika.quest.ai

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vika.quest.data.local.QuestDatabase
import com.vika.quest.data.local.entity.MemoryEntity
import com.vika.quest.data.repository.GoalRepository
import com.vika.quest.data.repository.MemoryRepository
import com.vika.quest.data.repository.ProjectRepository
import com.vika.quest.data.repository.QuestRepository
import com.vika.quest.data.repository.UserPersona
import com.vika.quest.data.repository.UserPreferenceRepository
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClarificationContextBoundsTest {
    private lateinit var db: QuestDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, QuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After fun close() = db.close()

    @Test
    fun clarificationContextStaysBounded() = runBlocking {
        val projects = ProjectRepository(db.projectDao())
        repeat(5) { projects.save(null, "项目$it", "描述$it", "状态$it", now = it.toLong()) }
        repeat(6) {
            db.memoryDao().upsert(MemoryEntity(UUID.randomUUID().toString(), null, null, "发现", "这是一条用于测试的长期记忆内容$it", 5, it.toLong()))
        }
        val preferences = UserPreferenceRepository(db.userPreferenceDao())
        preferences.savePersona(UserPersona(identityAndStage = "消防员，正在准备机械考研"))
        val builder = ContextBuilder(GoalRepository(db.goalDao()), projects, MemoryRepository(db.memoryDao()), QuestRepository(db), preferences)
        val history = List(5) { ClarificationExchange("问题$it", "回答$it") }
        val context = builder.buildClarification("研究 AI 产品获客", 15, 3, setOf(QuestResource.PHONE), history)
        assertEquals(ContextBuilder.CLARIFICATION_PROJECT_LIMIT, context.projects.size)
        assertEquals(ContextBuilder.CLARIFICATION_MEMORY_LIMIT, context.memories.size)
        assertEquals(ContextBuilder.CLARIFICATION_HISTORY_LIMIT, context.history.size)
    }
}
