package com.uv.sanuvia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
// Asegúrate que la ruta a tu modelo es correcta
import com.uv.sanuvia.data.repository.EnfermedadInfantil.EnfermedadInfantilModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticuloDetalleScreen(
    articulo: EnfermedadInfantilModel, // Recibe el objeto artículo completo
    onNavigateBack: () -> Unit        // Lambda para manejar la navegación hacia atrás
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = articulo.titulo,
                        maxLines = 1, // Evita que títulos largos ocupen mucho espacio
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Aplica padding del Scaffold
                .padding(16.dp)         // Padding adicional para el contenido
                .verticalScroll(rememberScrollState()) // Permite scroll
        ) {
            // Imagen del Artículo (más grande)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(articulo.urlImage)
                    .crossfade(true)
                    // TODO: Añade placeholders y error drawables
                    // .placeholder(R.drawable.placeholder_articulo_detalle)
                    // .error(R.drawable.error_articulo_detalle)
                    .build(),
                contentDescription = "Imagen de ${articulo.titulo}",
                contentScale = ContentScale.Crop, // O Fit, según prefieras
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp) // Altura mayor para la imagen de detalle
                    .clip(RoundedCornerShape(16.dp)) // Bordes redondeados
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )

            Spacer(modifier = Modifier.height(16.dp))



            Spacer(modifier = Modifier.height(16.dp))

            // Descripción del Artículo
            Text(
                text = articulo.descripcion, // Ahora mostramos la descripción
                style = MaterialTheme.typography.bodyLarge // Texto de cuerpo más grande
            )

            Spacer(modifier = Modifier.height(16.dp))
            // Aquí podrías añadir más detalles del artículo si los tuvieras
        }
    }
}
