package com.vika.quest.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.vika.quest.data.local.QuestDatabase
import com.vika.quest.ui.AppViewModel
import com.vika.quest.ui.Routes
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserPersonaPersistenceTest {
    private lateinit var db: QuestDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, QuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After fun close() = db.close()

    @Test
    fun personaPersistsInExistingPreferencesTable() = runBlocking {
        val repository = UserPreferenceRepository(db.userPreferenceDao())
        assertFalse(repository.isPersonaComplete())
        repository.savePersona(UserPersona("消防员，准备机械考研", "完成 Quest Android 产品", "直接具体", "空泛鼓励"))
        val restored = repository.getPersona()
        assertTrue(repository.isPersonaComplete())
        assertEquals("消防员，准备机械考研", restored.identityAndStage)
        assertEquals("直接具体", restored.guidanceStyle)
    }

    @Test
    fun startRouteRequiresGoalThenPersona() = runBlocking {
        val goals = GoalRepository(db.goalDao())
        val preferences = UserPreferenceRepository(db.userPreferenceDao())
        assertEquals(Routes.ONBOARDING, route(goals, preferences))
        goals.createGoal("推进个人成长", "推进个人成长")
        assertEquals(Routes.PERSONA_SETUP, route(goals, preferences))
        preferences.savePersona(UserPersona(identityAndStage = "正在推进长期项目"))
        assertEquals(Routes.HOME, route(goals, preferences))
    }

    private suspend fun route(goals: GoalRepository, preferences: UserPreferenceRepository): String =
        withTimeout(2_000) { AppViewModel(goals, preferences).startRoute.first { it != null }!! }
}
