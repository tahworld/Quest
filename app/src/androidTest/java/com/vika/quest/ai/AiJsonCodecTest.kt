package com.vika.quest.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiJsonCodecTest {
    @Test fun parsesTypedQuestJson() { val draft = AiJsonCodec.parseQuest("""{"title":"记录三个竞品入口","reason":"符合方向","estimatedMinutes":15,"steps":["搜索官网"],"completionCriteria":["记录三个入口"],"expectedOutput":"一份入口表","relatedGoalId":null,"relatedProjectId":null}"""); assertEquals(15, draft.estimatedMinutes); assertEquals(1, draft.steps.size) }
    @Test(expected = Exception::class) fun rejectsMissingFields() { AiJsonCodec.parseQuest("{}") }
}
