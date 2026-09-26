package com.vika.quest.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiJsonCodecTest {
    @Test fun parsesTypedQuestJson() { val draft = AiJsonCodec.parseQuest("""{"title":"记录三个竞品入口","reason":"符合方向","estimatedMinutes":15,"steps":["搜索官网"],"completionCriteria":["记录三个入口"],"expectedOutput":"一份入口表","relatedGoalId":null,"relatedProjectId":null}"""); assertEquals(15, draft.estimatedMinutes); assertEquals(1, draft.steps.size) }
    @Test(expected = Exception::class) fun rejectsMissingFields() { AiJsonCodec.parseQuest("{}") }
    @Test fun parsesClarificationQuestion() { val turn = AiJsonCodec.parseClarification("""{"status":"ask","question":"你希望留下什么成果？","options":["清单","表格"],"allowCustomAnswer":true,"refinedIntention":null}"""); assertEquals(ClarificationStatus.ASK, turn.status); assertEquals(2, turn.options.size) }
    @Test fun parsesClarificationReady() { val turn = AiJsonCodec.parseClarification("""{"status":"ready","question":null,"options":[],"allowCustomAnswer":false,"refinedIntention":"研究三个 AI 产品的获客入口"}"""); assertEquals(ClarificationStatus.READY, turn.status); assertNotNull(turn.refinedIntention) }
    @Test fun chatRequestUsesSelectedModel() { val request = AiJsonCodec.chatRequest("selected-model", "system", "user"); assertTrue(request.contains("\"model\":\"selected-model\"")) }
    @Test fun parsesTypedMentorReply() { val reply = AiJsonCodec.parseMentorReply("""{"answer":"先验证需求","followUpQuestion":"你能接触到哪类用户？","refinedIntention":"访谈三名目标用户并记录需求证据","readyForAction":true}"""); assertTrue(reply.readyForAction); assertEquals("先验证需求", reply.answer) }
}
