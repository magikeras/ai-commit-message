package com.aicommitmessage.service

import com.aicommitmessage.settings.AppSettings
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AnthropicService {

    private const val MAX_TOKENS = 1024L

    private val SYSTEM_PROMPT = """
        You are a git commit message generator. Analyze the provided git diff
        and generate a concise commit message following the Conventional Commits specification.

        Format: <type>(<optional scope>): <description>

        Types: feat, fix, docs, style, refactor, perf, test, build, ci, chore

        Rules:
        - Keep the first line (subject) under 72 characters
        - Use imperative mood ("add" not "added")
        - Do not end the subject with a period
        - If the change is complex, add a body after a blank line with bullet points
        - Only output the commit message, nothing else
    """.trimIndent()

    suspend fun generateCommitMessage(diff: String): String = withContext(Dispatchers.IO) {
        val settings = AppSettings.instance
        val apiKey = settings.apiKey
        if (apiKey.isBlank()) {
            throw IllegalStateException("Anthropic API key is not configured. Go to Settings > Tools > AI Commit Message.")
        }

        val client = AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
            .build()

        val locale = settings.locale
        val localeInstruction = if (locale == "ru") {
            "\nWrite the commit message in Russian."
        } else {
            "\nWrite the commit message in English."
        }

        val systemPrompt = if (settings.customPrompt.isNotBlank()) {
            settings.customPrompt
        } else {
            SYSTEM_PROMPT + localeInstruction
        }

        val model = when (settings.model) {
            "claude-sonnet-4-6" -> Model.CLAUDE_SONNET_4_6
            "claude-haiku-4-5-20251001" -> Model.CLAUDE_HAIKU_4_5_20251001
            "claude-opus-4-6" -> Model.CLAUDE_OPUS_4_6
            else -> Model.CLAUDE_SONNET_4_6
        }

        val params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(MAX_TOKENS)
            .system(MessageCreateParams.System.ofString(systemPrompt))
            .addUserMessage(diff)
            .build()

        val message = client.messages().create(params)
        val result = message.content()
            .filter { it.isText() }
            .joinToString("") { it.asText().text() }

        client.close()
        result.trim()
    }
}
