package com.uv.sanuvia.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.uv.sanuvia.data.repository.Comentario
import com.uv.sanuvia.data.repository.ComentarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ComentariosState(
    val comentarios: List<Comentario> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCreating: Boolean = false,
    val mostrandoDialogoEliminar: Boolean = false,
    val idComentarioAEliminar: String? = null,
    val publicacionActualId: String? = null,
    val usuarioActualId: String? = null
)

class ComentariosViewModel(application: Application) : AndroidViewModel(application) {

    private val comentarioRepository = ComentarioRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _state = MutableStateFlow(ComentariosState(
        usuarioActualId = auth.currentUser?.uid
    ))
    val state: StateFlow<ComentariosState> = _state.asStateFlow()

    fun cargarComentarios(publicacionId: String) {
        _state.update { it.copy(publicacionActualId = publicacionId) }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = comentarioRepository.obtenerComentariosPorPublicacion(publicacionId)
            result.onSuccess { listaComentarios ->
                _state.update { it.copy(isLoading = false, comentarios = listaComentarios, error = null) }
            }.onFailure { exception ->
                _state.update { it.copy(isLoading = false, error = exception.localizedMessage) }
            }
        }
    }

    fun crearComentario(texto: String) {
        if (texto.isBlank()) {
            _state.update { it.copy(error = "El comentario no puede estar vacío") }
            return
        }

        val usuarioActualId = obtenerUsuarioActualId()
        if (usuarioActualId == null) {
            _state.update { it.copy(error = "Debes iniciar sesión para comentar") }
            return
        }

        val publicacionId = _state.value.publicacionActualId
        if (publicacionId == null) {
            _state.update { it.copy(error = "No se ha seleccionado una publicación") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, error = null) }
            val nuevoComentario = Comentario(publicacionId = publicacionId, authorId = usuarioActualId, text = texto)
            val result = comentarioRepository.crearComentario(nuevoComentario)
            result.onSuccess { _ ->
                _state.update { it.copy(isCreating = false, error = null) }
                cargarComentarios(publicacionId)
            }.onFailure { exception ->
                _state.update { it.copy(isCreating = false, error = exception.localizedMessage) }
            }
        }
    }

    fun mostrarDialogoEliminar(idComentario: String) {
        val comentario = _state.value.comentarios.find { it.idComentario == idComentario }

        if (comentario != null && esAutorDelComentario(comentario)) {
            _state.update { it.copy(mostrandoDialogoEliminar = true, idComentarioAEliminar = idComentario) }
        } else {
            _state.update { it.copy(error = "No tienes permiso para eliminar este comentario") }
        }
    }

    fun confirmarEliminarComentario() {
        val idComentario = _state.value.idComentarioAEliminar ?: return
        val usuarioActualId = obtenerUsuarioActualId() ?: return
        val publicacionId = _state.value.publicacionActualId ?: return

        viewModelScope.launch {
            _state.update { it.copy(mostrandoDialogoEliminar = false) }

            val result = comentarioRepository.eliminarComentario(idComentario, usuarioActualId)
            result.onSuccess {
                cargarComentarios(publicacionId)
            }.onFailure { exception ->
                _state.update { it.copy(error = exception.localizedMessage) }
            }
        }
    }

    fun cancelarEliminarComentario() {
        _state.update { it.copy(mostrandoDialogoEliminar = false, idComentarioAEliminar = null) }
    }

    fun obtenerUsuarioActualId(): String? {
        return auth.currentUser?.uid
    }

    fun esAutorDelComentario(comentario: Comentario): Boolean {
        val usuarioActualId = obtenerUsuarioActualId()
        return usuarioActualId != null && usuarioActualId == comentario.authorId
    }

    fun limpiarError() {
        _state.update { it.copy(error = null) }
    }
}