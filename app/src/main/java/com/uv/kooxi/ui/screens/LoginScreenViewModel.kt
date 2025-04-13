package com.uv.kooxi.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uv.kooxi.data.repository.Usuario
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Define los posibles estados del login
sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: FirebaseUser?) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginScreenViewModel : ViewModel() {

    private val usuarioRepository = Usuario()

    // Estado privado mutable y su contraparte inmutable para observar en la UI
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    // Función para realizar el login
    fun login(email: String, password: String) {
        // Actualiza el estado a Loading
        _loginState.value = LoginState.Loading

        // Lanza una corrutina en el scope del ViewModel
        viewModelScope.launch {
            val result = usuarioRepository.iniciarSesion(email, password)

            // Actualiza el estado según el resultado
            if (result.isSuccess) {
                _loginState.value = LoginState.Success(result.getOrNull())
            } else {
                _loginState.value = LoginState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            }
        }
    }
}