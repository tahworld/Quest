package com.vika.quest.ai

interface AiProvider {
    suspend fun generateQuest(context: QuestGenerationContext): AiQuestDraft

    suspend fun analyzeQuestResult(input: QuestResultInput): QuestResultAnalysis
}
