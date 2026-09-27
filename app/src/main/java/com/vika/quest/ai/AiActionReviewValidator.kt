package com.vika.quest.ai

class AiActionReviewValidator(private val questValidator: AiQuestDraftValidator) {
    fun validate(reply: AiActionReviewReply, context: QuestReviewContext): AiActionReviewReply {
        require(reply.answer.isNotBlank() && reply.answer.length <= 1_200) { "AI 没有回答这一步的问题" }
        require(reply.proposedQuest == null || !reply.adjustmentReason.isNullOrBlank()) { "调整建议缺少原因" }
        require(reply.adjustmentReason == null || reply.adjustmentReason.length <= 500) { "调整原因过长" }
        reply.proposedQuest?.let { draft ->
            require(draft.relatedGoalId == null || draft.relatedGoalId == context.goal?.id) { "答疑不能改变选定方向" }
            require(draft.relatedProjectId == null || draft.relatedProjectId == context.project?.id) { "答疑不能切换项目" }
            questValidator.validate(draft, context.generationContext())
        }
        return reply.copy(answer = reply.answer.trim(), adjustmentReason = reply.adjustmentReason?.trim())
    }
}
