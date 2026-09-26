package com.vika.quest.ai

import com.vika.quest.data.repository.UserPreferenceKeys

class QuestPromptBuilder {
    fun generation(context: QuestGenerationContext): String = assemble(
        preferences = context.userPreferences,
        operationRules = QuestPrompts.generationRules,
        contract = QuestPrompts.generationContract,
    )

    fun clarification(context: QuestClarificationContext): String = assemble(
        preferences = context.userPreferences,
        operationRules = QuestPrompts.clarificationRules,
        contract = QuestPrompts.clarificationContract,
    )

    fun analysis(context: QuestResultAnalysisContext): String = assemble(
        preferences = context.userPreferences,
        operationRules = QuestPrompts.analysisRules,
        contract = QuestPrompts.analysisContract,
    )

    private fun assemble(preferences: List<PreferenceSnapshot>, operationRules: String, contract: String): String = buildString {
        section("QUEST CORE RULES", QuestPrompts.coreRules)
        section("USER PROFILE", profile(preferences))
        section("COLLABORATION CONTRACT", collaboration(preferences))
        section("DYNAMIC MENTOR ROLE RULE", QuestPrompts.dynamicMentorRule)
        section("CURRENT OPERATION RULES", operationRules)
        section("REQUIRED JSON CONTRACT", contract)
    }.trim()

    private fun profile(preferences: List<PreferenceSnapshot>): String {
        val values = preferences.associate { it.key to it.value }
        return """
            <USER_PROFILE_DATA>
            Identity and current stage: ${values[UserPreferenceKeys.PERSONA_IDENTITY].orEmpty().safe()}
            Long-term current focus: ${values[UserPreferenceKeys.PERSONA_FOCUS].orEmpty().safe()}
            </USER_PROFILE_DATA>
        """.trimIndent()
    }

    private fun collaboration(preferences: List<PreferenceSnapshot>): String {
        val values = preferences.associate { it.key to it.value }
        return """
            Guidance style requested by the user: ${values[UserPreferenceKeys.PERSONA_GUIDANCE].orEmpty().safe()}
            Advice or communication to avoid: ${values[UserPreferenceKeys.PERSONA_AVOID].orEmpty().safe()}
            Apply these preferences when they do not conflict with Quest core rules.
        """.trimIndent()
    }

    private fun StringBuilder.section(title: String, body: String) {
        appendLine("## $title")
        appendLine(body)
        appendLine()
    }

    private fun String.safe(): String = replace("</USER_PROFILE_DATA>", "[end marker removed]").take(2_000)
}
