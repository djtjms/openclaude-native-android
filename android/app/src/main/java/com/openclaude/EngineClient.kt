package com.openclaude

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Represents a single event from the chat engine.
 */
sealed interface ChatEvent {
    data class Text(val text: String) : ChatEvent
    data class Thinking(val text: String) : ChatEvent
    data class ToolUse(val name: String, val input: String) : ChatEvent
    data class Done(val sessionId: String) : ChatEvent
    data class Error(val message: String) : ChatEvent
}

/**
 * Chat engine adapter with two backends:
 *
 * 1. **Exec mode** (default) — spawns `openclaude -p --verbose --output-format stream-json`
 *    directly via `ProcessBuilder`. Used when the binary is available on the device.
 *
 * 2. **Http mode** (fallback) — POSTs to the existing Termux bridge server at
 *    `http://127.0.0.1:8787/api/chat` and reads SSE events.
 */
class EngineClient(
    private val bridgeUrl: String = "http://127.0.0.1:8787",
    private val openclaudeBin: String = "openclaude"
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var useExec = false

    /** Probe whether the openclaude binary is available on the device. */
    fun probe(): Boolean {
        return try {
            val proc = ProcessBuilder("which", openclaudeBin)
                .redirectErrorStream(true)
                .start()
            val exit = proc.waitFor()
            useExec = exit == 0
            useExec
        } catch (_: Exception) {
            useExec = false
            false
        }
    }

    /** Send a message and stream back events. */
    fun chat(message: String, sessionId: String = ""): Flow<ChatEvent> = flow {
        if (useExec) {
            emitAll(chatExec(message, sessionId))
        } else {
            emitAll(chatHttp(message, sessionId))
        }
    }.flowOn(Dispatchers.IO)

    // ── Exec mode ──────────────────────────────────────────────────────────

    private fun chatExec(message: String, sessionId: String): Flow<ChatEvent> = flow {
        val args = mutableListOf("-p", "--verbose", "--output-format", "stream-json")
        if (sessionId.isNotBlank()) {
            args.add("--resume")
            args.add(sessionId)
        }
        args.add(message)

        val proc = ProcessBuilder(openclaudeBin, *args.toTypedArray())
            .redirectErrorStream(true)
            .start()

        val reader = BufferedReader(InputStreamReader(proc.inputStream))
        var line: String?
        var resolvedSessionId = sessionId

        while (reader.readLine().also { line = it } != null) {
            val evt = parseStreamJson(line!!) ?: continue
            when (evt.type) {
                "system" -> {
                    if (evt.subtype == "init" && evt.sessionId != null) {
                        resolvedSessionId = evt.sessionId
                    }
                }
                "assistant" -> {
                    val content = evt.content
                    if (content != null) {
                        for (part in content) {
                            when (part.type) {
                                "text" -> emit(ChatEvent.Text(part.text ?: ""))
                                "thinking" -> emit(ChatEvent.Thinking(part.thinking ?: ""))
                                "tool_use" -> emit(ChatEvent.ToolUse(part.name ?: "tool", part.input ?: ""))
                            }
                        }
                    }
                }
                "result" -> {
                    if (evt.sessionId != null) resolvedSessionId = evt.sessionId
                    if (evt.subtype == "error" || evt.subtype == "error_during_execution") {
                        emit(ChatEvent.Error(evt.errors?.joinToString("; ") ?: "Unknown error"))
                    } else {
                        emit(ChatEvent.Done(resolvedSessionId))
                    }
                    return@flow
                }
            }
        }

        val exitCode = proc.waitFor()
        if (exitCode != 0) {
            emit(ChatEvent.Error("openclaude exited with code $exitCode"))
        } else {
            emit(ChatEvent.Done(resolvedSessionId))
        }
    }

    // ── HTTP mode (SSE bridge) ─────────────────────────────────────────────

    private fun chatHttp(message: String, sessionId: String): Flow<ChatEvent> = flow {
        val url = URL("$bridgeUrl/api/chat")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 15_000
        conn.readTimeout = 60_000

        val body = buildString {
            append("{\"message\":")
            append(JsonPrimitive(message).toString())
            if (sessionId.isNotBlank()) {
                append(",\"session_id\":")
                append(JsonPrimitive(sessionId).toString())
            }
            append("}")
        }
        conn.outputStream.write(body.toByteArray())

        val reader = BufferedReader(InputStreamReader(conn.inputStream))
        var line: String?
        var resolvedSessionId = sessionId

        while (reader.readLine().also { line = it } != null) {
            val l = line!!.trim()
            if (l.isBlank()) continue

            // SSE format: "event: <type>\ndata: <json>\n\n"
            // We receive one line at a time via readLine.
            if (l.startsWith("event: ")) {
                val eventType = l.removePrefix("event: ")
                // Next readLine will be "data: ..."
                val dataLine = reader.readLine() ?: continue
                val data = dataLine.removePrefix("data: ").trim()

                if (data.isBlank()) continue

                try {
                    val obj = json.parseToJsonElement(data).jsonObject
                    when (eventType) {
                        "message" -> {
                            val text = obj["text"]?.jsonPrimitive?.content ?: ""
                            if (text.isNotBlank()) emit(ChatEvent.Text(text))
                        }
                        "thinking" -> {
                            val text = obj["text"]?.jsonPrimitive?.content ?: ""
                            if (text.isNotBlank()) emit(ChatEvent.Thinking(text))
                        }
                        "tool_use" -> {
                            val name = obj["name"]?.jsonPrimitive?.content ?: "tool"
                            val input = obj["input"]?.toString() ?: ""
                            emit(ChatEvent.ToolUse(name, input))
                        }
                        "done" -> {
                            resolvedSessionId = obj["session_id"]?.jsonPrimitive?.content ?: resolvedSessionId
                            emit(ChatEvent.Done(resolvedSessionId))
                            return@flow
                        }
                        "error" -> {
                            val msg = obj["message"]?.jsonPrimitive?.content ?: "Unknown error"
                            emit(ChatEvent.Error(msg))
                            return@flow
                        }
                    }
                } catch (_: Exception) {
                    // skip malformed SSE data
                }
            }
        }
    }

    // ── JSON parsing helpers ────────────────────────────────────────────────

    private data class StreamEvent(
        val type: String?,
        val subtype: String? = null,
        val sessionId: String? = null,
        val content: List<ContentPart>? = null,
        val errors: List<String>? = null,
        val result: String? = null
    )

    private data class ContentPart(
        val type: String?,
        val text: String? = null,
        val thinking: String? = null,
        val name: String? = null,
        val input: String? = null
    )

    private fun parseStreamJson(line: String): StreamEvent? {
        return try {
            val obj = json.parseToJsonElement(line).jsonObject
            StreamEvent(
                type = obj["type"]?.jsonPrimitive?.contentOrNull,
                subtype = obj["subtype"]?.jsonPrimitive?.contentOrNull,
                sessionId = obj["session_id"]?.jsonPrimitive?.contentOrNull,
                content = obj["message"]?.jsonObject?.get("content")?.jsonArray?.mapNotNull { part ->
                    val p = part.jsonObject
                    ContentPart(
                        type = p["type"]?.jsonPrimitive?.contentOrNull,
                        text = p["text"]?.jsonPrimitive?.contentOrNull,
                        thinking = p["thinking"]?.jsonPrimitive?.contentOrNull,
                        name = p["name"]?.jsonPrimitive?.contentOrNull,
                        input = p["input"]?.toString()
                    )
                },
                errors = obj["errors"]?.jsonArray?.map { it.jsonPrimitive.content },
                result = obj["result"]?.jsonPrimitive?.contentOrNull
            )
        } catch (_: Exception) {
            null
        }
    }
}