package com.vika.quest.ai

class AiClarificationValidator {
    private val vagueIntention = listOf(
        Regex("(?i)^learn about\\b"),
        Regex("(?i)^make progress on\\b"),
        Regex("^学习.{0,12}$"),
        Regex("^推进.{0,12}$"),
        Regex("^继续做.{0,12}$"),
    )

    fun validate(turn: AiClarificationTurn, originalIntention: String): AiClarificationTurn = when (turn.status) {
        ClarificationStatus.ASK -> {
            val question = turn.question.orEmpty().trim()
            val options = turn.options.map(String::trim).filter(String::isNotEmpty).distinct()
            require(question.length in 4..240) { "AI 返回的澄清问题无效" }
            require(options.size in 2..4 && options.all { it.length <= 80 }) { "AI 必须返回 2 到 4 个简短选项" }
            require(turn.allowCustomAnswer) { "澄清问题必须允许用户自由回答" }
            require(turn.refinedIntention.isNullOrBlank()) { "提问阶段不能提前返回行动意图" }
            turn.copy(question = question, options = options, refinedIntention = null)
        }

        ClarificationStatus.READY -> {
            val refined = turn.refinedIntention.orEmpty().trim()
            require(turn.question.isNullOrBlank() && turn.options.isEmpty()) { "澄清完成后不能继续提问" }
            require(!turn.allowCustomAnswer) { "澄清完成状态无须自由回答" }
            require(refined.length in 8..2_000 && vagueIntention.none { it.containsMatchIn(refined) }) { "AI 返回的意图仍然过于笼统" }
            require(preservesDirection(originalIntention, refined)) { "AI 偏离了用户原本的方向" }
            turn.copy(question = null, refinedIntention = refined)
        }
    }

    private fun preservesDirection(original: String, refined: String): Boolean {
        val source = original.trim().lowercase()
        if (source.length < 4) return true
        val target = refined.lowercase()
        val latinTerms = Regex("[a-z0-9]{2,}").findAll(source).map { it.value }.toList()
        if (latinTerms.any(target::contains)) return true
        val han = Regex("[\\u4e00-\\u9fff]{2,}").findAll(source).flatMap { match ->
            match.value.windowed(size = 2, step = 1).asSequence()
        }.filterNot { it in genericBigrams }.toList()
        return han.isEmpty() || han.any(target::contains)
    }

    private companion object {
        val genericBigrams = setOf("我想", "现在", "这个", "一个", "一下", "帮我", "进行", "开始")
    }
}
