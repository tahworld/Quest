package com.vika.quest.ai

import org.json.JSONArray
import org.json.JSONObject

object AiJsonCodec {
    fun parseQuest(raw: String): AiQuestDraft {
        val json = JSONObject(extractJson(raw))
        return AiQuestDraft(
            title = json.requiredString("title"), reason = json.requiredString("reason"), estimatedMinutes = json.requiredInt("estimatedMinutes"),
            steps = json.getJSONArray("steps").strings(), completionCriteria = json.getJSONArray("completionCriteria").strings(),
            expectedOutput = json.requiredString("expectedOutput"), relatedGoalId = json.nullableString("relatedGoalId"),
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

    fun parseClarification(raw: String): AiClarificationTurn {
        val json = JSONObject(extractJson(raw))
        val status = when (json.getString("status").trim().lowercase()) {
            "ask" -> ClarificationStatus.ASK
            "ready" -> ClarificationStatus.READY
            else -> throw IllegalArgumentException("未知澄清状态")
        }
        return AiClarificationTurn(
            status = status,
            question = json.nullableString("question"),
            options = json.optJSONArray("options")?.strings().orEmpty(),
            allowCustomAnswer = json.optBoolean("allowCustomAnswer", false),
            refinedIntention = json.nullableString("refinedIntention"),
        )
    }

    fun parseMentorReply(raw: String): AiMentorReply {
        val json = JSONObject(extractJson(raw))
        return AiMentorReply(
            answer = json.getString("answer"),
            followUpQuestion = json.nullableString("followUpQuestion"),
            refinedIntention = json.getString("refinedIntention"),
            readyForAction = json.getBoolean("readyForAction"),
        )
    }

    fun parseActionReview(raw: String): AiActionReviewReply {
        val json = JSONObject(extractJson(raw))
        require(json.has("proposedQuest") && json.has("adjustmentReason")) { "答疑字段缺失" }
        val proposal = json.get("proposedQuest")
        require(proposal == JSONObject.NULL || proposal is JSONObject) { "调整建议必须为对象或 null" }
        return AiActionReviewReply(
            answer = json.requiredString("answer"),
            adjustmentReason = json.nullableString("adjustmentReason"),
            proposedQuest = (proposal as? JSONObject)?.let { parseQuest(it.toString()) },
        )
    }

    fun actionReviewJson(reply: AiActionReviewReply): String = JSONObject().apply {
        put("answer", reply.answer)
        put("adjustmentReason", reply.adjustmentReason)
        put("proposedQuest", reply.proposedQuest?.let { draft -> JSONObject().apply {
            put("title", draft.title); put("reason", draft.reason); put("estimatedMinutes", draft.estimatedMinutes)
            put("steps", JSONArray(draft.steps)); put("completionCriteria", JSONArray(draft.completionCriteria))
            put("expectedOutput", draft.expectedOutput); put("relatedGoalId", draft.relatedGoalId); put("relatedProjectId", draft.relatedProjectId)
        } })
    }.toString()

    fun actionReviewContext(context: QuestReviewContext): String = JSONObject().apply {
        put("quest", questJson(context.quest)); put("reason", context.reason); put("steps", JSONArray(context.steps))
        put("completionCriteria", JSONArray(context.completionCriteria)); put("expectedOutput", context.expectedOutput)
        put("sourceIntention", context.sourceIntention); put("availableMinutes", context.availableMinutes)
        put("energy", context.energy); put("resources", JSONArray(context.resources.map { it.name }))
        put("goal", context.goal?.let { JSONObject().put("id", it.id).put("name", it.name).put("description", it.description) })
        put("project", context.project?.let { JSONObject().put("id", it.id).put("name", it.name).put("currentState", it.currentState) })
        put("memories", JSONArray(context.memories.map { JSONObject().put("type", it.type).put("content", it.content) }))
        put("question", context.question)
        put("priorAnswers", JSONArray(context.history.map { JSONObject().put("question", it.question).put("answer", it.answer) }))
    }.toString()

    fun generationContext(context: QuestGenerationContext): String = JSONObject().apply {
        put("currentUserIntention", context.userIntention); put("availableMinutes", context.availableMinutes); put("energy", context.energy)
        put("currentDirection", context.currentDirection?.let { JSONObject().put("id", it.id).put("name", it.name).put("description", it.description) })
        put("resources", JSONArray(context.resources.map { it.name })); put("goals", JSONArray(context.goals.map { JSONObject().put("id", it.id).put("name", it.name).put("description", it.description).put("priority", it.priority) }))
        put("projects", JSONArray(context.projects.map { JSONObject().put("id", it.id).put("goalId", it.goalId).put("name", it.name).put("description", it.description).put("currentState", it.currentState) }))
        put("workingMemory", JSONArray(context.memories.map { JSONObject().put("type", it.type).put("content", it.content).put("importance", it.importance) }))
        put("recentQuests", JSONArray(context.recentQuests.map(::questJson))); put("recentRejections", JSONArray(context.recentRejections.map { JSONObject().put("questTitle", it.questTitle).put("reason", it.reason).put("details", it.details) }))
        put("rejectedQuest", context.rejectedQuest?.let(::questJson)); put("rejectionReason", context.rejectionReason)
    }.toString()

    fun analysisContext(context: QuestResultAnalysisContext): String = JSONObject().apply {
        put("goal", context.goal?.let { JSONObject().put("id", it.id).put("name", it.name).put("description", it.description).put("priority", it.priority) }); put("project", context.project?.let { JSONObject().put("id", it.id).put("name", it.name).put("description", it.description).put("currentState", it.currentState) }); put("memories", JSONArray(context.memories.map { JSONObject().put("type", it.type).put("content", it.content).put("importance", it.importance) }))
        put("quest", questJson(context.quest)); put("steps", JSONArray(context.steps)); put("completionCriteria", JSONArray(context.completionCriteria))
        put("resultText", context.resultText); put("actualMinutes", context.actualMinutes); put("difficultyRating", context.difficultyRating); put("usefulnessRating", context.usefulnessRating)
    }.toString()

    fun clarificationContext(context: QuestClarificationContext): String = JSONObject().apply {
        put("currentUserIntention", context.userIntention)
        put("availableMinutes", context.availableMinutes)
        put("energy", context.energy)
        put("resources", JSONArray(context.resources.map { it.name }))
        put("projects", JSONArray(context.projects.map {
            JSONObject().put("id", it.id).put("name", it.name).put("description", it.description).put("currentState", it.currentState)
        }))
        put("workingMemory", JSONArray(context.memories.map {
            JSONObject().put("type", it.type).put("content", it.content).put("importance", it.importance)
        }))
        put("clarificationHistory", JSONArray(context.history.map {
            JSONObject().put("question", it.question).put("answer", it.answer)
        }))
    }.toString()

    fun mentorConversationContext(context: MentorConversationContext): String = JSONObject().apply {
        put("currentUserIntention", context.userIntention)
        put("availableMinutes", context.availableMinutes)
        put("energy", context.energy)
        put("resources", JSONArray(context.resources.map { it.name }))
        put("projects", JSONArray(context.projects.map {
            JSONObject().put("id", it.id).put("name", it.name).put("description", it.description).put("currentState", it.currentState)
        }))
        put("workingMemory", JSONArray(context.memories.map {
            JSONObject().put("type", it.type).put("content", it.content).put("importance", it.importance)
        }))
        put("conversation", JSONArray(context.messages.map {
            JSONObject().put("role", it.role.name.lowercase()).put("content", it.content)
        }))
    }.toString()

    fun chatRequest(model: String, system: String, user: String, maxTokens: Int = 1600) = JSONObject().apply {
        put("model", model); put("stream", false); put("max_tokens", maxTokens); put("response_format", JSONObject().put("type", "json_object"))
        put("messages", JSONArray().put(JSONObject().put("role", "system").put("content", system)).put(JSONObject().put("role", "user").put("content", user)))
    }.toString()

    fun contentFromChatResponse(raw: String): String = JSONObject(raw).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")

    private fun JSONArray.strings() = List(length()) { index ->
        get(index).let { value -> require(value is String) { "数组元素必须为文本" }; value }
    }
    private fun questJson(it: QuestSnapshot) = JSONObject().put("id", it.id).put("goalId", it.goalId).put("projectId", it.projectId).put("title", it.title).put("status", it.status).put("resultSummary", it.resultSummary)
    private fun JSONObject.requiredString(key: String): String = get(key).let { value ->
        require(value is String) { "$key 必须为文本" }; value
    }
    private fun JSONObject.requiredInt(key: String): Int = get(key).let { value ->
        require(value is Number && value.toDouble() == value.toInt().toDouble()) { "$key 必须为整数" }; value.toInt()
    }
    private fun JSONObject.nullableString(key: String): String? = if (isNull(key)) null else requiredString(key).takeIf(String::isNotBlank)
    private fun extractJson(raw: String): String = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
}
