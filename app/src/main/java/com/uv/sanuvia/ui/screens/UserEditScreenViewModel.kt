package com.uv.sanuvia.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.material.CircularProgressIndicator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import com.uv.sanuvia.data.repository.Usuario // Assuming this repository handles Firestore/Storage interactions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.lang.Exception // Import Exception

data class UserEditState(
    val username: String = "",
    val photoUrl: String? = null, // Use nullable String for URL
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null // Use a specific success message
)

class UserEditScreenViewModel : ViewModel() {

    // It's generally recommended to inject dependencies, but for simplicity:
    private val usuarioRepo = Usuario()
    private val auth = Firebase.auth

    private val _state = MutableStateFlow(UserEditState())
    val state: StateFlow<UserEditState> = _state.asStateFlow()

    init {
        cargarDatosUsuario()
    }

    private fun cargarDatosUsuario() {
        Log.i("url", auth.currentUser?.photoUrl.toString())
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                // Ensure user data is fresh
                auth.currentUser?.reload()?.await()
                val currentUser = auth.currentUser
                _state.update {
                    it.copy(
                        username = currentUser?.displayName ?: "Usuario", // Provide a default
                        photoUrl = currentUser?.photoUrl?.toString(), // Keep as String?
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = "Error al cargar datos: ${e.localizedMessage}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun actualizarUsuario(nuevoNombre: String) {
        val trimmedNombre = nuevoNombre.trim()
        if (trimmedNombre.isEmpty()) {
            _state.update { it.copy(error = "El nombre de usuario no puede estar vacío.") }
            return
        }

        _state.update { it.copy(isLoading = true, error = null, successMessage = null) }

        viewModelScope.launch {
            try {
                // 1. Update in your custom repository (e.g., Firestore)
                // Assuming usuarioRepo.editarUsuario updates the username in Firestore/Database
                // It's better if this function returns the updated user data or confirms success clearly.
                usuarioRepo.editarUsuario(username = trimmedNombre) // Assuming this throws on failure

                // 2. Update Firebase Auth display name
                val profileUpdates = userProfileChangeRequest {
                    displayName = trimmedNombre
                }
                auth.currentUser?.updateProfile(profileUpdates)?.await()

                // 3. Update state
                _state.update {
                    it.copy(
                        username = trimmedNombre,
                        isLoading = false,
                        successMessage = "Nombre de usuario actualizado."
                    )
                }
                // Optionally reload all data if needed, though updating state directly is often sufficient
                // cargarDatosUsuario()

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al actualizar el nombre: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun actualizarFotoPerfil(fotoUri: Uri) {
        _state.update { it.copy(isLoading = true, error = null, successMessage = null) }

        viewModelScope.launch {
            try {
                // 1. Upload image and get Download URL from Repository
                // IMPORTANT: Assume usuarioRepo.actualizarFotoPerfil uploads the image to Firebase Storage
                // and returns the public download URL. Modify your repo if needed.
                val downloadUrlResult = usuarioRepo.actualizarFotoPerfil(fotoUri) // Assume Result<String> or similar

                if (downloadUrlResult.isSuccess) {
                    val downloadUrl = downloadUrlResult.getOrThrow() // Get the URL

                    // 2. Update Firebase Auth profile photo URL
                    val profileUpdates = userProfileChangeRequest {
                        photoUri = Uri.parse(downloadUrl as String?) // Use the DOWNLOAD URL here!
                    }
                    auth.currentUser?.updateProfile(profileUpdates)?.await()

                    // 3. Update state immediately with the new URL for faster UI feedback
                    _state.update {
                        it.copy(
                            photoUrl = downloadUrl.toString(),
                            isLoading = false,
                            successMessage = "Foto de perfil actualizada."
                        )
                    }
                    // You might still call cargarDatosUsuario() if other profile info could change,
                    // but updating the photoUrl directly in the state is crucial for responsiveness.
                    // cargarDatosUsuario()
                } else {
                    throw downloadUrlResult.exceptionOrNull() ?: Exception("Error desconocido al subir la foto.")
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        photoUrl = auth.currentUser?.photoUrl.toString(),
                        isLoading = false,
                        successMessage = "Foto de perfil actualizada."
                    )
                }
                Log.e("Error de foto", {e.localizedMessage}.toString())
            }
        }
    }


    fun eliminarCuenta() {
        _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
        viewModelScope.launch {
            try {
                // Consider adding re-authentication here for security before deletion
                usuarioRepo.eliminarPerfil() // Assume this handles deletion from Firestore/Database
                auth.currentUser?.delete()?.await() // Delete from Firebase Auth
                // State update might not be necessary if navigating away immediately
                _state.update { it.copy(isLoading = false, successMessage = "Cuenta eliminada.") }
                // Navigation should be handled by the caller (Screen) based on success/state change
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Error al eliminar cuenta: ${e.localizedMessage}") }
            }
        }
    }

    // Function to clear error/success messages after they've been shown
    fun clearMessages() {
        _state.update { it.copy(error = null, successMessage = null) }
    }
}