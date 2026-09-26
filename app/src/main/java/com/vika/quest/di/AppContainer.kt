package com.vika.quest.di

import android.content.Context
import com.vika.quest.ai.AiProvider
import com.vika.quest.ai.AiQuestDraftValidator
import com.vika.quest.ai.FakeAiProvider
import com.vika.quest.ai.AiQuestResultValidator
import com.vika.quest.ai.AiSettingsStore
import com.vika.quest.ai.ConfiguredAiProvider
import com.vika.quest.ai.ContextBuilder
import com.vika.quest.ai.DeepSeekAiProvider
import com.vika.quest.ai.AiClarificationValidator
import com.vika.quest.ai.AiMentorReplyValidator
import com.vika.quest.ai.QuestPromptBuilder
import com.vika.quest.data.local.QuestDatabase
import com.vika.quest.data.repository.GoalRepository
import com.vika.quest.data.repository.QuestRepository
import com.vika.quest.data.repository.MemoryRepository
import com.vika.quest.data.repository.ProjectRepository
import com.vika.quest.data.repository.UserPreferenceRepository

class AppContainer(context: Context) {
    private val database = QuestDatabase.create(context)

    val goalRepository = GoalRepository(database.goalDao())
    val questRepository = QuestRepository(database)
    val projectRepository = ProjectRepository(database.projectDao())
    val memoryRepository = MemoryRepository(database.memoryDao())
    val userPreferenceRepository = UserPreferenceRepository(database.userPreferenceDao())
    val aiQuestDraftValidator = AiQuestDraftValidator()
    val aiClarificationValidator = AiClarificationValidator()
    val aiMentorReplyValidator = AiMentorReplyValidator()
    val aiQuestResultValidator = AiQuestResultValidator()
    val questPromptBuilder = QuestPromptBuilder()
    val aiSettingsStore = AiSettingsStore(context)
    private val deepSeekProvider = DeepSeekAiProvider(aiSettingsStore, aiQuestDraftValidator, aiClarificationValidator, aiMentorReplyValidator, aiQuestResultValidator, questPromptBuilder)
    val aiProvider: AiProvider = ConfiguredAiProvider(aiSettingsStore, deepSeekProvider, FakeAiProvider())
    val contextBuilder = ContextBuilder(goalRepository, projectRepository, memoryRepository, questRepository, userPreferenceRepository)
}
