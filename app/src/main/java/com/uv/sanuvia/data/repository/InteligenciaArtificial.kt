package com.uv.sanuvia.data.repository

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class InteligenciaArtificial {

    private val client = OkHttpClient()
    private val apiKey = "Bearer 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"
    private val apiUrl = "https://api.openai.com/v1/chat/completions"


    fun generarInformacion(base64Image: String): String {
        val systemContent = """
            Eres un especialista en nutrición y seguridad alimentaria.
            Recibirás una imagen codificada en Base64 de un producto alimenticio.
            Devuelve una descripción de sus nutrientes, calorías aproximadas y recomendaciones de consumo.
        """.trimIndent()

        // Mensajes para la API de chat
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemContent)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", "data:image/png;base64,$base64Image")
            })
        }

        // Cuerpo de la petición
        val jsonBody = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", messages)
            put("temperature", 0.7)
        }
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toString().toRequestBody(mediaType)

        // Construcción y envío de la petición HTTP
        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("Error OpenAI: ${response.code} ${response.message}")
            }
            val respStr = response.body?.string() ?: throw Exception("Respuesta vacía de OpenAI")
            val respJson = JSONObject(respStr)
            return respJson
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
        }
    }
}