package com.uv.sanuvia.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.time.LocalDateTime


class EscaneoAlimento(private val usuario: Usuario) {

    var idEscaneo: Int = 0
        private set

    var foto: String? = null
        private set

    var fechaHora: LocalDateTime? = null
        private set

    var resultado: String? = null
        private set

    private val ia = InteligenciaArtificial()

    /**
     * Inicia el proceso de escaneo: almacena la imagen y fecha,
     * y solicita información a la IA en un hilo de fondo.
     */
    fun scanAlimento(base64Image: String): LiveData<String?> {
        val liveData = MutableLiveData<String?>()
        foto = base64Image
        fechaHora = LocalDateTime.now()

        Thread {
            try {
                val res = ia.generarInformacion(base64Image)
                resultado = res
                liveData.postValue(res)
            } catch (e: Exception) {
                liveData.postValue(null)
            }
        }.start()

        return liveData
    }

    /**
     * Devuelve el resultado del último escaneo.
     */
    fun mostrarResultado(): String? = resultado
}