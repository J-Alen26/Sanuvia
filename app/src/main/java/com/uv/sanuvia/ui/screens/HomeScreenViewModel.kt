package com.uv.sanuvia.ui.screens

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uv.sanuvia.data.repository.Usuario
import com.uv.sanuvia.data.repository.EnfermedadInfantil
import com.uv.sanuvia.data.repository.EnfermedadInfantil.EnfermedadInfantilModel
import com.uv.sanuvia.data.repository.EscaneoAlimento
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class HomeScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val usuarioRepo = Usuario()
    private val enfermedadRepo = EnfermedadInfantil()
    private val escaneoAlimento = EscaneoAlimento(usuarioRepo)
    private val _scanResult = MutableStateFlow<String?>(null)
    val scanResult = _scanResult.asStateFlow()
    private val _articleContent = MutableStateFlow<List<EnfermedadInfantilModel>>(emptyList())
    val articleContent = _articleContent.asStateFlow()

    private val _userLocation = MutableStateFlow<Location?>(null)
    val userLocation = _userLocation.asStateFlow()

    private val _openAiResponse = MutableStateFlow("")
    val openAiResponse = _openAiResponse.asStateFlow()


    companion object {
        private const val TAG = "HomeScreenViewModel"
        private const val OPENAI_URL = "https://api.openai.com/v1/chat/completions"
        private const val OPENAI_KEY = "Bearer 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"

    }

    fun fetchLocation() = viewModelScope.launch(Dispatchers.IO) {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "Permiso de ubicación no concedido")
            return@launch
        }
        usuarioRepo.obtenerUbicacionUsuario(context)
            .onSuccess { location ->
                _userLocation.value = location
                Log.d(TAG, "Ubicación obtenida: $location")
            }
            .onFailure { exception ->
                Log.e(TAG, "Error al obtener la ubicación: ${exception.message}")
            }
    }

    fun consultarCultivosPorUbicacion() = viewModelScope.launch(Dispatchers.IO) {
        val location = userLocation.value
        if (location == null) {
            _openAiResponse.value = "Ubicación no disponible"
            return@launch
        }
    }

    /**
     * Inicia el escaneo de un alimento enviando la imagen en Base64
     * y publica el resultado en scanResult.
     */
    fun scanAlimento(base64Image: String) {
        escaneoAlimento.scanAlimento(base64Image).observeForever { res ->
            _scanResult.value = res
        }
    }

    fun logout() {
        usuarioRepo.cerrarSesion()
    }

    init {
        // Carga de enfermedades infantiles desde Firestore
        enfermedadRepo.getEnfermedadesInfantiles().observeForever { list ->
            _articleContent.value = list ?: emptyList()
        }
    }
}