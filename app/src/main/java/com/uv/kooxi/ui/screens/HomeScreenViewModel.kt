package com.uv.kooxi.ui.screens

import androidx.lifecycle.ViewModel
import com.uv.kooxi.data.repository.Usuario

class HomeScreenViewModel : ViewModel() {

    private val usuarioRepo = Usuario()

    // Función para cerrar sesión utilizando la lógica de la clase Usuario
    fun logout() {
        usuarioRepo.cerrarSesion()
    }
}