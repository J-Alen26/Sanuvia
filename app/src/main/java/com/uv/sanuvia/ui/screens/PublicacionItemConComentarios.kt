package com.uv.sanuvia.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.uv.sanuvia.data.repository.Publicacion
import com.uv.sanuvia.ui.screens.common.ProfileAvatarConUrl

@Composable
fun PublicacionItemConComentarios(
    publicacion: Publicacion,
    onLikeClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    esPropietario: Boolean,
    comentariosViewModel: ComentariosViewModel
) {
    var mostrarComentarios by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            // Header con avatar, nombre y opciones
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfileAvatarConUrl(
                    imageUrl = publicacion.userProfileImageUrl,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = publicacion.username ?: "Usuario Anónimo",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                if (esPropietario) {
                    var expandedMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(
                            onClick = { expandedMenu = true },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false },
                            modifier = Modifier.height(75.dp)
                        ) {
                            DropdownMenuItem(
                                modifier = Modifier.height(30.dp),
                                text = {
                                    Text(
                                        "Editar",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                },
                                onClick = {
                                    onEditClick()
                                    expandedMenu = false
                                }
                            )
                            Divider(modifier = Modifier.padding(horizontal = 3.dp))
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

            Spacer(modifier = Modifier.height(15.dp))

            // Contenido de la publicación
            Text(
                text = publicacion.content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Acciones (like y comentarios)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Botón de like
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLikeClick() }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = "Me gusta",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = publicacion.likes.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Botón de comentarios
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { mostrarComentarios = !mostrarComentarios }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comentarios",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = if (mostrarComentarios) "Ocultar" else "Comentarios",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            // Sección de comentarios (expandible)
            if (mostrarComentarios) {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    color = Color.LightGray.copy(alpha = 0.5f),
                    thickness = 0.8.dp
                )

                ComentariosSeccion(
                    publicacionId = publicacion.idPublicacion,
                    comentariosViewModel = comentariosViewModel
                )
            }
        }
    }
}