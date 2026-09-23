package com.vika.quest.ai

import org.json.JSONArray
import org.json.JSONObject

object AiJsonCodec {
    fun parseQuest(raw: String): AiQuestDraft {
        val json = JSONObject(extractJson(raw))
        return AiQuestDraft(
            title = json.getString("title"), reason = json.getString("reason"), estimatedMinutes = json.getInt("estimatedMinutes"),
            steps = json.getJSONArray("steps").strings(), completionCriteria = json.getJSONArray("completionCriteria").strings(),
            expectedOutput = json.getString("expectedOutput"), relatedGoalId = json.nullableString("relatedGoalId"),
            relatedProjectId = json.nullableString("relatedProjectId"),
        )
    }

    fun parseAnalysis(raw: String): AiQuestResultAnalysis {
        val json = JSONObject(extractJson(raw))
        val memoryArray = json.getJSONArray("memoriesToSave")
        return AiQuestResultAnalysis(
            summary = json.getString("summary"), evidence = json.getJSONArray("evidence").strings(), insights = json.getJSONArray("insights").strings(),
            projectProgress = json.optString("projectProgress"), suggestedNextStep = json.optString("suggestedNextStep"),
            memoriesToSave = List(memoryArray.length()) { index -> memoryArray.getJSONObject(index).let { ProposedMemory(it.getString("type"), it.getString("content"), it.getInt("importance")) } },
        )
    }

    fun generationContext(context: QuestGenerationContext): String = JSONObject().apply {
        put("currentUserIntention", context.userIntention); put("availableMinutes", context.availableMinutes); put("energy", context.energy)
        put("resources", JSONArray(context.resources.map { it.name })); put("goals", JSONArray(context.goals.map { JSONObject().put("id", it.id).put("name", it.name).put("description", it.description).put("priority", it.priority) }))
        put("projects", JSONArray(context.projects.map { JSONObject().put("id", it.id).put("goalId", it.goalId).put("name", it.name).put("description", it.description).put("currentState", it.currentState) }))
        put("workingMemory", JSONArray(context.memories.map { JSONObject().put("type", it.type).put("content", it.content).put("importance", it.importance) }))
        put("recentQuests", JSONArray(context.recentQuests.map(::questJson))); put("recentRejections", JSONArray(context.recentRejections.map { JSONObject().put("questTitle", it.questTitle).put("reason", it.reason).put("details", it.details) }))
        put("userPreferences", JSONArray(context.userPreferences.map { JSONObject().put("key", it.key).put("value", it.value) })); put("rejectedQuest", context.rejectedQuest?.let(::questJson)); put("rejectionReason", context.rejectionReason)
    }.toString()

    fun analysisContext(context: QuestResultAnalysisContext): String = JSONObject().apply {
        put("goal", context.goal?.let { JSONObject().put("id", it.id).put("name", it.name).put("description", it.description).put("priority", it.priority) }); put("project", context.project?.let { JSONObject().put("id", it.id).put("name", it.name).put("description", it.description).put("currentState", it.currentState) }); put("memories", JSONArray(context.memories.map { JSONObject().put("type", it.type).put("content", it.content).put("importance", it.importance) }))
        put("quest", questJson(context.quest)); put("steps", JSONArray(context.steps)); put("completionCriteria", JSONArray(context.completionCriteria))
        put("resultText", context.resultText); put("actualMinutes", context.actualMinutes); put("difficultyRating", context.difficultyRating); put("usefulnessRating", context.usefulnessRating)
    }.toString()

    fun chatRequest(model: String, system: String, user: String) = JSONObject().apply {
        put("model", model); put("stream", false); put("max_tokens", 1600); put("response_format", JSONObject().put("type", "json_object"))
        put("messages", JSONArray().put(JSONObject().put("role", "system").put("content", system)).put(JSONObject().put("role", "user").put("content", user)))
    }.toString()

    fun contentFromChatResponse(raw: String): String = JSONObject(raw).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")

    private fun JSONArray.strings() = List(length()) { getString(it) }
    private fun questJson(it: QuestSnapshot) = JSONObject().put("id", it.id).put("goalId", it.goalId).put("projectId", it.projectId).put("title", it.title).put("status", it.status).put("resultSummary", it.resultSummary)
    private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else getString(key).takeIf(String::isNotBlank)
    private fun extractJson(raw: String): String = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
}
