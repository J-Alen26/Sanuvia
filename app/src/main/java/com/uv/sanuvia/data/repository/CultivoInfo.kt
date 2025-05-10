package com.uv.sanuvia.data.repository // O donde pongas tus modelos

// Representa la información de un cultivo devuelta por la función/API
data class CultivoInfo(
    val nombre: String = "",
    val descripcion: String = "",
    val urlImagen: String? = "https://firebasestorage.googleapis.com/v0/b/anfeca2025.firebasestorage.app/o/imagenes_cultivos%2Fdefault%2Fnature_14666297.png?alt=media&token=2956ed45-5a7d-4d99-8ac8-a3447a1be51b"
)
