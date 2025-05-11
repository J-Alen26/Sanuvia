package com.uv.sanuvia.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uv.sanuvia.data.repository.Usuario // Asegúrate que esta ruta es correcta
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow // Importa asStateFlow
import kotlinx.coroutines.launch

// Define los posibles estados del registro
sealed class RegisterState {
    object Idle : RegisterState() // Estado inicial o después de resetear
    object Loading : RegisterState()
    data class Success(val user: FirebaseUser?) : RegisterState() // Devuelve el usuario si es exitoso
    data class Error(val message: String) : RegisterState()
}

class RegisterScreenViewModel : ViewModel() {

    private val usuarioRepository = Usuario() // Instancia de tu repositorio

    private val _registrationState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registrationState: StateFlow<RegisterState> = _registrationState.asStateFlow() // Exponer como StateFlow

    /**
     * Inicia el proceso de registro con email, contraseña y nombre de usuario.
     */
    fun register(email: String, password: String, username: String) {
        _registrationState.value = RegisterState.Loading // Indica que la operación está en curso

        viewModelScope.launch {
            // Llama a la función de tu repositorio para registrar al usuario.
            // Asegúrate que tu repositorio.registrarse (o como se llame)
            // también maneje la actualización del displayName en Firebase Auth.
            val result = usuarioRepository.registrarse(email, password, username) // Asume que esta función existe

            if (result.isSuccess) {
                _registrationState.value = RegisterState.Success(result.getOrNull())
            } else {
                _registrationState.value = RegisterState.Error(
                    result.exceptionOrNull()?.message ?: "Error desconocido durante el registro"
                )
            }
        }
    }

    /**
     * Resetea el estado del registro a Idle.
     * Se llama desde la UI después de que un mensaje de error/éxito se ha mostrado
     * o después de una navegación para limpiar el estado.
     */
    fun resetRegistrationState() {
        _registrationState.value = RegisterState.Idle
    }
}
