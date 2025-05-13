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

data class ForoScreenState(
    val publicaciones: List<PublicacionConEstadoLike> = emptyList(), // Cambiamos a una lista de PublicacionConEstadoLike
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

data class PublicacionConEstadoLike(
    val publicacion: Publicacion,
    val haDadoLike: Boolean
)

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
        cargarPublicacionesConEstadoLike()
    }

    fun cargarPublicacionesConEstadoLike() {
        viewModelScope.launch {
            _state.update { it.copy(isPublicacionesLoading = true, publicacionError = null) }
            val result: Result<List<Publicacion>> = publicacionRepository.obtenerPublicaciones()
            result.onSuccess { listaPublicaciones ->
                val publicacionesConEstado = listaPublicaciones.map { publicacion ->
                    val haDadoLikeUsuario = publicacionRepository.haDadoLike(publicacion.idPublicacion)
                    PublicacionConEstadoLike(publicacion, haDadoLikeUsuario)
                }
                _state.update {
                    it.copy(
                        isPublicacionesLoading = false,
                        publicaciones = publicacionesConEstado,
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
            // Aquí deberías obtener el username y userProfileImageUrl del usuario actual
            // y pasarlos al crear la publicación. Esto dependerá de dónde guardes
            // esa información (Firebase Auth profile, Firestore, etc.).
            val usuario = auth.currentUser
            val username = usuario?.displayName
            // Asumiendo que tienes una forma de obtener la URL de la foto de perfil
            // Podría ser desde el objeto 'usuario' o desde Firestore.
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
                cargarPublicacionesConEstadoLike()
            }.onFailure { exception ->
                _state.update { it.copy(isCreatingPublicacion = false, publicacionError = exception.localizedMessage) }
            }
        }
    }

    fun toggleLikePublicacion(idPublicacion: String, haDadoLikeActual: Boolean) {
        viewModelScope.launch {
            val result = if (haDadoLikeActual) {
                publicacionRepository.quitarLikePublicacion(idPublicacion)
            } else {
                publicacionRepository.darLikePublicacion(idPublicacion)
            }

            result.onSuccess {
                // Recargar la lista completa para actualizar el estado de like de todas las publicaciones
                cargarPublicacionesConEstadoLike()
            }.onFailure { exception ->
                _state.update { it.copy(publicacionError = exception.localizedMessage) }
            }
        }
    }

    fun mostrarDialogoEliminar(idPublicacion: String) {
        val publicacion = _state.value.publicaciones.find { it.publicacion.idPublicacion == idPublicacion }?.publicacion

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
                cargarPublicacionesConEstadoLike()
            }.onFailure { exception ->
                _state.update { it.copy(publicacionError = exception.localizedMessage) }
            }
        }
    }

    fun cancelarEliminarPublicacion() {
        _state.update { it.copy(mostrandoDialogoEliminar = false, idPublicacionAEliminar = null) }
    }

    fun mostrarDialogoEditar(publicacionConEstadoLike: PublicacionConEstadoLike) {
        val publicacion = publicacionConEstadoLike.publicacion
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
                cargarPublicacionesConEstadoLike()
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