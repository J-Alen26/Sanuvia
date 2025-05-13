package com.uv.sanuvia.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class Publicacion(
    @DocumentId
    val idPublicacion: String = "",

    val authorId: String = "",
    val content: String = "",
    val likes: Int = 0,

    @ServerTimestamp
    val timestamp: Timestamp? = null
)