package com.uv.sanuvia.data.repository // O tu paquete de repositorios

import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.Result
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ServerTimestamp

class EscaneoRepository {
    private val storageRef: StorageReference = Firebase.storage.reference
    private val db: FirebaseFirestore = Firebase.firestore
    private val auth = Firebase.auth
    private suspend fun subirFotoYObtenerUrl(fotoUri: Uri, rutaStorage: String = "escaneos_alimentos"): Result<String> = withContext(Dispatchers.IO) {
        val nombreArchivo = "${UUID.randomUUID()}.jpg"
        val archivoRef = storageRef.child("$rutaStorage/$nombreArchivo")
        try {
            Log.d("EscaneoRepository", "Subiendo a: ${archivoRef.path}")
            archivoRef.putFile(fotoUri).await()
            val downloadUrl = archivoRef.downloadUrl.await().toString()
            Log.d("EscaneoRepository", "URL: $downloadUrl")
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e("EscaneoRepository", "Error Storage: ${e.message}", e)
            Result.failure(Exception("Error en Firebase Storage: ${e.localizedMessage}", e))
        }
    }

    // --- Función para llamar a la CLASE InteligenciaArtificial (MODIFICADO) ---
    /**
     * Llama a la clase local InteligenciaArtificial para analizar la imagen.
     * ¡¡ADVERTENCIA!! Esta implementación es insegura y temporal.
     * La API Key de OpenAI está expuesta en el cliente.
     * Migrar a Cloud Functions es CRUCIAL.
     *
     * @param imageUrl La URL pública de la imagen en Firebase Storage.
     * @return Result<String> Contiene la descripción si es exitoso, o una excepción si falla.
     */
    private suspend fun obtenerDescripcionDeImagenIA_Local(imageUrl: String): Result<String> {
        Log.w("EscaneoRepository", "ADVERTENCIA: Usando llamada local INSEGURA a IA.")
        // Instanciamos aquí temporalmente. ¡NO ES LO IDEAL!
        val iaService = InteligenciaArtificial()
        return try {
            Log.d("EscaneoRepository", "Llamando a IA local...")
            // Llama al método de tu clase local
            val description = iaService.describirImagen(imageUrl)
            Log.d("EscaneoRepository", "Descripción IA local recibida.")
            Result.success(description)
        } catch (e: Exception) {
            // Captura errores de la llamada a OpenAI (red, API key inválida, error de formato, etc.)
            Log.e("EscaneoRepository", "Error IA Local: ${e.message}", e)
            Result.failure(Exception("Error al analizar la imagen con IA local: ${e.localizedMessage}", e))
        }
    }


    // --- Función para guardar el resultado en Firestore (sin cambios conceptuales) ---
    private suspend fun guardarAnalisisEnFirestore(escaneo: EscaneoAlimento): Result<String> = withContext(Dispatchers.IO) {
        try {
            // ¡NO CREES UN MAP MANUALMENTE!
            // Pasa el objeto 'escaneo' directamente a .add()
            // Firestore usará las anotaciones @ServerTimestamp y @DocumentId definidas
            // en la clase EscaneoAlimento.
            // El campo fechaHora es null en el objeto 'escaneo' local, pero Firestore
            // lo reemplazará con la hora del servidor gracias a @ServerTimestamp.

            Log.d("EscaneoRepository", "Guardando objeto EscaneoAlimento en Firestore...")
            val documentRef = db.collection("escaneos") // Nombre de tu colección
                //      vvvvvvv <-- Pasa el objeto Kotlin directamente
                .add(escaneo)
                .await()
            Log.d("EscaneoRepository", "Análisis guardado con ID: ${documentRef.id}")
            Result.success(documentRef.id) // Devuelve el ID autogenerado
        } catch (e: Exception) {
            Log.e("EscaneoRepository", "Error Firestore: ${e.message}", e)
            Result.failure(Exception("Error al guardar el análisis: ${e.localizedMessage}", e))
        }
    }

    // La función que llama a esta no necesita cambios significativos,
// solo asegúrate de que crea el objeto EscaneoAlimento correctamente:
    suspend fun procesarYGuardarEscaneo(fotoUri: Uri): Result<String> {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            return Result.failure(Exception("Usuario no autenticado."))
        }

        val urlFoto = subirFotoYObtenerUrl(fotoUri).getOrElse { return Result.failure(it) }
        val descripcion = obtenerDescripcionDeImagenIA_Local(urlFoto).getOrElse { return Result.failure(it) }

        // Crea el objeto completo. fechaHora será null aquí, pero está bien.
        val nuevoEscaneo = EscaneoAlimento(
            // idEscaneo se deja vacío (String por defecto) para que @DocumentId funcione al leer
            urlFoto = urlFoto,
            resultado = descripcion,
            idUsuario = userId,
            fechaHora = null // Explicitamente null o dejas que tome el valor por defecto (null)
        )

        // Pasa el objeto completo a la función de guardado
        return guardarAnalisisEnFirestore(nuevoEscaneo)
    }


    // --- Función para LEER los escaneos (Sin cambios) ---
    suspend fun obtenerMisEscaneos(): Result<List<EscaneoAlimento>> = withContext(Dispatchers.IO) {
        val userId = auth.currentUser?.uid ?: return@withContext Result.failure(Exception("Usuario no autenticado."))
        try {
            Log.d("EscaneoRepository", "Obteniendo escaneos para usuario: $userId")
            val querySnapshot = db.collection("escaneos")
                .whereEqualTo("idUsuario", userId)
                .orderBy("fechaHora", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            val escaneos = querySnapshot.documents.mapNotNull { document ->
                document.toObject(EscaneoAlimento::class.java)
            }
            Log.d("EscaneoRepository", "Se obtuvieron ${escaneos.size} escaneos.")
            Result.success(escaneos)
        } catch (e: Exception) {
            Log.e("EscaneoRepository", "Error al obtener escaneos: ${e.message}", e)
            Result.failure(Exception("Error al obtener los escaneos: ${e.localizedMessage}", e))
        }
    }
}