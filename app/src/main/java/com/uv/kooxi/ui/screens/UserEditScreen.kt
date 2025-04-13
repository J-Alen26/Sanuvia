package com.uv.kooxi.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment

@Composable
fun UserEditScreen(
    onEdit: () -> Unit = {},
    onDeleteAccount: () -> Unit = {}
) {
    // Obtiene el estado desde el ViewModel de edición
    val viewModel: UserEditScreenViewModel = viewModel()
    val userState by viewModel.state.collectAsState()

    // Organiza el layout en una columna que ocupa toda la pantalla
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Zona superior: Imagen de perfil y nombre de usuario
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Carga la foto de perfil con Coil
            val painter = rememberAsyncImagePainter(userState.photoUrl)
            Image(
                painter = painter,
                contentDescription = "Foto de perfil",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = userState.username,
                style = MaterialTheme.typography.headlineSmall
            )
        }

        // Zona inferior: Botones para editar y eliminar cuenta
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { onEdit() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Editar")
            }
            Button(
                onClick = { onDeleteAccount() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Eliminar cuenta")
            }
        }
    }
}