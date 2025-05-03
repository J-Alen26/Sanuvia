package com.uv.sanuvia.data.repository

import com.google.firebase.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Encapsula la comunicación directa con la API de OpenAI.
 *
 * Si prefieres mayor seguridad, extrae este código a una Cloud Function
 * y sustituye la llamada HTTPS aquí por una llamada Firebase Functions.
 */
class InteligenciaArtificial(
    private val client: OkHttpClient = OkHttpClient(),
    private val apiKey: String = "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",    // <-- NO expongas la key en código
    private val apiUrl: String = "https://api.openai.com/v1/chat/completions"
) {

    /**
     * Envía la URL pública de la imagen al modelo GPT‑4o‑mini y devuelve la descripción generada.
     *
     * @param imageUrl URL HTTPS de la imagen alojada en Firebase Storage.
     * @return Texto devuelto por el modelo (nutrientes, calorías, etc.).
     * @throws Exception si la API responde con error o el cuerpo viene vacío.
     */
    suspend fun describirImagen(imageUrl: String): String = withContext(Dispatchers.IO) {

        // Mensaje de sistema: contextualiza al modelo
        val systemContent = """
            Eres un especialista en nutrición y seguridad alimentaria.
            Analiza la imagen enviada y devuélveme:
            - Descripción nutricional (macro y micronutrientes más relevantes)
            - Calorías aproximadas por porción
            - Recomendaciones de consumo o alertas (alergias, exceso de azúcares, etc.)
            Responde en un párrafo claro para el usuario final.
        """.trimIndent()

        // Construimos el JSON conforme a la especificación de vision
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", systemContent)
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", JSONArray().apply {
                    put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", JSONObject().put("url", imageUrl))
                    })
                })
            })
        }

        val bodyJson = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", messages)
            put("temperature", 0.6)
            put("max_tokens", 300)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = bodyJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("OpenAI error ${response.code}: ${response.message}")
            }

            val raw = response.body?.string()
                ?: throw Exception("Cuerpo vacío recibido de OpenAI")

            val json = JSONObject(raw)
            return@withContext json
                .getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        }
    }
}
