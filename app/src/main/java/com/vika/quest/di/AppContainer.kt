package com.vika.quest.di

import android.content.Context
import com.vika.quest.ai.AiProvider
import com.vika.quest.ai.AiQuestDraftValidator
import com.vika.quest.ai.FakeAiProvider
import com.vika.quest.data.local.QuestDatabase
import com.vika.quest.data.repository.GoalRepository
import com.vika.quest.data.repository.QuestRepository

class AppContainer(context: Context) {
    private val database = QuestDatabase.create(context)

    val goalRepository = GoalRepository(database.goalDao())
    val questRepository = QuestRepository(database.questDao())
    val aiProvider: AiProvider = FakeAiProvider()
    val aiQuestDraftValidator = AiQuestDraftValidator()
}
