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
import android.location.Address // Importa Address
import android.location.Geocoder // Importa Geocoder
import android.os.Build // Necesario para la versión del Geocoder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.Result
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
    suspend fun obtenerUbicacionUsuario(context: Context): Result<String> = withContext(Dispatchers.IO) {
        // Obtiene el FusedLocationProviderClient
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        try {
            // 1. Obtiene la última ubicación disponible (igual que antes)
            val location: Location? = fusedLocationClient.lastLocation.await()

            if (location != null) {
                // 2. Si tenemos ubicación, intentamos la Geocodificación Inversa
                if (Geocoder.isPresent()) { // Verifica si Geocoder está disponible en el dispositivo
                    val geocoder = Geocoder(context, Locale.getDefault())
                    try {
                        val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            // Usa la API asíncrona para Android 13 (Tiramisu) y superior (más complejo de implementar aquí, mostramos la síncrona por simplicidad)
                            // TODO: Considerar implementar la versión con listener para API 33+ para evitar bloqueos si no se llama desde IO dispatcher
                            geocoder.getFromLocation(location.latitude, location.longitude, 1) // Pide 1 resultado
                        } else {
                            // Usa la API síncrona (deprecated en API 33+) - ¡Asegúrate de llamar desde Dispatchers.IO!
                            @Suppress("DEPRECATION")
                            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        }

                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0] // Toma la primera dirección (la más probable)

                            // 3. Construye el String con la dirección formateada
                            // Puedes ajustar qué campos incluir y el formato
                            val addressString = listOfNotNull(
                                address.locality,       // Ciudad
                                address.adminArea,      // Estado/Provincia
                                address.countryName     // País
                            ).joinToString(separator = ", ") // Une con ", "

                            if (addressString.isNotBlank()) {
                                Log.d("UsuarioRepo", "Dirección encontrada: $addressString")
                                Result.success(addressString)
                            } else {
                                Log.w("UsuarioRepo", "Geocoder devolvió una dirección vacía.")
                                // Devolvemos las coordenadas como fallback si no hay texto de dirección
                                Result.success("Lat: ${location.latitude}, Lon: ${location.longitude}")
                                // O puedes devolver un fallo específico: Result.failure(Exception("No se encontró nombre de ubicación"))
                            }
                        } else {
                            Log.w("UsuarioRepo", "Geocoder no devolvió direcciones para las coordenadas.")
                            // Devolvemos las coordenadas como fallback
                            Result.success("Lat: ${location.latitude}, Lon: ${location.longitude}")
                            // O puedes devolver un fallo específico: Result.failure(Exception("No se encontraron direcciones"))
                        }
                    } catch (e: Exception) { // Error durante la geocodificación
                        Log.e("UsuarioRepo", "Error de Geocoder: ${e.message}", e)
                        Result.failure(Exception("Error al obtener nombre de ubicación: ${e.localizedMessage}", e))
                    }
                } else {
                    Log.w("UsuarioRepo", "Geocoder no está presente en este dispositivo.")
                    // Devolvemos las coordenadas como fallback si no hay Geocoder
                    Result.success("Lat: ${location.latitude}, Lon: ${location.longitude}")
                    // O puedes devolver un fallo específico: Result.failure(Exception("Servicio de Geocoder no disponible"))
                }
            } else {
                // No se pudo obtener la ubicación inicial
                Log.w("UsuarioRepo", "No se pudo obtener la ubicación (lastLocation fue null)")
                Result.failure(Exception("No se pudo obtener la ubicación inicial"))
            }
        } catch (e: SecurityException) { // Error específico de permisos
            Log.e("UsuarioRepo", "Error de permisos de ubicación: ${e.message}", e)
            Result.failure(Exception("Permiso de ubicación denegado.", e))
        } catch (e: Exception) { // Otros errores (ej: FusedLocationProvider)
            Log.e("UsuarioRepo", "Error general al obtener ubicación: ${e.message}", e)
            Result.failure(Exception("Error obteniendo ubicación: ${e.localizedMessage}", e))
        }
    }
}