package com.uv.sanuvia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uv.sanuvia.ui.screens.common.ProfileAvatar
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign

@Composable
fun ForoScreen(
    foroViewModel: ForoScreenViewModel = viewModel(),
    comentariosViewModel: ComentariosViewModel = viewModel()
) {
    val state by foroViewModel.state.collectAsState()
    var nuevaPublicacion by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Recetas",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp), // Actualizado para coincidir
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp) // Actualizado a 16.dp para coincidir
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ProfileAvatar(modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.width(0.dp))
                        OutlinedTextField(
                            value = nuevaPublicacion,
                            onValueChange = { nuevaPublicacion = it },
                            placeholder = {
                                Text(
                                    "Escribe aquí",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 40.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedContainerColor = Color.Transparent
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            maxLines = Int.MAX_VALUE,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (nuevaPublicacion.isNotBlank()) {
                                        foroViewModel.crearPublicacion(nuevaPublicacion)
                                        nuevaPublicacion = ""
                                    }
                                }
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (nuevaPublicacion.isNotBlank()) {
                                    foroViewModel.crearPublicacion(nuevaPublicacion)
                                    nuevaPublicacion = ""
                                }
                            },
                            enabled = nuevaPublicacion.isNotBlank() && !state.isCreatingPublicacion,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3A3E47),
                                disabledContainerColor = Color(0xFF3A3E47).copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(20.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Publicar", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            if (state.isCreatingPublicacion) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.publicacionError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { foroViewModel.limpiarErrorPublicacion() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("Cerrar")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        // 2. Lista de publicaciones
        when {
            state.isPublicacionesLoading && state.publicaciones.isEmpty() -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            state.publicaciones.isEmpty() && !state.isPublicacionesLoading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay publicaciones aún. ¡Sé el primero en compartir!")
                    }
                }
            }
            else -> {
                items(state.publicaciones) { publicacion ->
                    PublicacionItemConComentarios(
                        publicacion = publicacion,
                        onLikeClick = { foroViewModel.toggleLikePublicacion(publicacion.idPublicacion) },
                        onDeleteClick = { foroViewModel.mostrarDialogoEliminar(publicacion.idPublicacion) },
                        onEditClick = { foroViewModel.mostrarDialogoEditar(publicacion) },
                        esPropietario = foroViewModel.obtenerUsuarioActualId() == publicacion.authorId,
                        comentariosViewModel = comentariosViewModel
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    // 3. Diálogos (fuera del LazyColumn)
    if (state.mostrandoDialogoEliminar && state.idPublicacionAEliminar != null) {
        AlertDialog(
            onDismissRequest = { foroViewModel.cancelarEliminarPublicacion() },
            title = { Text("Eliminar publicación") },
            text = { Text("¿Estás seguro de que quieres eliminar esta publicación?") },
            confirmButton = {
                Button(
                    onClick = { foroViewModel.confirmarEliminarPublicacion() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { foroViewModel.cancelarEliminarPublicacion() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (state.mostrandoDialogoEditar && state.publicacionAEditar != null) {
        AlertDialog(
            onDismissRequest = { foroViewModel.cancelarEdicionPublicacion() },
            title = { Text("Editar publicación") },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.textoEdicion,
                        onValueChange = foroViewModel::actualizarTextoEdicion,
                        label = { Text("Editar contenido") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = { foroViewModel.guardarEdicionPublicacion() }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { foroViewModel.cancelarEdicionPublicacion() }) {
                    Text("Cancelar")
                }
            }
        )
    }
}