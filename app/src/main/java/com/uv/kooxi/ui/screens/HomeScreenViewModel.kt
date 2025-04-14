package com.uv.kooxi.ui.screens

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uv.kooxi.data.repository.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class HomeScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val usuarioRepo = Usuario()
    var userLocation: Location? = null

    private val _articleContent = MutableStateFlow(
        "Enfermedades Infantiles Más Comunes en México\n\n" +
                "Introducción:\n" +
                "La salud de nuestros pequeños es muy importante, y es común que los niños en México presenten ciertas enfermedades debido a factores ambientales y de desarrollo. En este artículo, te explicamos de forma sencilla algunas de las enfermedades infantiles más frecuentes, sus síntomas y recomendaciones para su cuidado.\n\n" +
                "1. Resfriado común:\n" +
                "Se presenta con congestión nasal, tos y, en ocasiones, fiebre leve. La mejor manera de manejar un resfriado es asegurarse de que el niño descanse y se mantenga hidratado.\n\n" +
                "2. Varicela:\n" +
                "Causa una erupción en la piel con pequeñas ampollas. Es importante evitar que el niño se rasque para prevenir infecciones secundarias y, en algunos casos, se recomienda el uso de medicamentos para aliviar la picazón.\n\n" +
                "3. Gastroenteritis:\n" +
                "Se manifiesta con vómitos y diarrea. Es fundamental mantenerlos hidratados y, si los síntomas se prolongan, buscar atención médica.\n\n" +
                "4. Infecciones respiratorias:\n" +
                "Pueden incluir bronquitis o neumonía. Ante cualquier dificultad para respirar, es vital consultar a un especialista.\n\n" +
                "Conclusión:\n" +
                "La prevención y el cuidado son fundamentales para mantener la salud infantil. Asegurarse de que los niños estén bien alimentados, vacunados y en un ambiente higiénico, así como acudir al médico ante cualquier síntoma inusual, son medidas clave para proteger a nuestros pequeños."
    )
    val articleContent = _articleContent.asStateFlow()

    fun updateArticleContent(newContent: String) {
        _articleContent.value = newContent
    }

    private val _openAiResponse = MutableStateFlow("")
    val openAiResponse = _openAiResponse.asStateFlow()

    fun consultarCultivosPorUbicacion() {
        val location = userLocation
        if (location != null) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val prompt = "Dime los cultivos mas comunes de lat=${location.latitude}, lon=${location.longitude}, limitate a solo responder con los cultivos en forma de lista"
                    val jsonPayload = """
                    {
                      "model": "gpt-4o-mini",
                      "messages": [
                        {
                          "role": "user",
                          "content": "$prompt"
                        }
                      ],
                      "temperature": 0.5,
                      "max_tokens": 100
                    }
                    """.trimIndent()
                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = jsonPayload.toRequestBody(mediaType)
                    val request = Request.Builder()
                        .url("https://api.openai.com/v1/chat/completions")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "Bearer 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")
                        .post(body)
                        .build()
                    val client = OkHttpClient()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        val responseBody = response.body?.string() ?: ""
                        val jsonObject = JSONObject(responseBody)
                        val choices = jsonObject.getJSONArray("choices")
                        if (choices.length() > 0) {
                            val text = choices.getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")
                            _openAiResponse.value = text.trim()
                        } else {
                            _openAiResponse.value = "No se recibió respuesta de OpenAI"
                        }
                    }
                } catch (e: Exception) {
                    _openAiResponse.value = "Error: ${e.message}"
                }
            }
        } else {
            _openAiResponse.value = "Ubicación no disponible"
        }
    }

    init {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            viewModelScope.launch {
                val result = usuarioRepo.obtenerUbicacionUsuario(getApplication())
                result.onSuccess { location ->
                    userLocation = location
                    Log.d("HomeScreenViewModel", "Ubicación obtenida: $location")
                }.onFailure { exception ->
                    Log.e("HomeScreenViewModel", "Error al obtener la ubicación: ${exception.message}")
                }
            }
        } else {
            Log.e("HomeScreenViewModel", "Permiso de ubicación no concedido")
        }
    }

    fun logout() {
        usuarioRepo.cerrarSesion()
    }
}