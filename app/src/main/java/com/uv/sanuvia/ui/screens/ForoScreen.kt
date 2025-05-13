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

@Composable
fun ForoScreen(
    foroViewModel: ForoScreenViewModel = viewModel(),
    comentariosViewModel: ComentariosViewModel = viewModel()
) {
    val state by foroViewModel.state.collectAsState()
    var nuevaPublicacion by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Área de nueva publicación
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ProfileAvatar()
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = nuevaPublicacion,
                        onValueChange = { nuevaPublicacion = it },
                        placeholder = { Text("escribe aqui", color = Color.Gray) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        ),
                        maxLines = 1
                    )
                }
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
                        enabled = !state.isCreatingPublicacion,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A3E47)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Publicar")
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
            Button(onClick = { foroViewModel.limpiarErrorPublicacion() }) {
                Text("Cerrar")
            }
        }

        // Diálogo para confirmar eliminación de publicación
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

        // Diálogo para editar publicación
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

        // Lista de publicaciones con la sección de comentarios integrada
        if (state.isPublicacionesLoading && state.publicaciones.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.publicaciones.isEmpty() && !state.isPublicacionesLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay publicaciones aún. ¡Sé el primero en compartir!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(state.publicaciones) { publicacion ->
                    PublicacionItemConComentarios( // Ahora está llamando al componente del otro archivo
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
}