package com.vika.quest.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AiClarificationValidatorTest {
    private val validator = AiClarificationValidator()

    @Test
    fun acceptsOneMaterialQuestion() {
        val result = validator.validate(
            AiClarificationTurn(ClarificationStatus.ASK, "这次结束时最希望留下什么成果？", listOf("案例清单", "分析表"), true, null),
            "研究 AI 产品如何获客",
        )
        assertEquals(ClarificationStatus.ASK, result.status)
    }

    @Test
    fun rejectsQuestionWithoutCustomAnswer() {
        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(AiClarificationTurn(ClarificationStatus.ASK, "选择一个成果形式", listOf("清单", "表格"), false, null), "研究 AI 产品如何获客")
        }
    }

    @Test
    fun acceptsReadyWhenDirectionIsPreserved() {
        val result = validator.validate(
            AiClarificationTurn(ClarificationStatus.READY, null, emptyList(), false, "研究三个 AI 产品的获客入口，并输出带证据的对比表"),
            "研究 AI 产品如何获客",
        )
        assertEquals(ClarificationStatus.READY, result.status)
    }

    @Test
    fun rejectsReadyThatChangesDirection() {
        assertThrows(IllegalArgumentException::class.java) {
            validator.validate(
                AiClarificationTurn(ClarificationStatus.READY, null, emptyList(), false, "制定一份跑步训练计划并记录配速"),
                "研究 AI 产品如何获客",
            )
        }
    }
}
