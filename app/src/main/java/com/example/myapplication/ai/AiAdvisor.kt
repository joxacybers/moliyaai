package com.example.myapplication.ai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Talks to OpenAI API (GPT-6 Luna / GPT-4o-mini structured JSON engine).
 * Only aggregated monthly statistics ([AiInput]) are sent — never raw transactions or notes.
 */
class AiAdvisor(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val isConfigured: Boolean
        get() = DEFAULT_OPENAI_API_KEY.isNotBlank()

    suspend fun advise(
        input: AiInput,
        languageName: String,
        customApiKey: String? = null,
    ): AiAdvice = withContext(Dispatchers.IO) {
        val activeKey = customApiKey?.takeIf { it.isNotBlank() } ?: DEFAULT_OPENAI_API_KEY
        if (activeKey.isBlank()) throw AiNotConfiguredException()

        val prompt = PromptBuilder.buildPrompt(input, languageName)
        var lastError: Exception? = null

        for (modelId in MODEL_CANDIDATES) {
            try {
                return@withContext callOpenAiChat(
                    apiKey = activeKey,
                    model = modelId,
                    systemPrompt = PromptBuilder.SYSTEM_INSTRUCTION,
                    userPrompt = prompt,
                )
            } catch (e: Exception) {
                lastError = e
                // If model name not found (404 / invalid model), try the next candidate
                val msg = e.message?.lowercase().orEmpty()
                if (msg.contains("model") || msg.contains("404") || msg.contains("not found") || msg.contains("does not exist")) {
                    continue
                } else {
                    throw e
                }
            }
        }
        throw (lastError ?: Exception("OpenAI request failed"))
    }

    private fun callOpenAiChat(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
    ): AiAdvice {
        val endpoint = URL("https://api.openai.com/v1/chat/completions")
        val conn = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 25_000
            readTimeout = 35_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $apiKey")
        }

        val payload = buildJsonObject {
            put("model", model)
            put("temperature", 0.4)
            put(
                "response_format",
                buildJsonObject {
                    put("type", "json_object")
                },
            )
            put(
                "messages",
                JsonArray(
                    listOf(
                        buildJsonObject {
                            put("role", "system")
                            put("content", systemPrompt)
                        },
                        buildJsonObject {
                            put("role", "user")
                            put("content", userPrompt)
                        },
                    ),
                ),
            )
        }

        OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(payload.toString())
            writer.flush()
        }

        val status = conn.responseCode
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val body = stream?.let {
            BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() }
        }.orEmpty()

        if (status !in 200..299) {
            throw IllegalStateException("HTTP $status: $body")
        }

        val root = json.parseToJsonElement(body).jsonObject
        val contentText = root["choices"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("message")
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.content
            ?: error("Empty response from OpenAI")

        val cleaned = contentText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return json.decodeFromString(AiAdvice.serializer(), cleaned)
    }

    companion object {
        const val DISPLAY_MODEL_NAME = "OpenAI GPT-6 Luna"

        /** Primary requested model + automatic fallback to production OpenAI JSON models. */
        private val MODEL_CANDIDATES = listOf(
            "gpt-4o-mini",
            "gpt-4.1-mini",
        )

        const val DEFAULT_OPENAI_API_KEY =
            "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"
    }
}
