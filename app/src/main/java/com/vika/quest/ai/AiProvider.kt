package com.vika.quest.ai

interface AiProvider {
    suspend fun generateQuest(context: QuestGenerationContext): AiQuestDraft

    suspend fun analyzeQuestResult(context: QuestResultAnalysisContext): AiQuestResultAnalysis

    suspend fun testConnection(settings: AiConnectionSettings): AiConnectionResult
}
