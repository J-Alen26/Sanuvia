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
    private val LIKES_SUBCOLLECTION = "likes_usuarios"

    // Crear una nueva publicación (sin cambios relevantes para los likes)
    suspend fun crearPublicacion(publicacion: Publicacion): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("PublicacionRepository", "Guardando publicación en Firestore...")
            val publicacionMap = hashMapOf(
                "authorId" to publicacion.authorId,
                "content" to publicacion.content,
                "likes" to publicacion.likes,
                "timestamp" to FieldValue.serverTimestamp(),
                "username" to publicacion.username, // Asegúrate de guardar estos campos también
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

    // ... (funciones actualizarPublicacion y eliminarPublicacion sin cambios relevantes para los likes) ...

    // Dar like a una publicación (limitado a un like por usuario)
    suspend fun darLikePublicacion(idPublicacion: String): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            return@withContext Result.failure(Exception("Usuario no autenticado."))
        }

        try {
            Log.d("PublicacionRepository", "Intentando dar like a publicación con ID: $idPublicacion por usuario: $userId")

            val likeDocumentRef = db.collection(PUBLICACIONES_COLLECTION)
                .document(idPublicacion)
                .collection(LIKES_SUBCOLLECTION)
                .document(userId)
                .get()
                .await()

            if (likeDocumentRef.exists()) {
                Log.d("PublicacionRepository", "El usuario $userId ya dio like a la publicación $idPublicacion.")
                return@withContext Result.failure(Exception("Ya has dado like a esta publicación."))
            }

            // Si el usuario no ha dado like, agregamos su ID a la subcolección de likes
            db.collection(PUBLICACIONES_COLLECTION)
                .document(idPublicacion)
                .collection(LIKES_SUBCOLLECTION)
                .document(userId)
                .set(hashMapOf("timestamp" to FieldValue.serverTimestamp())) // Puedes guardar más info si quieres
                .await()

            // Incrementamos el contador de likes en el documento principal de la publicación
            db.collection(PUBLICACIONES_COLLECTION).document(idPublicacion)
                .update("likes", FieldValue.increment(1))
                .await()

            Log.d("PublicacionRepository", "Like aplicado a publicación con ID: $idPublicacion por usuario: $userId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("PublicacionRepository", "Error al dar like: ${e.message}", e)
            Result.failure(Exception("Error al dar like a la publicación: ${e.localizedMessage}", e))
        }
    }

    // Quitar like a una publicación
    suspend fun quitarLikePublicacion(idPublicacion: String): Result<Unit> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            return@withContext Result.failure(Exception("Usuario no autenticado."))
        }

        try {
            Log.d("PublicacionRepository", "Intentando quitar like a publicación con ID: $idPublicacion por usuario: $userId")

            // Eliminamos el documento del usuario de la subcolección de likes
            val likeDocumentRef = db.collection(PUBLICACIONES_COLLECTION)
                .document(idPublicacion)
                .collection(LIKES_SUBCOLLECTION)
                .document(userId)
                .delete()
                .await()

            // Decrementamos el contador de likes en el documento principal de la publicación
            db.collection(PUBLICACIONES_COLLECTION).document(idPublicacion)
                .update("likes", FieldValue.increment(-1))
                .await()

            Log.d("PublicacionRepository", "Like quitado de publicación con ID: $idPublicacion por usuario: $userId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("PublicacionRepository", "Error al quitar like: ${e.message}", e)
            Result.failure(Exception("Error al quitar like de la publicación: ${e.localizedMessage}", e))
        }
    }

    // Función para verificar si el usuario actual ya dio like a una publicación
    suspend fun haDadoLike(idPublicacion: String): Boolean = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            return@withContext false // Usuario no autenticado no puede haber dado like
        }

        return@withContext try {
            val likeDocumentRef = db.collection(PUBLICACIONES_COLLECTION)
                .document(idPublicacion)
                .collection(LIKES_SUBCOLLECTION)
                .document(userId)
                .get()
                .await()
            likeDocumentRef.exists()
        } catch (e: Exception) {
            Log.e("PublicacionRepository", "Error al verificar si el usuario dio like: ${e.message}", e)
            false // En caso de error, asumimos que no ha dado like para permitir reintentar
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

}