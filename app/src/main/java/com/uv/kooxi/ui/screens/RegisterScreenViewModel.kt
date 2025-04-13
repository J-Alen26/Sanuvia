package com.uv.kooxi.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uv.kooxi.data.repository.Usuario
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Define los posibles estados del registro
sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val user: FirebaseUser?) : RegisterState()
    data class Error(val message: String) : RegisterState()
}

class RegisterScreenViewModel : ViewModel() {

    private val usuarioRepository = Usuario()

    // Estado privado mutable y su contraparte inmutable para observar en la UI
    private val _registrationState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registrationState: StateFlow<RegisterState> = _registrationState

    // Función para registrar un nuevo usuario, ahora con el parámetro adicional 'username'
    fun register(email: String, password: String, username: String) {
        _registrationState.value = RegisterState.Loading

        viewModelScope.launch {
            val result = usuarioRepository.registrarse(email, password, username)

            if (result.isSuccess) {
                _registrationState.value = RegisterState.Success(result.getOrNull())
            } else {
                _registrationState.value = RegisterState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }
}