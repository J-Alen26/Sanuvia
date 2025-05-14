package com.uv.sanuvia.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class Comentario(
    @DocumentId
    val idComentario: String = "",

    val publicacionId: String = "",
    val authorId: String = "",
    val text: String = "",

    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val username: String? = null,
    val userProfileImageUrl: String? = null
)