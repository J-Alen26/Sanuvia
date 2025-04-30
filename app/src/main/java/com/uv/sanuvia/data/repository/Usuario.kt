package com.uv.sanuvia.data.repository

import android.Manifest
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import androidx.core.net.toUri
import android.content.Context
import android.location.Location
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.LocationServices

class Usuario {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    companion object {
        const val DEFAULT_PROFILE_IMAGE_STORAGE_URI = "gs://anfeca2025.firebasestorage.app/profile_images/default/pexels-moh-adbelghaffar-771742.jpg"
    }

    private suspend fun getDefaultProfileImageUri(): Uri {
        val storageRef = FirebaseStorage.getInstance()
            .getReferenceFromUrl(DEFAULT_PROFILE_IMAGE_STORAGE_URI)
        val downloadUrl = storageRef.downloadUrl.await()
        return downloadUrl
    }

    suspend fun registrarse(email: String, password: String, username: String): Result<FirebaseUser?> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                val request = UserProfileChangeRequest.Builder().apply {
                    displayName = username
                    photoUri = getDefaultProfileImageUri()
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

    suspend fun editarUsuario(username: String): Result<FirebaseUser?> {
        val user = firebaseAuth.currentUser ?: return Result.failure(Exception("No hay usuario autenticado"))
        val request = UserProfileChangeRequest.Builder().apply {
            displayName = username
        }.build()
        return try {
            user.updateProfile(request).await()
            user.reload().await()
            Result.success(firebaseAuth.currentUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarFotoPerfil(localImageUri: Uri): Result<FirebaseUser?> {
        val user = firebaseAuth.currentUser ?: return Result.failure(Exception("No hay usuario autenticado"))
        return try {
            // Sube la nueva foto a Storage
            val uploadResult = subirFotoPerfil(localImageUri)
            if (uploadResult.isFailure) throw uploadResult.exceptionOrNull()!!
            val newPhotoUrl = uploadResult.getOrNull()!!
            // Construye el cambio de perfil solo con la nueva foto
            val request = UserProfileChangeRequest.Builder()
                .setPhotoUri(newPhotoUrl.toUri())
                .build()
            user.updateProfile(request).await()
            user.reload().await()
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

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    suspend fun obtenerUbicacionUsuario(context: Context): Result<Location> {
        // Obtiene el FusedLocationProviderClient
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        return try {
            // Obtiene la última ubicación disponible de forma asíncrona
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                Result.success(location)
            } else {
                Result.failure(Exception("No se pudo obtener la ubicación"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } as Result<Location>
    }
}