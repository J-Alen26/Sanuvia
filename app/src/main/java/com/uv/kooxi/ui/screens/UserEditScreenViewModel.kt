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

    fun actualizarUsuario(nuevoNombre: String, nuevaFotoUri: Uri?) {
        _state.value = _state.value.copy(isLoading = true, error = null, success = false)

        viewModelScope.launch {
            val photoUrl = nuevaFotoUri?.let {
                val result = usuarioRepo.subirFotoPerfil(it)
                if (result.isSuccess) result.getOrNull() else null
            }

            val result = usuarioRepo.editarUsuario(username = nuevoNombre, photoUrl = photoUrl)

            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    username = nuevoNombre,
                    photoUrl = photoUrl ?: _state.value.photoUrl,
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