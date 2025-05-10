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
import androidx.compose.foundation.layout.width
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

@Composable
fun EscaneoItem(escaneo: EscaneoAlimento, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(escaneo.urlFoto)
                .crossfade(true)
                // .placeholder(R.drawable.placeholder_image_scan)
                // .error(R.drawable.error_image_scan)
                .build(),
            contentDescription = "Imagen escaneada ${escaneo.idEscaneo}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.3f)
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp)) // Un poco más suave que CircleShape
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = escaneo.resultado,
                style = MaterialTheme.typography.bodyMedium

            )
            Spacer(modifier = Modifier.height(4.dp))
            val formattedDate = remember(escaneo.fechaHora) {
                escaneo.fechaHora?.toDate()?.let {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                } ?: "Fecha desconocida"
            }
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
