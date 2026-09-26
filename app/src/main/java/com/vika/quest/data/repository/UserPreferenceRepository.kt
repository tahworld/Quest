package com.vika.quest.data.repository

import com.vika.quest.data.local.dao.UserPreferenceDao
import com.vika.quest.data.local.entity.UserPreferenceEntity

data class UserPersona(
    val identityAndStage: String = "",
    val currentFocus: String = "",
    val guidanceStyle: String = "",
    val adviceToAvoid: String = "",
) {
    val isComplete: Boolean get() = identityAndStage.isNotBlank()
}

object UserPreferenceKeys {
    const val PERSONA_IDENTITY = "persona_identity_and_stage"
    const val PERSONA_FOCUS = "persona_current_focus"
    const val PERSONA_GUIDANCE = "persona_guidance_style"
    const val PERSONA_AVOID = "persona_advice_to_avoid"
    const val PERSONA_COMPLETE = "persona_setup_complete"

    val personaPromptKeys = listOf(PERSONA_IDENTITY, PERSONA_FOCUS, PERSONA_GUIDANCE, PERSONA_AVOID)
}

class UserPreferenceRepository(private val dao: UserPreferenceDao) {
    suspend fun getRecent(limit: Int) = dao.getRecent(limit)
    suspend fun save(key: String, value: String) = dao.upsert(UserPreferenceEntity(key, value, System.currentTimeMillis()))

    suspend fun get(key: String): String? = dao.getByKey(key)?.value

    suspend fun getPersona() = UserPersona(
        identityAndStage = get(UserPreferenceKeys.PERSONA_IDENTITY).orEmpty(),
        currentFocus = get(UserPreferenceKeys.PERSONA_FOCUS).orEmpty(),
        guidanceStyle = get(UserPreferenceKeys.PERSONA_GUIDANCE).orEmpty(),
        adviceToAvoid = get(UserPreferenceKeys.PERSONA_AVOID).orEmpty(),
    )

    suspend fun isPersonaComplete(): Boolean =
        get(UserPreferenceKeys.PERSONA_COMPLETE) == "true" && getPersona().isComplete

    suspend fun savePersona(persona: UserPersona) {
        require(persona.identityAndStage.isNotBlank()) { "请先介绍你是谁以及当前所处的阶段。" }
        save(UserPreferenceKeys.PERSONA_IDENTITY, persona.identityAndStage.trim().take(2_000))
        save(UserPreferenceKeys.PERSONA_FOCUS, persona.currentFocus.trim().take(2_000))
        save(UserPreferenceKeys.PERSONA_GUIDANCE, persona.guidanceStyle.trim().take(1_000))
        save(UserPreferenceKeys.PERSONA_AVOID, persona.adviceToAvoid.trim().take(1_000))
        save(UserPreferenceKeys.PERSONA_COMPLETE, "true")
    }

    suspend fun getPromptPreferences(limit: Int): List<UserPreferenceEntity> {
        require(limit > 0)
        val persona = UserPreferenceKeys.personaPromptKeys.mapNotNull { dao.getByKey(it) }
        val recent = dao.getRecent(limit).filterNot { it.key == UserPreferenceKeys.PERSONA_COMPLETE }
        return (persona + recent).distinctBy { it.key }.take(limit)
    }
}
