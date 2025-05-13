package com.uv.sanuvia.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PublicacionRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val PUBLICACIONES_COLLECTION = "publicaciones"

    // Crear una nueva publicación (sin cambios relevantes para los likes)
    suspend fun crearPublicacion(publicacion: Publicacion): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("PublicacionRepository", "Guardando publicación en Firestore...")
            val publicacionMap = hashMapOf(
                "authorId" to publicacion.authorId,
                "content" to publicacion.content,
                "likes" to publicacion.likes,
                "timestamp" to FieldValue.serverTimestamp(),
                "username" to publicacion.username,
                "userProfileImageUrl" to publicacion.userProfileImageUrl
            )
            val documentRef = db.collection(PUBLICACIONES_COLLECTION)
                .add(publicacionMap)
                .await()
            Log.d("PublicacionRepository", "Publicación guardada con ID: ${documentRef.id}")
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Log.e("PublicacionRepository", "Error Firestore: ${e.message}", e)
            Result.failure(Exception("Error al guardar la publicación: ${e.localizedMessage}", e))
        }
    }

    // Obtener todas las publicaciones (sin cambios relevantes para los likes)
    suspend fun obtenerPublicaciones(): Result<List<Publicacion>> = withContext(Dispatchers.IO) {
        try {
            Log.d("PublicacionRepository", "Obteniendo publicaciones...")
            val querySnapshot = db.collection(PUBLICACIONES_COLLECTION)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val publicaciones = querySnapshot.documents.mapNotNull { document ->
                val data = document.data
                if (data != null) {
                    Publicacion(
                        idPublicacion = document.id,
                        authorId = data["authorId"] as? String ?: "",
                        content = data["content"] as? String ?: "",
                        likes = (data["likes"] as? Number)?.toInt() ?: 0,
                        timestamp = data["timestamp"] as? com.google.firebase.Timestamp,
                        username = data["username"] as? String,
                        userProfileImageUrl = data["userProfileImageUrl"] as? String
                    )
                } else null
            }
            Log.d("PublicacionRepository", "Se obtuvieron ${publicaciones.size} publicaciones.")
            Result.success(publicaciones)
        } catch (e: Exception) {
            Log.e("PublicacionRepository", "Error al obtener publicaciones: ${e.message}", e)
            Result.failure(Exception("Error al obtener las publicaciones: ${e.localizedMessage}", e))
        }
    }

    // Dar like a una publicación
    suspend fun darLikePublicacion(idPublicacion: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("PublicacionRepository", "Dando like a publicación con ID: $idPublicacion...")

            // Utilizamos FieldValue.increment para aumentar de manera atómica el contador de likes
            val incrementLikes = hashMapOf<String, Any>(
                "likes" to com.google.firebase.firestore.FieldValue.increment(1)
            )

            db.collection("publicaciones").document(idPublicacion)
                .update(incrementLikes)
                .await()

            Log.d("PublicacionRepository", "Like aplicado a publicación con ID: $idPublicacion")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PublicacionRepository", "Error al dar like: ${e.message}", e)
            Result.failure(Exception("Error al dar like a la publicación: ${e.localizedMessage}", e))
        }
    }

    // Ya no necesitamos la función haDadoLike

    // Actualizar una publicación (sin cambios relevantes para los likes)
    suspend fun actualizarPublicacion(
        idPublicacion: String,
        publicacion: Publicacion,
        usuarioActualId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docSnapshot = db.collection("publicaciones")
                .document(idPublicacion)
                .get()
                .await()

            val authorId = docSnapshot.getString("authorId")

            if (authorId != usuarioActualId) {
                return@withContext Result.failure(Exception("No tienes permiso para editar esta publicación"))
            }

            Log.d("PublicacionRepository", "Actualizando publicación con ID: $idPublicacion...")
            val updateData = hashMapOf<String, Any>(
                "content" to publicacion.content
            )
            db.collection("publicaciones").document(idPublicacion)
                .update(updateData)
                .await()
            Log.d("PublicacionRepository", "Publicación actualizada con ID: $idPublicacion")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PublicacionRepository", "Error al actualizar publicación: ${e.message}", e)
            Result.failure(Exception("Error al actualizar la publicación: ${e.localizedMessage}", e))
        }
    }

    // Eliminar una publicación (sin cambios relevantes para los likes)
    suspend fun eliminarPublicacion(
        idPublicacion: String,
        usuarioActualId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val docSnapshot = db.collection("publicaciones")
                .document(idPublicacion)
                .get()
                .await()

            val authorId = docSnapshot.getString("authorId")

            if (authorId != usuarioActualId) {
                return@withContext Result.failure(Exception("No tienes permiso para eliminar esta publicación"))
            }

            Log.d("PublicacionRepository", "Eliminando publicación con ID: $idPublicacion...")
            db.collection("publicaciones").document(idPublicacion)
                .delete()
                .await()
            Log.d("PublicacionRepository", "Publicación eliminada con ID: $idPublicacion")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PublicacionRepository", "Error al eliminar publicación: ${e.message}", e)
            Result.failure(Exception("Error al eliminar la publicación: ${e.localizedMessage}", e))
        }
    }
}