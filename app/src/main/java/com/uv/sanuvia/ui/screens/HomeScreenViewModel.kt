package com.uv.sanuvia.ui.screens

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uv.sanuvia.data.repository.CultivoInfo
import com.uv.sanuvia.data.repository.Usuario
import com.uv.sanuvia.data.repository.EnfermedadInfantil
import com.uv.sanuvia.data.repository.EnfermedadInfantil.EnfermedadInfantilModel
import com.uv.sanuvia.data.repository.EscaneoRepository
import com.uv.sanuvia.data.repository.EscaneoAlimento
import com.uv.sanuvia.data.repository.UbicacionRepository
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.Result


data class HomeScreenState(
    val direccionUsuario: String? = null,
    val isCultivosLoading: Boolean = false,
    val cultivosError: String? = null,
    val cultivos: List<CultivoInfo> = emptyList(),
    val isLocationLoading: Boolean = false,
    val userLocation: String = "",
    val locationError: String? = null,
    val articles: List<EnfermedadInfantilModel> = emptyList(),
    val isScanning: Boolean = false,
    val scanError: String? = null,
    val lastScanId: String? = null,
    val misEscaneos: List<EscaneoAlimento> = emptyList(),
    val isScanListLoading: Boolean = false

)


class HomeScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val ubicacionRepository = UbicacionRepository()
    private val usuarioRepo = Usuario()
    private val enfermedadRepo = EnfermedadInfantil()
    private val escaneoRepository = EscaneoRepository() // <-- Añade instancia del nuevo repo

    // Un único StateFlow para manejar todo el estado de la pantalla
    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state.asStateFlow()

    companion object {
        private const val TAG = "HomeScreenViewModel"
    }

    init {
        cargarEnfermedades()
        fetchLocation()
        cargarMisEscaneos()
    }

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
                    consultarCultivosPorUbicacion()
                }
                .onFailure { exception ->
                    Log.e(TAG, "Error al obtener la ubicación: ${exception.message}")
                    _state.update { it.copy(isLocationLoading = false, locationError = "Error al obtener ubicación: ${exception.localizedMessage}") }
                }
        }
    }

    fun consultarCultivosPorUbicacion() {
        val direccion = _state.value.direccionUsuario

        if (direccion.isNullOrBlank()) {
            Log.w(TAG, "Dirección de usuario no disponible para consulta de cultivos.")
            _state.update { it.copy(cultivosError = "Obtén primero tu ubicación.") }
            return
        }

        viewModelScope.launch {
            Log.d(TAG, "Consultando cultivos para: $direccion")
            // 2. Actualiza estado: Inicia carga, limpia errores previos
            _state.update { it.copy(isCultivosLoading = true, cultivosError = null) }

            val direccion = _state.value.direccionUsuario
            if (direccion.isNullOrBlank()) {
                // ... (manejo de error si es nulo) ...
                return@launch
            }

            _state.update { it.copy(isCultivosLoading = true, cultivosError = null) }

            Log.d(TAG, "ViewModel: Llamando a obtenerCultivos con direccion = '$direccion'")

            val result: Result<List<CultivoInfo>> = ubicacionRepository.obtenerCultivos(direccion)

            // 4. Maneja el Resultado
            result.onSuccess { listaCultivos ->
                Log.i(TAG, "Cultivos obtenidos (${listaCultivos.size}) para $direccion")
                // Actualiza estado: Finaliza carga, guarda la lista, limpia error
                _state.update {
                    it.copy(
                        isCultivosLoading = false,
                        cultivos = listaCultivos,
                        cultivosError = null
                    )
                }
            }.onFailure { error ->
                Log.e(TAG, "Error al obtener cultivos para $direccion: ${error.message}", error)
                // Actualiza estado: Finaliza carga, guarda mensaje de error
                _state.update {
                    it.copy(
                        isCultivosLoading = false,
                        cultivosError = error.localizedMessage ?: "Error desconocido obteniendo cultivos."
                        // Opcional: podrías querer vaciar la lista de cultivos aquí:
                        // cultivos = emptyList()
                    )
                }
            }
        } // Fin launch
    } // Fin consultarCultivosPorUbicacion

    fun obtenerYGuardarDireccionUsuario() {
        viewModelScope.launch { // No necesitas Dispatchers.IO aquí, el repo lo maneja
            _state.update { it.copy(isLocationLoading = true, locationError = null) }
            val context = getApplication<Application>().applicationContext // Obtén el contexto

            // --- ¡AÑADIR ESTA COMPROBACIÓN DE PERMISOS! ---
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                // --- PERMISO CONCEDIDO: Continúa con la lógica ---
                Log.d(TAG, "Permiso de ubicación concedido. Obteniendo dirección...")
                val direccionResult = usuarioRepo.obtenerUbicacionUsuario(context) // Llama al repo

                direccionResult.onSuccess { direccion -> // 'direccion' es el String que viene del Geocoder
                    _state.update { it.copy(
                        isLocationLoading = false,
                        direccionUsuario = direccion, // <-- Se guarda el string aquí
                        locationError = null
                    )}
                    // Ahora que tienes la dirección, llamas a consultarCultivosPorUbicacion
                    consultarCultivosPorUbicacion()
                }.onFailure { error ->
                    _state.update { it.copy(
                        isLocationLoading = false,
                        direccionUsuario = null, // Limpia si hubo error
                        locationError = error.localizedMessage ?: "Error obteniendo dirección"
                    )}
                }
                // --- FIN LÓGICA CON PERMISO ---

            } else {
                // --- PERMISO DENEGADO: No llames a la función del repo ---
                Log.w(TAG, "Permiso de ubicación NO concedido al intentar obtener dirección.")
                _state.update {
                    it.copy(
                        isLocationLoading = false,
                        locationError = "Permiso de ubicación necesario para obtener la dirección."
                        // Opcional: Poner direccionUsuario a null si quieres limpiarlo
                        // direccionUsuario = null
                    )
                }
                // No continúes si no hay permiso
                return@launch // Sale de la coroutine
            }
            // --- FIN COMPROBACIÓN DE PERMISOS ---
        } // Fin launch
    }




    // --- Necesitarás una función para obtener y guardar la DIRECCIÓN (String) ---
// Esta función usaría tu usuarioRepo.obtenerUbicacionUsuario (que devuelve Result<String>)
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
    init {
        obtenerYGuardarDireccionUsuario()
        cargarMisEscaneos()
    }
}

