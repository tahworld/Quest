package com.vika.quest.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FakeAiProviderTest {
    @Test fun generatedDraftFitsAndValidates() = runBlocking { val context = QuestGenerationContext("梳理产品方向", 5, 3, setOf(QuestResource.PHONE), listOf(GoalSnapshot("g", "做产品", "", 1)), emptyList(), emptyList(), emptyList(), emptyList(), emptyList()); val draft = FakeAiProvider().generateQuest(context); assertEquals(5, draft.estimatedMinutes); assertNotNull(AiQuestDraftValidator().validate(draft, context)) }
}
