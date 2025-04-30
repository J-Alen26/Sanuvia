package com.uv.sanuvia.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uv.sanuvia.data.repository.Usuario
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class RegisterState {
    object Idle : RegisterState()
    object Loading : RegisterState()
    data class Success(val user: FirebaseUser?) : RegisterState()
    data class Error(val message: String) : RegisterState()
}

class RegisterScreenViewModel : ViewModel() {

    private val usuarioRepository = Usuario()

    private val _registrationState = MutableStateFlow<RegisterState>(RegisterState.Idle)
    val registrationState: StateFlow<RegisterState> = _registrationState

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