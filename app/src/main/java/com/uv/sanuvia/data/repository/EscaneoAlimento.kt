package com.uv.sanuvia.data.repository // O donde tengas tus modelos

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId // Para obtener el ID automático de Firestore
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp // Para fechas automáticas del servidor

data class EscaneoAlimento(
    @DocumentId // Anotación para que Firestore ponga aquí el ID del documento
    val idEscaneo: String = "", // Usamos String para el ID de Firestore


    val urlFoto: String = "", // Almacenamos la URL de Storage, no la foto en sí

    @ServerTimestamp // Firestore pondrá la fecha del servidor automáticamente al crear
    val fechaHora: Timestamp? = null, // Nullable hasta que el servidor la establezca
    val resultado: String = "", // El texto de la IA

    val idUsuario: String = "" // El ID del usuario de Firebase Auth
)