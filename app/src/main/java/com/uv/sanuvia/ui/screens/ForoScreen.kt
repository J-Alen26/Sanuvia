package com.uv.sanuvia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.uv.sanuvia.data.repository.Publicacion
import java.text.SimpleDateFormat
import java.util.Locale
import com.uv.sanuvia.ui.screens.common.ProfileAvatar

@Composable
fun ForoScreen(
    foroViewModel: ForoScreenViewModel = viewModel(),
) {
    val state by foroViewModel.state.collectAsState()
    var nuevaPublicacion by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        foroViewModel.cargarPublicaciones()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Área de nueva publicación (card blanca con bordes redondeados)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3A3E47)
                        ),
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

        // Diálogo para confirmar eliminación
        if (state.mostrandoDialogoEliminar && state.idPublicacionAEliminar != null) {
            AlertDialog(
                onDismissRequest = { foroViewModel.cancelarEliminarPublicacion() },
                title = { Text("Eliminar publicación") },
                text = { Text("¿Estás seguro de que quieres eliminar esta publicación?") },
                confirmButton = {
                    Button(
                        onClick = { foroViewModel.confirmarEliminarPublicacion() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
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

        // Lista de publicaciones
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
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun AvatarPlaceholder(modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = Color(0xFFE0E0E0),
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Avatar",
            tint = Color(0xFF808080),
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
fun PublicacionItem(
    publicacion: Publicacion,
    onLikeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    esPropietario: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileAvatar()
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = publicacion.authorId,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (esPropietario) {
                    var expandedMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { expandedMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = Color.Gray
                        )
                    }
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = "Editar") },
                            text = { Text("Editar") },
                            onClick = { onEditClick(); expandedMenu = false }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = "Eliminar") },
                            text = { Text("Eliminar") },
                            onClick = { onDeleteClick(); expandedMenu = false }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = publicacion.content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onLikeClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Me gusta",
                        tint = Color(0xFF8830FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "${publicacion.likes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.DarkGray
                )
            }
        }
    }
}