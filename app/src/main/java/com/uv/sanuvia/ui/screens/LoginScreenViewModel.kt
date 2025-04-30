package com.uv.sanuvia.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uv.sanuvia.data.repository.Usuario
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuth
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

sealed class PasswordResetState {
    object Idle : PasswordResetState()
    object Loading : PasswordResetState()
    object Success : PasswordResetState()
    data class Error(val message: String) : PasswordResetState()
}

class LoginScreenViewModel : ViewModel() {

    private val usuarioRepository = Usuario()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val _passwordResetState = MutableStateFlow<PasswordResetState>(PasswordResetState.Idle)
    val passwordResetState: StateFlow<PasswordResetState> = _passwordResetState

    fun login(email: String, password: String) {
        _loginState.value = LoginState.Loading

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

    fun recoverPassword(email: String) {
        _passwordResetState.value = PasswordResetState.Loading
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if(task.isSuccessful){
                    _passwordResetState.value = PasswordResetState.Success
                } else {
                    _passwordResetState.value = PasswordResetState.Error(task.exception?.message ?: "Error desconocido")
                }
            }
    }
}