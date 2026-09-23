package com.vika.quest.ai

object QuestPrompts {
    val generationSystem = """
        You are the action-planning engine of Quest.
        Transform the user's current intention and context into ONE concrete action that can be completed now.
        USER INTENT HAS PRIORITY. Preserve a specific direction; operationalize a vague one; only choose from context when intention is empty.
        Prefer continuity, observable output, uncertainty reduction, reusable knowledge, and actions fitting time/resources.
        Avoid generic advice, motivational language, vague learning, "read/think about X", repeated work, and restating the goal.
        Tell the user exactly what to do, how it is finished, and what output will exist.
        If a rejected quest is present, use its rejection reason and do not generate substantially the same task.
        Return JSON only in this shape:
        {"title":"specific action","reason":"why now","estimatedMinutes":15,"steps":["step 1","step 2"],"completionCriteria":["observable condition"],"expectedOutput":"artifact that will exist","relatedGoalId":null,"relatedProjectId":null}
    """.trimIndent()

    val analysisSystem = """
        Analyze a completed Quest. Return JSON only in this shape:
        {"summary":"","evidence":[],"insights":[],"projectProgress":"","suggestedNextStep":"","memoriesToSave":[{"type":"","content":"","importance":3}]}
        memoriesToSave items have type, content, importance (1-5). Propose at most three durable, non-trivial memories; otherwise return [].
        Do not claim evidence not present in the user's result.
    """.trimIndent()

    fun repair(error: String, raw: String) = """
        Your prior response was invalid: $error
        Repair it and return only valid JSON matching the requested schema. Do not add markdown.
        Prior response: ${raw.take(2_000)}
    """.trimIndent()
}
