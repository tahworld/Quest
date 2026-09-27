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
import com.vika.quest.data.repository.UserPreferenceKeys
import com.vika.quest.model.NewQuest
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

    @Test
    fun mentorConversationContextStaysBounded() = runBlocking {
        val projects = ProjectRepository(db.projectDao())
        repeat(5) { projects.save(null, "项目$it", "描述$it", "状态$it", now = it.toLong()) }
        repeat(6) {
            db.memoryDao().upsert(MemoryEntity(UUID.randomUUID().toString(), null, null, "发现", "导师记忆$it", 5, it.toLong()))
        }
        val builder = ContextBuilder(
            GoalRepository(db.goalDao()), projects, MemoryRepository(db.memoryDao()), QuestRepository(db), UserPreferenceRepository(db.userPreferenceDao()),
        )
        val messages = List(20) { MentorMessage(if (it % 2 == 0) MentorMessageRole.USER else MentorMessageRole.MENTOR, "消息$it") }
        val context = builder.buildMentorConversation("讨论产品方向", 15, 3, setOf(QuestResource.PHONE), messages)
        assertEquals(2, context.projects.size)
        assertEquals(3, context.memories.size)
        assertEquals(12, context.messages.size)
    }

    @Test fun generationKeepsSelectedDirectionFirstAndBoundsHistory() = runBlocking {
        val goals = GoalRepository(db.goalDao())
        val all = (0..6).map { goals.createGoal("方向$it", "描述$it", createdAt = it.toLong()) }
        val preferences = UserPreferenceRepository(db.userPreferenceDao())
        preferences.save(UserPreferenceKeys.ACTIVE_DIRECTION_GOAL_ID, all.last().id)
        val quests = QuestRepository(db)
        repeat(12) { index ->
            quests.createQuest(NewQuest(all[index % all.size].id, title = "记录第 $index 个具体问题", reason = "承接方向",
                steps = listOf("写下一条可核实的事实"), instruction = "写下一条可核实的事实", estimatedMinutes = 5,
                completionCriteria = listOf("留下事实"), expectedOutput = "一条记录", sourceIntention = "", sourceAvailableMinutes = 5,
                sourceEnergy = 3, sourceResources = listOf("PHONE")), createdAt = index.toLong())
        }
        val builder = ContextBuilder(goals, ProjectRepository(db.projectDao()), MemoryRepository(db.memoryDao()), quests, preferences)
        val context = builder.build("", 5, 3, setOf(QuestResource.PHONE))
        assertEquals(all.last().id, context.currentDirection?.id)
        assertEquals(all.last().id, context.goals.first().id)
        assertEquals(ContextBuilder.GOAL_LIMIT, context.goals.size)
        assertEquals(ContextBuilder.QUEST_LIMIT, context.recentQuests.size)
    }

    @Test fun reviewContextKeepsOnlyThreeRelevantMemoriesAndThreeAnswers() = runBlocking {
        val goals = GoalRepository(db.goalDao())
        val target = goals.createGoal("读教材", "阅读/学习：读教材")
        val other = goals.createGoal("其他", "不相关方向")
        repeat(8) { index ->
            db.memoryDao().upsert(MemoryEntity(UUID.randomUUID().toString(), if (index % 2 == 0) target.id else other.id,
                null, "线索", "第 $index 条参考信息", 5, index.toLong()))
        }
        val quests = QuestRepository(db)
        val quest = quests.createQuest(NewQuest(target.id, title = "从教材中复述一段", reason = "承接阅读",
            steps = listOf("打开教材"), instruction = "打开教材", estimatedMinutes = 5,
            completionCriteria = listOf("写下一条笔记"), expectedOutput = "一条笔记", sourceIntention = "读教材",
            sourceAvailableMinutes = 5, sourceEnergy = 3, sourceResources = listOf("PHONE")))
        val builder = ContextBuilder(goals, ProjectRepository(db.projectDao()), MemoryRepository(db.memoryDao()), quests, UserPreferenceRepository(db.userPreferenceDao()))
        val review = builder.buildReview(quest, "这一步为什么合适？", List(5) { ActionReviewExchange("问题$it", "回答$it") })
        assertEquals(ContextBuilder.REVIEW_HISTORY_LIMIT, review.history.size)
        assertEquals(ContextBuilder.REVIEW_MEMORY_LIMIT, review.memories.size)
    }
}
