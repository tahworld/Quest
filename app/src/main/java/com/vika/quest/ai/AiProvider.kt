package com.vika.quest.ai

interface AiProvider {
    suspend fun clarifyQuest(context: QuestClarificationContext): AiClarificationTurn

    suspend fun continueMentorConversation(context: MentorConversationContext): AiMentorReply

    suspend fun generateQuest(context: QuestGenerationContext): AiQuestDraft

    suspend fun analyzeQuestResult(context: QuestResultAnalysisContext): AiQuestResultAnalysis

    suspend fun testConnection(settings: AiConnectionSettings): AiConnectionResult
}
