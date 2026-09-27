package com.vika.quest.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FakeAiProviderTest {
    @Test fun generatedDraftFitsAndValidates() = runBlocking { val context = QuestGenerationContext("梳理产品方向", 5, 3, setOf(QuestResource.PHONE), listOf(GoalSnapshot("g", "做产品", "", 1)), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()); val draft = FakeAiProvider().generateQuest(context); assertEquals(5, draft.estimatedMinutes); assertNotNull(AiQuestDraftValidator().validate(draft, context)) }

    @Test fun clarificationIsDeterministicAndBecomesReady() = runBlocking {
        val provider = FakeAiProvider()
        val first = provider.clarifyQuest(clarificationContext(emptyList()))
        assertEquals(ClarificationStatus.ASK, first.status)
        val second = provider.clarifyQuest(clarificationContext(listOf(ClarificationExchange(checkNotNull(first.question), "对比表"))))
        assertEquals(ClarificationStatus.ASK, second.status)
        val ready = provider.clarifyQuest(clarificationContext(listOf(ClarificationExchange("成果？", "对比表"), ClarificationExchange("限制？", "时间少"))))
        assertEquals(ClarificationStatus.READY, ready.status)
        assertTrue(checkNotNull(ready.refinedIntention).contains("梳理产品方向"))
    }

    @Test fun mentorConversationAnswersThenBecomesReady() = runBlocking {
        val provider = FakeAiProvider()
        val first = provider.continueMentorConversation(mentorContext(emptyList()))
        assertFalse(first.readyForAction)
        assertNotNull(first.followUpQuestion)
        val second = provider.continueMentorConversation(
            mentorContext(listOf(MentorMessage(MentorMessageRole.USER, "我卡在不知道先验证需求还是先写代码"))),
        )
        assertTrue(second.readyForAction)
        assertTrue(second.refinedIntention.contains("梳理产品方向"))
    }

    @Test fun reviewAnswersAndOnlyProposesWhenConditionsConflict() = runBlocking {
        val provider = FakeAiProvider()
        val base = QuestReviewContext(
            QuestSnapshot("q", "g", null, "复述一个阅读观点", "PENDING", null), "承接阅读",
            listOf("打开材料复述一小段"), listOf("留下一条观点"), "一条阅读笔记", "继续阅读", 5, 3,
            setOf(QuestResource.PHONE), GoalSnapshot("g", "读书", "阅读/学习：读书", 0), null,
            emptyList(), emptyList(), "为什么要做？", emptyList(),
        )
        assertNull(provider.reviewQuest(base).proposedQuest)
        val blocked = provider.reviewQuest(base.copy(question = "现在没时间，能改吗？"))
        assertNotNull(blocked.proposedQuest)
        AiActionReviewValidator(AiQuestDraftValidator()).validate(blocked, base.copy(question = "现在没时间，能改吗？"))
    }

    @Test fun fakeContinuesPreviousReadingResult() = runBlocking {
        val goal = GoalSnapshot("g", "现有教材", "阅读/学习：现有教材", 0)
        val base = QuestGenerationContext("", 5, 3, setOf(QuestResource.PHONE), listOf(goal), emptyList(), emptyList(),
            listOf(QuestSnapshot("previous", "g", null, "从现有材料提取一个可复述的观点", "COMPLETED", "记下一个观点")), emptyList(), emptyList(), currentDirection = goal)
        val draft = FakeAiProvider().generateQuest(base)
        assertTrue(draft.steps.any { it.contains("记下一个观点") })
        AiQuestDraftValidator().validate(draft, base)
    }

    @Test fun readingOnPhoneInFiveMinutesLeavesARealNote() = runBlocking {
        val goal = GoalSnapshot("reading", "手头的机械教材", "阅读/学习：手头的机械教材", 0)
        val context = QuestGenerationContext("", 5, 3, setOf(QuestResource.PHONE), listOf(goal), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), currentDirection = goal)
        val draft = FakeAiProvider().generateQuest(context)
        assertEquals(5, draft.estimatedMinutes)
        assertTrue(draft.expectedOutput.contains("笔记"))
        AiQuestDraftValidator().validate(draft, context)
    }

    @Test fun physicalDirectionWithoutMovementDoesNotOrderExercise() = runBlocking {
        val goal = GoalSnapshot("movement", "恢复轻量运动", "身体活动：恢复轻量运动", 0)
        val context = QuestGenerationContext("", 5, 1, setOf(QuestResource.PHONE), listOf(goal), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), currentDirection = goal)
        val draft = FakeAiProvider().generateQuest(context)
        assertFalse(draft.steps.any { "步行" in it || "冲刺" in it })
        AiQuestDraftValidator().validate(draft, context)
    }

    private fun clarificationContext(history: List<ClarificationExchange>) = QuestClarificationContext(
        "梳理产品方向", 15, 3, setOf(QuestResource.PHONE), emptyList(), emptyList(), emptyList(), history,
    )

    private fun mentorContext(messages: List<MentorMessage>) = MentorConversationContext(
        "梳理产品方向", 15, 3, setOf(QuestResource.PHONE), emptyList(), emptyList(), emptyList(), messages,
    )
}
