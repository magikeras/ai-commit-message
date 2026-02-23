package com.aicommitmessage.service

import com.aicommitmessage.settings.AppSettings
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AnthropicService {

    private const val MAX_TOKENS = 1024L

    private val BASE_PROMPT = """
        You are an expert git commit message generator. Analyze the git diff and produce a precise commit message following Conventional Commits.

        FORMAT:
        <type>(<scope>): <subject>

        TYPES (choose the most accurate):
        - feat: new functionality visible to users
        - fix: bug fix
        - refactor: code restructuring without behavior change
        - perf: performance improvement
        - docs: documentation only
        - style: formatting, whitespace, semicolons (no logic change)
        - test: adding or updating tests
        - build: build system, dependencies, CI config
        - chore: maintenance tasks (configs, gitignore, env files)

        SUBJECT LINE RULES:
        - Max 160 characters
        - Imperative mood: "add", "fix", "remove" (NOT "added", "fixes", "removed")
        - No period at the end
        - Describe the PRIMARY functional change, not the files modified
        - Be specific: "add Stripe webhook handler" NOT "update payment code"
        - If changes are mixed, focus on the most impactful change for the subject

        SCOPE RULES:
        - Derive scope from the domain/module affected (auth, api, ui, db, payment, config, docker)
        - Omit scope if changes span many unrelated modules
        - Use lowercase, single word or hyphenated

        BODY RULES:
        - Separate from subject with a blank line
        - Use bullet points starting with "- "
        - Each bullet = one logical change
        - Focus on WHAT changed and WHY, not HOW
        - Group related changes together
        - Mention file names only when it adds clarity (e.g. config files)
        - Skip trivial changes (whitespace, imports) unless that's all there is

        CRITICAL:
        - Output ONLY the commit message, no explanations or markdown
        - Never start subject with "Update", "Change", "Modify" — these are vague
        - Never list file names in the subject line
        - If the diff contains sensitive data (passwords, keys), mention "update credentials" without exposing values
    """.trimIndent()

    private fun buildSystemPrompt(settings: AppSettings, skipTag: String): String {
        val base = if (settings.customPrompt.isNotBlank()) settings.customPrompt else BASE_PROMPT

        val depthInstruction = when (settings.messageDepth) {
            "short" -> "\nMESSAGE DEPTH: Write ONLY the subject line. Do NOT write any body text, bullet points, or additional lines. Single line only."
            "medium" -> "\nMESSAGE DEPTH: Write a subject line + a short body (2-3 lines max) after a blank line."
            "detailed" -> "\nMESSAGE DEPTH: Write a subject line + a detailed body with bullet points covering all changes."
            else -> ""
        }

        val skipInstruction = if (skipTag != "none") {
            "\nSKIP TAG: Append '$skipTag' at the end of the subject line (first line)."
        } else {
            ""
        }

        val localeInstruction = if (settings.locale == "ru") {
            "\nLANGUAGE: Write the commit message in Russian."
        } else {
            ""
        }

        return base + depthInstruction + skipInstruction + localeInstruction
    }

    suspend fun generateCommitMessage(diff: String, skipTag: String = "none"): String = withContext(Dispatchers.IO) {
        val settings = AppSettings.instance
        val apiKey = settings.apiKey
        if (apiKey.isBlank()) {
            throw IllegalStateException("Anthropic API key is not configured.")
        }

        val client = AnthropicOkHttpClient.builder()
            .apiKey(apiKey)
            .build()

        val systemPrompt = buildSystemPrompt(settings, skipTag)

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
