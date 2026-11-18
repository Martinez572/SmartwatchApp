package com.example.smartwatchapp.presentation

import com.example.smartwatchapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val MODEL = "claude-sonnet-5-5"

// Envía un texto a Claude y devuelve su respuesta (o el error)
suspend fun askClaude(prompt: String): Result<String> = withContext(Dispatchers.IO) {
    try {
        val body = JSONObject()
            .put("model", MODEL)
            .put("max_tokens", 500)
            .put(
                "messages",
                JSONArray().put(
                    JSONObject().put("role", "user").put("content", prompt)
                )
            )

        val connection = URL("https://api.anthropic.com/v1/messages")
            .openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        connection.setRequestProperty("content-type", "application/json")
        connection.setRequestProperty("x-api-key", BuildConfig.ANTHROPIC_API_KEY)
        connection.setRequestProperty("anthropic-version", "2023-06-01")
        connection.doOutput = true
        connection.outputStream.use { it.write(body.toString().toByteArray()) }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream.bufferedReader().use { it.readText() }

        if (code !in 200..299) {
            return@withContext Result.failure<String>(Exception("Error $code: ${responseText.take(150)}"))
        }

        // La respuesta trae bloques; juntamos los de tipo "text"
        val blocks = JSONObject(responseText).getJSONArray("content")
        val answer = StringBuilder()
        for (i in 0 until blocks.length()) {
            val block = blocks.getJSONObject(i)
            if (block.optString("type") == "text") {
                answer.append(block.optString("text"))
            }
        }
        Result.success(answer.toString().trim())
    } catch (e: Exception) {
        Result.failure<String>(e)
    }
}