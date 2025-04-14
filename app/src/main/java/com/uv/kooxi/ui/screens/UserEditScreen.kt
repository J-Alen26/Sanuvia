package com.uv.kooxi.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment

@Composable
fun UserEditScreen(
    onNavigateToLogin: () -> Unit

    ) {
    // Obtiene el estado desde el ViewModel de edición
    val viewModel: UserEditScreenViewModel = viewModel()
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.subirFotoPerfil(it) }
    }
    val userState by viewModel.state.collectAsState()

    val updatedUsername = remember { mutableStateOf(userState.username) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userState.username) {
        updatedUsername.value = userState.username
    }

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
            TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                Text(text = "Cambiar foto de perfil")
            }
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
            // TextField to update username
            OutlinedTextField(
                value = updatedUsername.value,
                onValueChange = { updatedUsername.value = it },
                label = { Text("Nombre de usuario") },
                modifier = Modifier.fillMaxWidth()
            )
            // Button to update user information using actualizarUsuario
            Button(
                onClick = { viewModel.actualizarUsuario(updatedUsername.value) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar")
            }

            // If in loading state, show a CircularProgressIndicator in place of the delete button
            if (userState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                // Button to trigger delete account confirmation dialog
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar cuenta")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Advertencia") },
            text = { Text("¿Seguro que deseas eliminar tu cuenta? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarCuenta()
                        showDeleteDialog = false
                        onNavigateToLogin()
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}