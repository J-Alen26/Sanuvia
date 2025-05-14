package com.uv.sanuvia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uv.sanuvia.data.repository.Comentario
import com.uv.sanuvia.ui.screens.common.ProfileAvatar
import java.text.SimpleDateFormat
import java.util.*
import com.uv.sanuvia.ui.screens.common.ProfileAvatarConUrl

@Composable
fun ComentariosSeccion(
    publicacionId: String,
    comentariosViewModel: ComentariosViewModel = viewModel()
) {
    val state by comentariosViewModel.state.collectAsState()
    var nuevoComentario by remember { mutableStateOf("") }

    LaunchedEffect(publicacionId) {
        comentariosViewModel.cargarComentarios(publicacionId)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Título de la sección
        Text(
            text = "Comentarios",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Campo para añadir nuevo comentario
        // Campo para añadir nuevo comentario modificado para ser expandible
        Row(
            verticalAlignment = Alignment.Top, // cambiado a Top para que se alinee bien cuando crece
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            ProfileAvatar(size = 36.dp)
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = nuevoComentario,
                onValueChange = { nuevoComentario = it },
                placeholder = { Text("Añade un comentario...", color = Color.Gray) },
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 50.dp), // Usa defaultMinSize en lugar de height fija
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.LightGray,
                    focusedBorderColor = Color(0xFF8830FF),
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                ),
                maxLines = Int.MAX_VALUE, // Permitir múltiples líneas
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (nuevoComentario.isNotBlank()) {
                                comentariosViewModel.crearComentario(nuevoComentario)
                                nuevoComentario = ""
                            }
                        },
                        enabled = !state.isCreating && nuevoComentario.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Enviar comentario",
                            tint = if (!state.isCreating && nuevoComentario.isNotBlank()) Color(0xFF8830FF) else Color.Gray
                        )
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }

        if (state.isCreating) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        state.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = { comentariosViewModel.limpiarError() }) {
                Text("Cerrar")
            }
        }

        // Diálogo para confirmar eliminación
        if (state.mostrandoDialogoEliminar && state.idComentarioAEliminar != null) {
            AlertDialog(
                onDismissRequest = { comentariosViewModel.cancelarEliminarComentario() },
                title = { Text("Eliminar comentario") },
                text = { Text("¿Estás seguro de que quieres eliminar este comentario?") },
                confirmButton = {
                    Button(
                        onClick = { comentariosViewModel.confirmarEliminarComentario() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { comentariosViewModel.cancelarEliminarComentario() }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Lista de comentarios
        if (state.isLoading && state.comentarios.isEmpty()) {
            Box(
                modifier = Modifier.height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF8830FF))
            }
        } else if (state.comentarios.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay comentarios aún. ¡Sé el primero en comentar!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
                    .height(120.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(state.comentarios) { comentario ->
                    ComentarioItem(
                        comentario = comentario,
                        onDeleteClick = { comentariosViewModel.mostrarDialogoEliminar(comentario.idComentario) },
                        esPropietario = comentariosViewModel.esAutorDelComentario(comentario)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ComentarioItem(
    comentario: Comentario,
    onDeleteClick: () -> Unit,
    esPropietario: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF7F7F7)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileAvatarConUrl(
                    imageUrl = comentario.userProfileImageUrl,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comentario.username ?: "Usuario Anónimo",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
                if (esPropietario) {
                    var expandedMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { expandedMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false },
                            modifier = Modifier
                                .width(120.dp)
                                .height(44.dp)  // Ajusta según necesidad
                        ) {
                            DropdownMenuItem(
                                modifier = Modifier.height(30.dp),
                                text = {
                                    Text(
                                        "Eliminar",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                onClick = {
                                    onDeleteClick()
                                    expandedMenu = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = comentario.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            )
        }
    }
}