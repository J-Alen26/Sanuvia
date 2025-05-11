package com.uv.sanuvia.ui.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Compost
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.PagerState
import com.google.firebase.auth.FirebaseAuth
import com.uv.sanuvia.data.repository.EscaneoAlimento
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.layout.widthIn // Para limitar el ancho de la burbuja
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.style.TextAlign



@Composable
fun ProfileAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val user = FirebaseAuth.getInstance().currentUser
    val photoUri = user?.photoUrl

    if (photoUri == null) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Avatar placeholder",
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoUri)
                .crossfade(true)
                .build(),
            contentDescription = "Avatar de usuario",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun PagerIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier,
    onIconClick: (Int) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(0.dp)
            .height(30.dp),

        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        val icons = listOf(
            Icons.Filled.CropFree to "Escanear Alimento",
            Icons.Filled.Compost to "Cultivos por Ubicación",
            Icons.Filled.ChildCare to "Artículos de Salud"
        )

        repeat(pageCount) { index ->
            val isSelected = pagerState.currentPage == index
            val (icon, contentDescription) = icons[index]

            IconButton(onClick = { onIconClick(index) }) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            }
        }
    }
}

/**
 * Composable para mostrar un ítem de escaneo de alimento con estilo de chat.
 * Muestra la imagen del alimento y el resultado del análisis de la IA.
 */
@Composable
fun EscaneoItem(escaneo: EscaneoAlimento, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        // --- Burbuja para la Imagen Enviada (simulada) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End // Alinea a la derecha
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 300.dp) // Limita el ancho de la burbuja
                    .padding(start = 40.dp), // Espacio a la izquierda
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 4.dp, // Esquina menos redondeada para el "pico"
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(escaneo.urlFoto)
                        .crossfade(true)
                        // .placeholder(R.drawable.placeholder_scan_image)
                        // .error(R.drawable.error_scan_image)
                        .build(),
                    contentDescription = "Imagen escaneada: ${escaneo.resultado.take(20)}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp)) // Espacio entre burbujas (puede ser 4.dp o 6.dp)

        // --- Burbuja para el Resultado de la IA (simulada) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start // Alinea a la izquierda
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 300.dp) // Limita el ancho
                    .padding(end = 40.dp), // Espacio a la derecha
                shape = RoundedCornerShape(
                    topStart = 4.dp, // Esquina menos redondeada para el "pico"
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Análisis Nutricional:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = escaneo.resultado,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val formattedDate = remember(escaneo.fechaHora) {
                        escaneo.fechaHora?.toDate()?.let {
                            SimpleDateFormat(
                                "dd/MM/yy HH:mm",
                                Locale.getDefault()
                            ).format(it)
                        } ?: ""
                    }
                    if (formattedDate.isNotEmpty()) {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        // --- CAMBIO AQUÍ: Aumentar el espacio inferior después de cada par de mensajes ---
        Spacer(modifier = Modifier.height(20.dp)) // Antes era 12.dp
    }
}
