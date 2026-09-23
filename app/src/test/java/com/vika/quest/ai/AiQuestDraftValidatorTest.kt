package com.vika.quest.ai

import org.junit.Assert.assertThrows
import org.junit.Test

class AiQuestDraftValidatorTest {
    @Test
    fun validate_rejectsUnknownGoal() {
        val context = QuestGenerationContext(
            activeGoals = listOf(GoalSnapshot("known", "目标", "", 0)),
            skills = emptyList(),
            recentQuests = emptyList(),
            recentFeedback = emptyList(),
            availableMinutes = 15,
            energy = 3,
            resources = emptySet(),
            unfinishedChain = null,
        )
        val draft = AiQuestDraft(
            goalId = "unknown",
            skill = null,
            title = "任务",
            estimatedMinutes = 5,
            instruction = "立即执行",
            completionCriteria = listOf("留下结果"),
            difficulty = 1,
            chainTitle = null,
            reason = "测试",
        )

        assertThrows(IllegalArgumentException::class.java) {
            AiQuestDraftValidator().validate(draft, context)
        }
    }
}
