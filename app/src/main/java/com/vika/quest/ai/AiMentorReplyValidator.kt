package com.vika.quest.ai

class AiMentorReplyValidator {
    fun validate(reply: AiMentorReply): AiMentorReply {
        val answer = reply.answer.trim()
        val question = reply.followUpQuestion?.trim()?.takeIf(String::isNotEmpty)
        val intention = reply.refinedIntention.trim()

        require(answer.length in 2..2_000) { "导师回答为空或过长" }
        require(question == null || question.length in 4..240) { "导师追问无效" }
        require(intention.length in 8..2_000) { "导师没有形成可继续处理的明确意图" }
        return reply.copy(answer = answer, followUpQuestion = question, refinedIntention = intention)
    }
}
