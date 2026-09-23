package com.vika.quest.model

data class NewQuest(
    val goalId: String,
    val skillId: String? = null,
    val chainId: String? = null,
    val title: String,
    val instruction: String,
    val estimatedMinutes: Int,
    val completionCriteria: List<String>,
    val difficulty: Int,
) {
    init {
        require(goalId.isNotBlank()) { "goalId 不能为空" }
        require(title.isNotBlank()) { "title 不能为空" }
        require(instruction.isNotBlank()) { "instruction 不能为空" }
        require(estimatedMinutes > 0) { "estimatedMinutes 必须大于 0" }
        require(completionCriteria.isNotEmpty()) { "至少需要一条完成标准" }
        require(completionCriteria.none(String::isBlank)) { "完成标准不能是空文本" }
        require(difficulty in 1..5) { "difficulty 必须在 1 到 5 之间" }
    }
}
