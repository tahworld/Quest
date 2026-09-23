package com.vika.quest.model

data class NewQuest(
    val goalId: String?,
    val projectId: String? = null,
    val skillId: String? = null,
    val chainId: String? = null,
    val title: String,
    val reason: String,
    val steps: List<String>,
    val instruction: String,
    val estimatedMinutes: Int,
    val completionCriteria: List<String>,
    val expectedOutput: String,
    val difficulty: Int = 2,
    val sourceIntention: String,
    val sourceAvailableMinutes: Int,
    val sourceEnergy: Int,
    val sourceResources: List<String>,
) {
    init {
        require(goalId == null || goalId.isNotBlank()) { "goalId 不能是空文本" }
        require(projectId == null || projectId.isNotBlank()) { "projectId 不能是空文本" }
        require(title.isNotBlank()) { "title 不能为空" }
        require(reason.isNotBlank()) { "reason 不能为空" }
        require(steps.isNotEmpty() && steps.none(String::isBlank)) { "至少需要一个有效步骤" }
        require(instruction.isNotBlank()) { "instruction 不能为空" }
        require(estimatedMinutes > 0) { "estimatedMinutes 必须大于 0" }
        require(completionCriteria.isNotEmpty()) { "至少需要一条完成标准" }
        require(completionCriteria.none(String::isBlank)) { "完成标准不能是空文本" }
        require(expectedOutput.isNotBlank()) { "expectedOutput 不能为空" }
        require(difficulty in 1..5) { "difficulty 必须在 1 到 5 之间" }
        require(sourceAvailableMinutes > 0) { "sourceAvailableMinutes 必须大于 0" }
        require(sourceEnergy in 1..5) { "sourceEnergy 必须在 1 到 5 之间" }
    }
}
