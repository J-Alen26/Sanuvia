package com.uv.sanuvia.ui.screens

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri // Importa Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uv.sanuvia.data.repository.Usuario
import com.uv.sanuvia.data.repository.EnfermedadInfantil
import com.uv.sanuvia.data.repository.EnfermedadInfantil.EnfermedadInfantilModel
// Asegúrate que la ruta de importación sea correcta
import com.uv.sanuvia.data.repository.EscaneoRepository
import com.uv.sanuvia.data.repository.EscaneoAlimento // Importa tu modelo de datos
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow // Importa StateFlow
import kotlinx.coroutines.flow.update // Importa update
import kotlinx.coroutines.launch
import kotlin.Result // Importa Result

// Define un data class para el estado específico de esta pantalla si se vuelve complejo,
// o maneja estados individuales como se muestra aquí.
data class HomeScreenState(
    val isLocationLoading: Boolean = false,
    val userLocation: Location? = null,
    val locationError: String? = null,
    val articles: List<EnfermedadInfantilModel> = emptyList(),
    val isScanning: Boolean = false, // Para el proceso de escaneo de alimento
    val scanError: String? = null,   // Error específico del escaneo
    val lastScanId: String? = null, // ID del último escaneo exitoso (opcional)
    val misEscaneos: List<EscaneoAlimento> = emptyList(), // Lista de escaneos del usuario
    val isScanListLoading: Boolean = false // Loading para la lista de escaneos
    // Puedes añadir más estados según necesites
)


class HomeScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val usuarioRepo = Usuario()
    private val enfermedadRepo = EnfermedadInfantil()
    private val escaneoRepository = EscaneoRepository() // <-- Añade instancia del nuevo repo

    // Un único StateFlow para manejar todo el estado de la pantalla
    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()

    // --- Estados Anteriores (Integrados en HomeScreenState o eliminados si no se usan) ---
    // private val _scanResult = MutableStateFlow<String?>(null) // Evalúa si aún lo necesitas
    // val scanResult = _scanResult.asStateFlow()
    // private val _articleContent = MutableStateFlow<List<EnfermedadInfantilModel>>(emptyList()) // Ahora en _state.articles
    // val articleContent = _articleContent.asStateFlow()
    // private val _userLocation = MutableStateFlow<Location?>(null) // Ahora en _state.userLocation
    // val userLocation = _userLocation.asStateFlow()
    // private val _openAiResponse = MutableStateFlow("") // Evalúa si aún lo necesitas
    // val openAiResponse = _openAiResponse.asStateFlow()
    // --- Fin Estados Anteriores ---


    companion object {
        private const val TAG = "HomeScreenViewModel"
        // --- CLAVE API ELIMINADA --- ¡BIEN!
        // private const val OPENAI_URL = "..." // Ya no se necesita aquí
        // private const val OPENAI_KEY = "..." // ¡NUNCA MÁS AQUÍ!
    }

    init {
        cargarEnfermedades() // Carga inicial de artículos
        fetchLocation()     // Intenta obtener ubicación inicial
        cargarMisEscaneos() // Carga inicial de escaneos del usuario
    }

    // --- Funciones existentes (adaptadas para usar _state) ---
    fun fetchLocation() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isLocationLoading = true, locationError = null) }
            val context = getApplication<Application>()
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "Permiso de ubicación no concedido")
                _state.update { it.copy(isLocationLoading = false, locationError = "Permiso de ubicación no concedido.") }
                return@launch
            }

            usuarioRepo.obtenerUbicacionUsuario(context)
                .onSuccess { location ->
                    _state.update { it.copy(isLocationLoading = false, userLocation = location) }
                    Log.d(TAG, "Ubicación obtenida: $location")
                    // Podrías llamar a consultarCultivos aquí si quieres que se haga auto
                    // consultarCultivosPorUbicacion()
                }
                .onFailure { exception ->
                    Log.e(TAG, "Error al obtener la ubicación: ${exception.message}")
                    _state.update { it.copy(isLocationLoading = false, locationError = "Error al obtener ubicación: ${exception.localizedMessage}") }
                }
        }
    }

    // Ejemplo: adaptarías esta función si sigue siendo necesaria
    fun consultarCultivosPorUbicacion() {
        viewModelScope.launch(Dispatchers.IO) {
            val location = _state.value.userLocation
            if (location == null) {
                // Actualizar estado con error o mensaje apropiado
                // _state.update { it.copy(cultivoError = "Ubicación no disponible") }
                Log.w(TAG, "Ubicación no disponible para consulta de cultivos")
                return@launch
            }
            // ... Lógica para llamar a la API o repositorio con la ubicación ...
        }
    }


    fun logout() {
        // Considera limpiar el estado si es necesario al hacer logout
        usuarioRepo.cerrarSesion()
    }

    private fun cargarEnfermedades() {
        // Asegúrate que esto no cause problemas si se llama múltiples veces
        // o si el LiveData emite valores continuamente. Considera usar Flows.
        enfermedadRepo.getEnfermedadesInfantiles().observeForever { list ->
            _state.update { it.copy(articles = list ?: emptyList()) }
        }
        // TODO: Recuerda quitar el observer en onCleared() si usas observeForever
    }

    // --- NUEVAS FUNCIONES PARA ESCANEO DE ALIMENTOS ---

    /**
     * Inicia el proceso completo para analizar una imagen de alimento.
     * Llamada desde la Vista después de obtener el Uri de la cámara/galería.
     */
    fun procesarImagenAlimento(fotoUri: Uri) {
        viewModelScope.launch { // No necesita Dispatchers.IO aquí, el repo lo maneja
            Log.d(TAG, "Iniciando procesamiento de imagen: $fotoUri")
            // Actualiza el estado para mostrar carga y limpiar errores previos
            _state.update { it.copy(isScanning = true, scanError = null, lastScanId = null) }

            val result: Result<String> = escaneoRepository.procesarYGuardarEscaneo(fotoUri)

            result.onSuccess { nuevoId ->
                Log.i(TAG, "Escaneo procesado y guardado con ID: $nuevoId")
                _state.update {
                    it.copy(
                        isScanning = false,
                        scanError = null, // Limpia error anterior si lo hubo
                        lastScanId = nuevoId // Guarda el ID del último escaneo (opcional)
                    )
                }
                // Opcional: Vuelve a cargar la lista para que incluya el nuevo item
                cargarMisEscaneos()
            }.onFailure { exception ->
                Log.e(TAG, "Error durante el procesamiento del escaneo: ${exception.message}", exception)
                _state.update {
                    it.copy(
                        isScanning = false,
                        scanError = exception.localizedMessage ?: "Error desconocido procesando imagen."
                    )
                }
            }
        }
    }

    /**
     * Carga la lista de escaneos realizados por el usuario actual desde Firestore.
     */
    fun cargarMisEscaneos() {
        viewModelScope.launch {
            Log.d(TAG, "Cargando mis escaneos...")
            _state.update { it.copy(isScanListLoading = true) } // Indicador de carga para la lista

            val result: Result<List<EscaneoAlimento>> = escaneoRepository.obtenerMisEscaneos()

            result.onSuccess { listaEscaneos ->
                Log.d(TAG, "Escaneos cargados: ${listaEscaneos.size}")
                _state.update {
                    it.copy(
                        isScanListLoading = false,
                        misEscaneos = listaEscaneos,
                        scanError = null // Limpia error si la carga fue exitosa
                    )
                }
            }.onFailure { exception ->
                Log.e(TAG, "Error al cargar mis escaneos: ${exception.message}", exception)
                _state.update {
                    it.copy(
                        isScanListLoading = false,
                        scanError = exception.localizedMessage ?: "Error desconocido cargando escaneos."
                    )
                }
            }
        }
    }

    // --- Limpieza (opcional pero recomendado) ---
    fun limpiarErrorEscaneo() {
        _state.update { it.copy(scanError = null) }
    }

    // TODO: Asegúrate de quitar el observer de LiveData en onCleared si usas observeForever
    // override fun onCleared() {
    //     super.onCleared()
    //     enfermedadRepo.getEnfermedadesInfantiles().removeObserver { ... } // Necesitas la referencia al observer
    // }
}