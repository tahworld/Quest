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

    private fun clarificationContext(history: List<ClarificationExchange>) = QuestClarificationContext(
        "梳理产品方向", 15, 3, setOf(QuestResource.PHONE), emptyList(), emptyList(), emptyList(), history,
    )
}
