package com.uv.kooxi.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.uv.kooxi.data.repository.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UserEditState(
    val username: String = "",
    val photoUrl: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class UserEditScreenViewModel : ViewModel() {

    private val usuarioRepo = Usuario()
    private val _state = MutableStateFlow(UserEditState())
    val state: StateFlow<UserEditState> = _state

    init {
        cargarDatosUsuario()
    }

    private fun cargarDatosUsuario() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let {
            _state.value = _state.value.copy(
                username = it.displayName ?: "",
                photoUrl = it.photoUrl?.toString() ?: ""
            )
        }
    }

    fun actualizarUsuario(nuevoNombre: String) {
        _state.value = _state.value.copy(isLoading = true, error = null, success = false)

        viewModelScope.launch {
            val result = usuarioRepo.editarUsuario(username = nuevoNombre)

            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    username = nuevoNombre,
                    isLoading = false,
                    success = true
                )
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun eliminarCuenta() {
        // Set loading state and reset error and success flags
        _state.value = _state.value.copy(isLoading = true, error = null, success = false)
        viewModelScope.launch {
            // Llamar a la función de eliminación de usuario del repositorio
            val result = usuarioRepo.eliminarPerfil()
            if (result.isSuccess) {
                _state.value = _state.value.copy(isLoading = false, success = true)
            } else {
                _state.value = _state.value.copy(isLoading = false, error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun subirFotoPerfil(fotoUri: Uri) {
        _state.value = _state.value.copy(isLoading = true, error = null, success = false)
        viewModelScope.launch {
            // Llamar al método de la clase Usuario que sube la foto de perfil
            val result = usuarioRepo.subirFotoPerfil(fotoUri)
            if (result.isSuccess) {
                // Se espera que el método retorne la URL de la foto subida
                _state.value = _state.value.copy(
                    photoUrl = result.getOrNull() ?: "",
                    isLoading = false,
                    success = true
                )
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }
}