package com.vika.quest.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAiProviderTest {
    private val provider = FakeAiProvider()

    @Test
    fun generateQuest_returnsOneDraftThatFitsAvailableTime() = runBlocking {
        val context = context(availableMinutes = 5)

        val draft = provider.generateQuest(context)

        assertEquals("goal-1", draft.goalId)
        assertEquals(5, draft.estimatedMinutes)
        assertTrue(draft.completionCriteria.isNotEmpty())
    }

    @Test
    fun generatedDraft_passesValidation() = runBlocking {
        val context = context(availableMinutes = 15)

        val newQuest = AiQuestDraftValidator().validate(
            draft = provider.generateQuest(context),
            context = context,
        )

        assertEquals("goal-1", newQuest.goalId)
        assertEquals(15, newQuest.estimatedMinutes)
    }

    private fun context(availableMinutes: Int) = QuestGenerationContext(
        activeGoals = listOf(
            GoalSnapshot(
                id = "goal-1",
                name = "建立商业能力",
                description = "发现真实需求",
                priority = 1,
            ),
        ),
        skills = emptyList(),
        recentQuests = emptyList(),
        recentFeedback = emptyList(),
        availableMinutes = availableMinutes,
        energy = 3,
        resources = setOf(QuestResource.PHONE),
        unfinishedChain = null,
    )
}
