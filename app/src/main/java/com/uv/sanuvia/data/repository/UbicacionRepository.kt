package com.uv.sanuvia.data.repository


import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.Result


class UbicacionRepository {


    // Inicializa Firebase Functions (ajusta la región si es necesario)
    private val functions: FirebaseFunctions = Firebase.functions("us-central1")

    companion object {
        private const val TAG = "UbicacionRepository"
        private const val CF_OBTENER_CULTIVOS = "obtenerCultivosPorUbicacion" // Nombre exacto de tu CF
    }

    /**
     * Llama a la Cloud Function para obtener la lista de cultivos comunes y sus descripciones
     * para una ubicación dada. Utiliza la caché de Firestore implementada en la Cloud Function.
     *
     * @param ubicacion String con el nombre de la ubicación (ej: "Ciudad, Estado, País").
     * @return Result<List<CultivoInfo>> Lista de cultivos con descripción si tiene éxito,
     * o una excepción encapsulada si falla.
     */
    suspend fun obtenerCultivos(ubicacion: String): Result<List<CultivoInfo>> {
        // Prepara los datos a enviar a la Cloud Function
        // La clave "ubicacion" debe coincidir con la que espera tu función (data.ubicacion)
        val data = hashMapOf(
            "ubicacion" to ubicacion
        )
        // Log para verificar justo antes de enviar (puedes añadirlo si no lo tenías)
        Log.d(TAG, "Repository: Enviando datos a CF: $data")


        Log.d(TAG, "Llamando a Cloud Function '$CF_OBTENER_CULTIVOS' para ubicación: $ubicacion")

        // Ejecuta la llamada en un contexto IO (aunque .await() libera el hilo)
        return withContext(Dispatchers.IO) {
            try {
                val result = functions
                    .getHttpsCallable(CF_OBTENER_CULTIVOS)
                    .call(data)
                    .await() // Espera el resultado de la función

                Log.d(TAG, "Respuesta cruda de CF: ${result.data}")

                // --- Procesar la Respuesta ---
                // La función debería devolver un Map, y dentro, una clave "cultivos" con una Lista de Mapas.
                val resultMap = result.data as? Map<String, Any>
                val cultivosData = resultMap?.get("cultivos") as? List<*> // Obtiene la lista (puede ser de tipo Any)

                if (cultivosData == null) {
                    Log.e(TAG, "La respuesta de la CF no contenía la clave 'cultivos' o no era una lista.")
                    // Puede que la función devolviera un array vacío [], lo cual es válido si no hay cultivos
                    if (resultMap != null && !resultMap.containsKey("cultivos")) {
                        return@withContext Result.failure(Exception("Respuesta inválida de la función (falta 'cultivos')."))
                    }
                    // Si la clave existe pero es null o no es lista, o si resultMap es null, consideramos éxito con lista vacía
                    // O podrías devolver fallo si prefieres
                    Log.w(TAG, "Campo 'cultivos' no encontrado o inválido en la respuesta, devolviendo lista vacía.")
                    return@withContext Result.success(emptyList<CultivoInfo>())

                }

                // Mapea la lista de mapas genéricos a una lista de CultivoInfo
                val listaCultivosInfo = cultivosData.mapNotNull { item ->
                    if (item is Map<*, *>) {
                        // Intenta extraer nombre y descripción de cada mapa
                        val nombre = item["nombre"] as? String
                        val descripcion = item["descripcion"] as? String
                        val urlImagen = item["urlImagen"] as? String
                        if (nombre != null && descripcion != null) {
                            CultivoInfo(nombre = nombre, descripcion = descripcion, urlImagen = urlImagen)
                        } else {
                            Log.w(TAG, "Item en lista 'cultivos' no tiene 'nombre' o 'descripcion' como String: $item")
                            null // Ignora items malformados
                        }
                    } else {
                        Log.w(TAG, "Item en lista 'cultivos' no es un Mapa: $item")
                        null // Ignora items que no son mapas
                    }
                }

                Log.d(TAG, "Se parsearon ${listaCultivosInfo.size} cultivos.")
                Result.success(listaCultivosInfo)

            } catch (e: Exception) {
                // Captura errores de la llamada (red, permisos, error interno de la función)
                Log.e(TAG, "Error al llamar a Cloud Function '$CF_OBTENER_CULTIVOS': ${e.message}", e)
                // Devuelve un Result de fallo con la excepción
                Result.failure(Exception("No se pudieron obtener los cultivos: ${e.localizedMessage}", e))
            }
        } // Fin withContext
    } // Fin obtenerCultivos
}