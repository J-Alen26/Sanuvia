package com.uv.sanuvia.ui.screens // Asegúrate que el paquete sea el correcto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uv.sanuvia.data.repository.Usuario // Asegúrate que la ruta a tu Repositorio Usuario es correcta
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow // Importa asStateFlow si no lo tienes
import kotlinx.coroutines.launch

// Define los posibles estados del login
sealed class LoginState {
    object Idle : LoginState() // Estado inicial o después de resetear
    object Loading : LoginState()
    data class Success(val user: FirebaseUser?) : LoginState()
    data class Error(val message: String) : LoginState()
}

// Define los posibles estados para la recuperación de contraseña
sealed class PasswordResetState {
    object Idle : PasswordResetState() // Estado inicial o después de resetear
    object Loading : PasswordResetState()
    object Success : PasswordResetState()
    data class Error(val message: String) : PasswordResetState()
}

class LoginScreenViewModel : ViewModel() {

    // Asume que tienes tu repositorio Usuario para manejar la lógica de Firebase Auth
    private val usuarioRepository = Usuario()

    // StateFlow para el estado del proceso de login
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow() // Usa asStateFlow para exponerlo

    // StateFlow para el estado del proceso de recuperación de contraseña
    private val _passwordResetState = MutableStateFlow<PasswordResetState>(PasswordResetState.Idle)
    val passwordResetState: StateFlow<PasswordResetState> = _passwordResetState.asStateFlow() // Usa asStateFlow

    /**
     * Inicia el proceso de login con email y contraseña.
     */
    fun login(email: String, password: String) {
        _loginState.value = LoginState.Loading // Indica que la operación está en curso

        viewModelScope.launch {
            // Llama a la función de tu repositorio para iniciar sesión
            val result = usuarioRepository.iniciarSesion(email, password)

            // Actualiza el estado según el resultado de la operación
            if (result.isSuccess) {
                _loginState.value = LoginState.Success(result.getOrNull())
            } else {
                _loginState.value = LoginState.Error(
                    result.exceptionOrNull()?.message ?: "Error desconocido al iniciar sesión"
                )
            }
        }
    }

    /**
     * Inicia el proceso de recuperación de contraseña para el email proporcionado.
     */
    fun recoverPassword(email: String) {
        _passwordResetState.value = PasswordResetState.Loading // Indica carga

        // Usa FirebaseAuth directamente para enviar el correo de recuperación
        // Podrías mover esto a tu repositorio Usuario también si prefieres
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _passwordResetState.value = PasswordResetState.Success
                } else {
                    _passwordResetState.value = PasswordResetState.Error(
                        task.exception?.message ?: "Error desconocido al enviar correo de recuperación"
                    )
                }
            }
    }

    /**
     * Resetea el estado del login a Idle.
     * Útil para llamar desde la UI después de que un mensaje de error/éxito se ha mostrado
     * o después de una navegación.
     */
    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }

    /**
     * Resetea el estado de recuperación de contraseña a Idle.
     * Útil para llamar desde la UI después de que un mensaje se ha mostrado.
     */
    fun resetPasswordResetState() {
        _passwordResetState.value = PasswordResetState.Idle
    }
}
