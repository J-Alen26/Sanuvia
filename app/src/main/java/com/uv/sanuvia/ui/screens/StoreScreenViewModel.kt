package com.uv.sanuvia.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

// --- Modelos de Datos Simulados ---
data class CultivoLocalItem(
    val id: String = UUID.randomUUID().toString(),
    val nombre: String,
    val descripcion: String,
    val precioPorUnidad: String, // Ej: "$25 / kg", "$10 / pieza"
    val imageUrl: String, // URL de imagen simulada
    val vendedorNombre: String = "Productor Local", // Podría venir del perfil del usuario
    val ubicacion: String = "Coatzacoalcos, Ver."
)

// --- Estado del UI ---
data class StoreScreenState(
    val cultivos: List<CultivoLocalItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // Para la creación de nuevos cultivos a ofrecer
    val nombreNuevoCultivo: String = "",
    val descripcionNuevoCultivo: String = "",
    val precioNuevoCultivo: String = "", // Ej: "20"
    val unidadNuevoCultivo: String = "kg", // Ej: "kg", "pieza", "manojo"
    val mostrandoDialogoPago: Boolean = false,
    val pagoExitoso: Boolean? = null,
    val isPublicandoCultivo: Boolean = false,
    val mensajePublicacion: String? = null
)

class StoreScreenViewModel : ViewModel() {

    private val _state = MutableStateFlow(StoreScreenState())
    val state: StateFlow<StoreScreenState> = _state.asStateFlow()

    private val nombresCultivosCoatza = listOf(
        "Mango Ataulfo Fresco", "Plátano Roatán Maduro", "Piña Miel Dulce", "Limón Persa Jugoso", "Naranja Valencia Temprana",
        "Papaya Maradol Grande", "Chile Habanero Picante", "Tomate Saladette Rojo", "Cilantro Fresco de Patio", "Mamey Delicioso"
    )
    private val descripcionesCultivos = listOf(
        "Cosechado esta mañana, dulzura garantizada.", "Perfecto para licuados o como snack.", "Directo del campo, ideal para jugos y postres.",
        "Ácido y refrescante, para tus platillos y bebidas.", "Rica en vitamina C, jugosa y dulce.", "Pulpa suave y dulce, excelente para el desayuno.",
        "El toque picante auténtico para tus salsas.", "Maduro y listo para ensaladas y guisos.", "Aroma y sabor inigualable para tus comidas.",
        "Textura cremosa y sabor único, un manjar local."
    )
    private val unidadesComunes = listOf("kg", "pieza", "docena", "manojo", "reja")

    init {
        cargarCultivosIniciales()
    }

    fun cargarCultivosIniciales() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            delay(1200) // Simula carga de red

            val cultivosSimulados = List(5) { index -> // Generar 5 cultivos iniciales
                val nombreCultivo = nombresCultivosCoatza.random()
                CultivoLocalItem(
                    id = UUID.randomUUID().toString(),
                    nombre = nombreCultivo,
                    descripcion = descripcionesCultivos.random(),
                    precioPorUnidad = "$${Random.nextInt(10, 80)} / ${unidadesComunes.random()}",
                    imageUrl = "https://picsum.photos/seed/${nombreCultivo.replace(" ", "")}/400/300",
                    vendedorNombre = "Familia Hernández" // Ejemplo
                )
            }
            _state.update {
                it.copy(
                    isLoading = false,
                    cultivos = cultivosSimulados
                )
            }
        }
    }

    // --- Funciones para la creación de nuevos cultivos ---
    fun onNombreCultivoCambiado(nombre: String) {
        _state.update { it.copy(nombreNuevoCultivo = nombre, mensajePublicacion = null) }
    }

    fun onDescripcionCultivoCambiada(descripcion: String) {
        _state.update { it.copy(descripcionNuevoCultivo = descripcion, mensajePublicacion = null) }
    }

    fun onPrecioCultivoCambiado(precioStr: String) {
        _state.update { it.copy(precioNuevoCultivo = precioStr, mensajePublicacion = null) }
    }

    fun onUnidadCultivoCambiada(unidad: String) {
        _state.update { it.copy(unidadNuevoCultivo = unidad, mensajePublicacion = null) }
    }


    fun iniciarPublicacionCultivo() {
        val currentState = _state.value
        if (currentState.nombreNuevoCultivo.isBlank() ||
            currentState.descripcionNuevoCultivo.isBlank() ||
            currentState.precioNuevoCultivo.isBlank() ||
            currentState.unidadNuevoCultivo.isBlank()) {
            _state.update { it.copy(mensajePublicacion = "Todos los campos son obligatorios para ofrecer tu cultivo.") }
            return
        }
        try {
            currentState.precioNuevoCultivo.toDouble()
        } catch (e: NumberFormatException) {
            _state.update { it.copy(mensajePublicacion = "El precio debe ser un número válido.") }
            return
        }

        _state.update { it.copy(mostrandoDialogoPago = true, pagoExitoso = null, mensajePublicacion = null) }
    }

    fun procesarPagoParaPublicar(exito: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(mostrandoDialogoPago = false, isPublicandoCultivo = true) }
            delay(1500) // Simula procesamiento de pago

            if (exito) {
                _state.update { it.copy(pagoExitoso = true) }
                val currentState = _state.value
                val nuevoCultivo = CultivoLocalItem(
                    nombre = currentState.nombreNuevoCultivo,
                    descripcion = currentState.descripcionNuevoCultivo,
                    precioPorUnidad = "$${currentState.precioNuevoCultivo} / ${currentState.unidadNuevoCultivo}",
                    imageUrl = "https://picsum.photos/seed/${currentState.nombreNuevoCultivo.replace(" ", "") + Random.nextInt()}/400/300",
                    vendedorNombre = "Tú (Productor Local)" // Simulado
                )
                _state.update {
                    it.copy(
                        cultivos = listOf(nuevoCultivo) + it.cultivos,
                        isPublicandoCultivo = false,
                        nombreNuevoCultivo = "",
                        descripcionNuevoCultivo = "",
                        precioNuevoCultivo = "",
                        // unidadNuevoCultivo = "kg", // Podrías resetearlo o mantener la última selección
                        mensajePublicacion = "¡Tu cultivo ha sido ofrecido con éxito!"
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        pagoExitoso = false,
                        isPublicandoCultivo = false,
                        mensajePublicacion = "El pago falló. No se pudo ofrecer el cultivo."
                    )
                }
            }
            delay(3500)
            _state.update { it.copy(pagoExitoso = null, mensajePublicacion = null) }
        }
    }

    fun cerrarDialogoPago() {
        _state.update { it.copy(mostrandoDialogoPago = false) }
    }

    fun limpiarMensaje() {
        _state.update { it.copy(mensajePublicacion = null) }
    }
}