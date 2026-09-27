package com.vika.quest.ai

import org.junit.Assert.*
import org.junit.Test

class AiActionReviewValidatorTest {
    private val context = QuestReviewContext(
        quest = QuestSnapshot("q", "g", null, "记录一个阅读观点", "PENDING", null),
        reason = "承接材料", steps = listOf("打开手头材料并复述一段"), completionCriteria = listOf("留下观点"),
        expectedOutput = "一条观点笔记", sourceIntention = "读手头材料", availableMinutes = 5, energy = 2,
        resources = setOf(QuestResource.PHONE), goal = GoalSnapshot("g", "读材料", "阅读/学习：读材料", 0),
        project = null, memories = emptyList(), userPreferences = emptyList(), question = "五分钟够吗？", history = emptyList(),
    )
    private val validator = AiActionReviewValidator(AiQuestDraftValidator())

    @Test fun acceptsDirectAnswerWithoutMutationProposal() {
        assertNull(validator.validate(AiActionReviewReply("只复述一小段即可。", null, null), context).proposedQuest)
    }

    @Test fun rejectsProposalBeyondAvailableTime() {
        val proposal = AiQuestDraft("复述材料中的一个观点", "缩小范围", 15, listOf("打开材料并复述一段"), listOf("记录一个观点"), "一条观点笔记", "g", null)
        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(AiActionReviewReply("请缩小范围。", "时间不够", proposal), context)
        }
    }

    @Test fun rejectsEmptyAnswer() {
        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(AiActionReviewReply(" ", null, null), context)
        }
    }
}
