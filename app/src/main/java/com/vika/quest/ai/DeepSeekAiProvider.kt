package com.vika.quest.ai

import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DeepSeekAiProvider(
    private val settingsStore: AiSettingsStore,
    private val questValidator: AiQuestDraftValidator,
    private val clarificationValidator: AiClarificationValidator,
    private val mentorReplyValidator: AiMentorReplyValidator,
    private val resultValidator: AiQuestResultValidator,
    private val promptBuilder: QuestPromptBuilder,
) : AiProvider {
    override suspend fun clarifyQuest(context: QuestClarificationContext): AiClarificationTurn {
        val settings = settingsStore.readConnectionSettings() ?: error("请先在 AI 设置中保存 DeepSeek API Key")
        var raw = chat(settings, promptBuilder.clarification(context), AiJsonCodec.clarificationContext(context), maxTokens = 700)
        repeat(2) { attempt ->
            try {
                return clarificationValidator.validate(AiJsonCodec.parseClarification(raw), context.userIntention)
            } catch (error: Exception) {
                if (attempt == 1) throw IllegalArgumentException("AI 返回的澄清内容无效：${error.message}")
                raw = chat(settings, promptBuilder.clarification(context), AiJsonCodec.clarificationContext(context) + "\n\n" + QuestPrompts.repair(error.message.orEmpty(), raw), maxTokens = 700)
            }
        }
        error("AI 返回的澄清内容无效")
    }

    override suspend fun continueMentorConversation(context: MentorConversationContext): AiMentorReply {
        val settings = settingsStore.readConnectionSettings() ?: error("请先在 AI 设置中保存 DeepSeek API Key")
        var raw = chat(
            settings,
            promptBuilder.mentorConversation(context),
            AiJsonCodec.mentorConversationContext(context),
            maxTokens = 1_000,
        )
        repeat(2) { attempt ->
            try {
                return mentorReplyValidator.validate(AiJsonCodec.parseMentorReply(raw))
            } catch (error: Exception) {
                if (attempt == 1) throw IllegalArgumentException("AI 返回的导师回答无效：${error.message}")
                raw = chat(
                    settings,
                    promptBuilder.mentorConversation(context),
                    AiJsonCodec.mentorConversationContext(context) + "\n\n" + QuestPrompts.repair(error.message.orEmpty(), raw),
                    maxTokens = 1_000,
                )
            }
        }
        error("AI 返回的导师回答无效")
    }

    override suspend fun generateQuest(context: QuestGenerationContext): AiQuestDraft {
        val settings = settingsStore.readConnectionSettings() ?: error("请先在 AI 设置中保存 DeepSeek API Key")
        var raw = chat(settings, promptBuilder.generation(context), AiJsonCodec.generationContext(context))
        repeat(2) { attempt ->
            try {
                return AiJsonCodec.parseQuest(raw).also { questValidator.validate(it, context) }
            } catch (error: Exception) {
                if (attempt == 1) throw IllegalArgumentException("AI 返回内容无效：${error.message}")
                raw = chat(settings, promptBuilder.generation(context), AiJsonCodec.generationContext(context) + "\n\n" + QuestPrompts.repair(error.message.orEmpty(), raw))
            }
        }
        error("AI 返回内容无效")
    }

    override suspend fun analyzeQuestResult(context: QuestResultAnalysisContext): AiQuestResultAnalysis {
        val settings = settingsStore.readConnectionSettings() ?: return FakeAiProvider().analyzeQuestResult(context)
        return resultValidator.validate(AiJsonCodec.parseAnalysis(chat(settings, promptBuilder.analysis(context), AiJsonCodec.analysisContext(context))))
    }

    override suspend fun testConnection(settings: AiConnectionSettings): AiConnectionResult = try {
        val models = JSONObject(request(settings, "/models", "GET", null)).getJSONArray("data")
        val available = List(models.length()) { models.getJSONObject(it).getString("id") }
        if (settings.model in available) AiConnectionResult(true, "连接成功，模型 ${settings.model} 可用", available)
        else AiConnectionResult(false, "连接成功，但账号当前没有模型 ${settings.model}", available)
    } catch (error: Exception) {
        AiConnectionResult(false, error.message ?: "连接失败")
    }

    private suspend fun chat(settings: AiConnectionSettings, system: String, user: String, maxTokens: Int = 1600): String {
        val body = request(settings, "/chat/completions", "POST", AiJsonCodec.chatRequest(settings.model, system, user, maxTokens))
        return runCatching { AiJsonCodec.contentFromChatResponse(body) }.getOrElse { throw IOException("DeepSeek 响应缺少有效内容") }.also { if (it.isBlank()) throw IOException("DeepSeek 返回了空内容") }
    }

    private suspend fun request(settings: AiConnectionSettings, path: String, method: String, body: String?): String = withContext(Dispatchers.IO) {
        require(settings.baseUrl.startsWith("https://")) { "API 地址必须使用 HTTPS" }
        try {
            val connection = (URL(settings.baseUrl.removeSuffix("/") + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method; connectTimeout = 10_000; readTimeout = 60_000
                setRequestProperty("Authorization", "Bearer ${settings.apiKey}"); setRequestProperty("Content-Type", "application/json")
                if (body != null) { doOutput = true; outputStream.bufferedWriter().use { it.write(body) } }
            }
            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException(httpMessage(code, text))
            text
        } catch (_: SocketTimeoutException) { throw IOException("连接 DeepSeek 超时，请检查网络后重试") }
    }

    private fun httpMessage(code: Int, raw: String): String {
        val detail = runCatching { JSONObject(raw).optJSONObject("error")?.optString("message") }.getOrNull()?.takeIf(String::isNotBlank)
        return when (code) { 401 -> "API Key 无效或已失效"; 402 -> "DeepSeek 账户余额不足"; 429 -> "请求过于频繁，请稍后重试"; in 500..599 -> "DeepSeek 服务暂时不可用"; else -> detail ?: "DeepSeek 请求失败（HTTP $code）" }
    }
}

class ConfiguredAiProvider(private val settings: AiSettingsStore, private val deepSeek: DeepSeekAiProvider, private val fake: FakeAiProvider) : AiProvider {
    private fun active(): AiProvider = if (settings.readPublic().hasApiKey) deepSeek else fake
    override suspend fun clarifyQuest(context: QuestClarificationContext) = active().clarifyQuest(context)
    override suspend fun continueMentorConversation(context: MentorConversationContext) = active().continueMentorConversation(context)
    override suspend fun generateQuest(context: QuestGenerationContext) = active().generateQuest(context)
    override suspend fun analyzeQuestResult(context: QuestResultAnalysisContext) = active().analyzeQuestResult(context)
    override suspend fun testConnection(settings: AiConnectionSettings) = deepSeek.testConnection(settings)
}
