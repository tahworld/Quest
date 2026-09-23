package com.vika.quest.ai

import org.junit.Assert.*
import org.junit.Test

class AiQuestDraftValidatorTest {
    private val context = QuestGenerationContext("研究 AI 产品获客", 15, 3, setOf(QuestResource.PHONE), listOf(GoalSnapshot("g", "商业能力", "", 1)), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    @Test fun acceptsConcreteBoundedQuest() { val result = AiQuestDraftValidator().validate(validDraft(), context); assertEquals(15, result.estimatedMinutes); assertEquals("g", result.goalId) }
    @Test fun rejectsUnknownGoal() { assertThrows(IllegalArgumentException::class.java) { AiQuestDraftValidator().validate(validDraft().copy(relatedGoalId = "bad"), context) } }
    @Test fun rejectsGenericTitle() { assertThrows(IllegalArgumentException::class.java) { AiQuestDraftValidator().validate(validDraft().copy(title = "继续推进 AI 产品"), context) } }
    @Test fun rejectsOvertime() { assertThrows(IllegalArgumentException::class.java) { AiQuestDraftValidator().validate(validDraft().copy(estimatedMinutes = 30), context) } }
    private fun validDraft() = AiQuestDraft("记录三个 AI 产品的获客入口", "直接承接用户的获客研究方向。", 15, listOf("搜索三个产品官网", "记录每个产品的首个获客入口"), listOf("记录了三个不同产品", "每个产品包含入口证据"), "一份含三个产品及证据链接的表格", "g", null)
}
