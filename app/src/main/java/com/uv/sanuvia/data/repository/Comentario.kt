package com.uv.sanuvia.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Comentario(
    @DocumentId
    val idComentario: String = "",

    val publicacionId: String = "",  // ID de la publicación a la que pertenece este comentario
    val authorId: String = "",       // ID del usuario que escribió el comentario
    val text: String = "",           // Contenido del comentario

    @ServerTimestamp
    val timestamp: Timestamp? = null
)