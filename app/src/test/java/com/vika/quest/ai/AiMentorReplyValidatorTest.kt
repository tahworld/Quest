package com.vika.quest.ai

import org.junit.Assert.assertEquals
import org.junit.Test

class AiMentorReplyValidatorTest {
    private val validator = AiMentorReplyValidator()

    @Test fun acceptsUsefulReply() {
        val reply = validator.validate(AiMentorReply("先验证需求。", "你能接触哪类用户？", "访谈三名目标用户并记录需求证据", true))
        assertEquals("先验证需求。", reply.answer)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingRefinedIntention() {
        validator.validate(AiMentorReply("先看看。", null, "", false))
    }
}
