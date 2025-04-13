package com.uv.kooxi.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import androidx.core.net.toUri

class Usuario {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    companion object {
        // Imagen por defecto. Coloca 'profile.png' en: app/src/main/res/drawable/profile.png
        const val DEFAULT_PROFILE_IMAGE_URI = "android.resource://com.uv.kooxi/drawable/profile.png"
    }

    // Método de registro actualizado para incluir el nombre de usuario (username)
    suspend fun registrarse(email: String, password: String, username: String): Result<FirebaseUser?> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                // Actualiza el perfil con el nombre de usuario y la imagen por defecto
                val request = UserProfileChangeRequest.Builder().apply {
                    setDisplayName(username)
                    setPhotoUri(DEFAULT_PROFILE_IMAGE_URI.toUri())
                }.build()
                user.updateProfile(request).await()
            }
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun iniciarSesion(email: String, password: String): Result<FirebaseUser?> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Result.success(authResult.user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun cerrarSesion() {
        firebaseAuth.signOut()
    }

    // Método para editar el perfil del usuario, requiere nuevo nombre de usuario
    suspend fun editarUsuario(username: String, photoUrl: String? = null): Result<FirebaseUser?> {
        val user = firebaseAuth.currentUser ?: return Result.failure(Exception("No hay usuario autenticado"))
        val finalPhotoUrl = if (photoUrl.isNullOrEmpty()) DEFAULT_PROFILE_IMAGE_URI else photoUrl
        val request = UserProfileChangeRequest.Builder().apply {
            setDisplayName(username)
            setPhotoUri(finalPhotoUrl.toUri())
        }.build()
        return try {
            user.updateProfile(request).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Método para actualizar la contraseña del usuario autenticado
    suspend fun actualizarContrasena(nuevaContrasena: String): Result<Unit> {
        val user = firebaseAuth.currentUser ?: return Result.failure(Exception("No hay usuario autenticado"))
        return try {
            user.updatePassword(nuevaContrasena).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Método para eliminar el perfil del usuario
    suspend fun eliminarPerfil(): Result<Unit> {
        val user = firebaseAuth.currentUser ?: return Result.failure(Exception("No hay usuario autenticado"))
        return try {
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Método para subir una foto de perfil a Firebase Storage y obtener la URL de descarga
    suspend fun subirFotoPerfil(localImageUri: Uri): Result<String> {
        val uid = firebaseAuth.currentUser?.uid ?: return Result.failure(Exception("Usuario no autenticado"))
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/$uid/profile.png")
        return try {
            storageRef.putFile(localImageUri).await()
            val downloadUrl = storageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}