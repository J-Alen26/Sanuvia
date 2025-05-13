package com.uv.sanuvia.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class PublicacionRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Crear una nueva publicación
    suspend fun crearPublicacion(publicacion: Publicacion): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("PublicacionRepository", "Guardando publicación en Firestore...")
            val publicacionMap = hashMapOf(
                "authorId" to publicacion.authorId,
                "content" to publicacion.content,
                "likes" to publicacion.likes,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            val documentRef = db.collection("publicaciones")
                .add(publicacionMap)
                .await()
            Log.d("PublicacionRepository", "Publicación guardada con ID: ${documentRef.id}")
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Log.e("PublicacionRepository", "Error Firestore: ${e.message}", e)
            Result.failure(Exception("Error al guardar la publicación: ${e.localizedMessage}", e))
        }
    }

    // Obtener todas las publicaciones
    suspend fun obtenerPublicaciones(): Result<List<Publicacion>> = withContext(Dispatchers.IO) {
        try {
            Log.d("PublicacionRepository", "Obteniendo publicaciones...")
            val querySnapshot = db.collection("publicaciones")
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
                        timestamp = data["timestamp"] as? com.google.firebase.Timestamp
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

    // Actualizar una publicación
    suspend fun actualizarPublicacion(
        idPublicacion: String,
        publicacion: Publicacion,
        usuarioActualId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Primero verificamos que la publicación pertenezca al usuario actual
            val docSnapshot = db.collection("publicaciones")
                .document(idPublicacion)
                .get()
                .await()

            val authorId = docSnapshot.getString("authorId")

            if (authorId != usuarioActualId) {
                return@withContext Result.failure(Exception("No tienes permiso para editar esta publicación"))
            }

            Log.d("PublicacionRepository", "Actualizando publicación con ID: $idPublicacion...")

            // Actualizamos solo el contenido de la publicación, manteniendo el resto de datos
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

    // Eliminar una publicación
    suspend fun eliminarPublicacion(
        idPublicacion: String,
        usuarioActualId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Primero verificamos que la publicación pertenezca al usuario actual
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
}