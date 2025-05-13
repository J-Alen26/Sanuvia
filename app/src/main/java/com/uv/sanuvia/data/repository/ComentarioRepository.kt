package com.uv.sanuvia.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ComentarioRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val COMENTARIOS_COLLECTION = "comentarios"

    // Crear un nuevo comentario
    suspend fun crearComentario(comentario: Comentario): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("ComentarioRepository", "Guardando comentario en Firestore...")
            val comentarioMap = hashMapOf(
                "publicacionId" to comentario.publicacionId,
                "authorId" to comentario.authorId,
                "text" to comentario.text,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            val documentRef = db.collection(COMENTARIOS_COLLECTION)
                .add(comentarioMap)
                .await()

            Log.d("ComentarioRepository", "Comentario guardado con ID: ${documentRef.id}")
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Log.e("ComentarioRepository", "Error Firestore: ${e.message}", e)
            Result.failure(Exception("Error al guardar el comentario: ${e.localizedMessage}", e))
        }
    }

    // Obtener todos los comentarios de una publicación específica
    suspend fun obtenerComentariosPorPublicacion(publicacionId: String): Result<List<Comentario>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d("ComentarioRepository", "Obteniendo comentarios para publicación: $publicacionId")
                val querySnapshot = db.collection(COMENTARIOS_COLLECTION)
                    .whereEqualTo("publicacionId", publicacionId)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                    .get()
                    .await()

                val comentarios = querySnapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject(Comentario::class.java)
                    } catch (e: Exception) {
                        Log.e("ComentarioRepository", "Error al convertir a Comentario: ${e.message}", e)
                        null
                    }
                }

                Log.d("ComentarioRepository", "Se obtuvieron ${comentarios.size} comentarios.")
                Result.success(comentarios)
            } catch (e: Exception) {
                Log.e("ComentarioRepository", "Error al obtener comentarios: ${e.message}", e)
                Result.failure(Exception("Error al obtener los comentarios: ${e.localizedMessage}", e))
            }
        }

    // Eliminar un comentario
    suspend fun eliminarComentario(
        idComentario: String,
        usuarioActualId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Primero verificamos que el comentario pertenezca al usuario actual
            val docSnapshot = db.collection(COMENTARIOS_COLLECTION)
                .document(idComentario)
                .get()
                .await()

            val authorId = docSnapshot.getString("authorId")

            if (authorId != usuarioActualId) {
                return@withContext Result.failure(Exception("No tienes permiso para eliminar este comentario"))
            }

            Log.d("ComentarioRepository", "Eliminando comentario con ID: $idComentario...")
            db.collection(COMENTARIOS_COLLECTION).document(idComentario)
                .delete()
                .await()

            Log.d("ComentarioRepository", "Comentario eliminado con ID: $idComentario")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ComentarioRepository", "Error al eliminar comentario: ${e.message}", e)
            Result.failure(Exception("Error al eliminar el comentario: ${e.localizedMessage}", e))
        }
    }
}