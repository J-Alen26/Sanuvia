package com.uv.sanuvia.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.uv.sanuvia.data.repository.Publicacion
import com.uv.sanuvia.data.repository.PublicacionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.Result
import android.util.Log

data class ForoScreenState(
    val publicaciones: List<Publicacion> = emptyList(), // Ahora la lista contiene directamente Publicacion
    val isPublicacionesLoading: Boolean = false,
    val publicacionError: String? = null,
    val isCreatingPublicacion: Boolean = false,
    val lastPublicacionId: String? = null,
    val mostrandoDialogoEliminar: Boolean = false,
    val idPublicacionAEliminar: String? = null,
    val mostrandoDialogoEditar: Boolean = false,
    val publicacionAEditar: Publicacion? = null,
    val textoEdicion: String = "",
    val usuarioActualId: String? = null
)

// Ya no necesitamos la clase PublicacionConEstadoLike

class ForoScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val publicacionRepository = PublicacionRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow(
        ForoScreenState(
            usuarioActualId = auth.currentUser?.uid
        )
    )
    val state: StateFlow<ForoScreenState> = _state.asStateFlow()

    init {
        cargarPublicaciones() // Cargamos directamente las publicaciones
    }

    fun cargarPublicaciones() {
        viewModelScope.launch {
            _state.update { it.copy(isPublicacionesLoading = true, publicacionError = null) }
            val result: Result<List<Publicacion>> = publicacionRepository.obtenerPublicaciones()
            result.onSuccess { listaPublicaciones ->
                _state.update {
                    it.copy(
                        isPublicacionesLoading = false,
                        publicaciones = listaPublicaciones,
                        publicacionError = null
                    )
                }
            }.onFailure { exception ->
                _state.update { it.copy(isPublicacionesLoading = false, publicacionError = exception.localizedMessage) }
            }
        }
    }

    fun crearPublicacion(contenido: String) {
        if (contenido.isBlank()) {
            _state.update { it.copy(publicacionError = "El contenido no puede estar vacío") }
            return
        }

        val usuarioActualId = obtenerUsuarioActualId()
        if (usuarioActualId == null) {
            _state.update { it.copy(publicacionError = "Debes iniciar sesión para publicar") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isCreatingPublicacion = true, publicacionError = null) }
            val usuario = auth.currentUser
            val username = usuario?.displayName
            val userProfileImageUrl = usuario?.photoUrl?.toString()

            val nuevaPublicacion = Publicacion(
                authorId = usuarioActualId,
                content = contenido,
                likes = 0,
                username = username,
                userProfileImageUrl = userProfileImageUrl
            )
            val result: Result<String> = publicacionRepository.crearPublicacion(nuevaPublicacion)
            result.onSuccess { nuevoId ->
                _state.update { it.copy(isCreatingPublicacion = false, publicacionError = null, lastPublicacionId = nuevoId) }
                cargarPublicaciones() // Recargamos las publicaciones
            }.onFailure { exception ->
                _state.update { it.copy(isCreatingPublicacion = false, publicacionError = exception.localizedMessage) }
            }
        }
    }

    fun toggleLikePublicacion(idPublicacion: String) {
        viewModelScope.launch {
            Log.d("ForoViewModel", "Intentando dar like a la publicación con ID: $idPublicacion")
            val result = publicacionRepository.darLikePublicacion(idPublicacion)
            result.onSuccess {
                Log.d("ForoViewModel", "Like incrementado exitosamente para la publicación con ID: $idPublicacion")
                // No necesitamos verificar si ya dio like, simplemente incrementamos
                // Opcionalmente, podrías recargar las publicaciones para ver el cambio inmediato
                cargarPublicaciones()
            }.onFailure { exception ->
                Log.e("ForoViewModel", "Error al dar like a la publicación con ID: $idPublicacion: ${exception.localizedMessage}", exception)
                _state.update { it.copy(publicacionError = exception.localizedMessage) }
            }
        }
    }

    fun mostrarDialogoEliminar(idPublicacion: String) {
        val publicacion = _state.value.publicaciones.find { it.idPublicacion == idPublicacion }

        if (publicacion != null && esAutorDeLaPublicacion(publicacion)) {
            _state.update { it.copy(mostrandoDialogoEliminar = true, idPublicacionAEliminar = idPublicacion) }
        } else {
            _state.update { it.copy(publicacionError = "No tienes permiso para eliminar esta publicación") }
        }
    }

    fun confirmarEliminarPublicacion() {
        val idPublicacion = _state.value.idPublicacionAEliminar ?: return
        val usuarioActualId = obtenerUsuarioActualId() ?: return

        viewModelScope.launch {
            _state.update { it.copy(mostrandoDialogoEliminar = false) }

            val result = publicacionRepository.eliminarPublicacion(idPublicacion, usuarioActualId)
            result.onSuccess {
                cargarPublicaciones()
            }.onFailure { exception ->
                _state.update { it.copy(publicacionError = exception.localizedMessage) }
            }
        }
    }

    fun cancelarEliminarPublicacion() {
        _state.update { it.copy(mostrandoDialogoEliminar = false, idPublicacionAEliminar = null) }
    }

    fun mostrarDialogoEditar(publicacion: Publicacion) {
        if (esAutorDeLaPublicacion(publicacion)) {
            _state.update {
                it.copy(
                    mostrandoDialogoEditar = true,
                    publicacionAEditar = publicacion,
                    textoEdicion = publicacion.content
                )
            }
        } else {
            _state.update { it.copy(publicacionError = "No tienes permiso para editar esta publicación") }
        }
    }

    fun actualizarTextoEdicion(nuevoTexto: String) {
        _state.update { it.copy(textoEdicion = nuevoTexto) }
    }

    fun guardarEdicionPublicacion() {
        val publicacion = _state.value.publicacionAEditar ?: return
        val idPublicacion = publicacion.idPublicacion
        val textoEditado = _state.value.textoEdicion
        val usuarioActualId = obtenerUsuarioActualId() ?: return

        if (textoEditado.isBlank()) {
            _state.update { it.copy(publicacionError = "El contenido no puede estar vacío") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(mostrandoDialogoEditar = false) }

            val publicacionActualizada = publicacion.copy(content = textoEditado)
            val result = publicacionRepository.actualizarPublicacion(idPublicacion, publicacionActualizada, usuarioActualId)

            result.onSuccess {
                cargarPublicaciones()
            }.onFailure { exception ->
                _state.update { it.copy(publicacionError = exception.localizedMessage) }
            }
        }
    }

    fun cancelarEdicionPublicacion() {
        _state.update {
            it.copy(
                mostrandoDialogoEditar = false,
                publicacionAEditar = null,
                textoEdicion = ""
            )
        }
    }

    fun obtenerUsuarioActualId(): String? {
        return auth.currentUser?.uid
    }

    private fun esAutorDeLaPublicacion(publicacion: Publicacion): Boolean {
        val usuarioActualId = obtenerUsuarioActualId()
        return usuarioActualId != null && usuarioActualId == publicacion.authorId
    }

    fun limpiarErrorPublicacion() {
        _state.update { it.copy(publicacionError = null) }
    }
}