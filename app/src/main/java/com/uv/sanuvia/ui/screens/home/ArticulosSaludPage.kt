package com.uv.sanuvia.ui.screens.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator // Para el caso de carga
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.uv.sanuvia.ui.screens.HomeScreenState
// Asegúrate que esta es la ruta correcta a tu modelo
import com.uv.sanuvia.data.repository.EnfermedadInfantil.EnfermedadInfantilModel

@Composable
fun ArticulosSaludPage(
    uiState: HomeScreenState,
    // Nueva lambda para manejar el clic en un artículo, pasando un identificador
    onArticuloClick: (articuloId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 16.dp) // Padding general
    ) {
        Text(
            "Artículos de Salud",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp) // Padding para el título
        )

        // Asumiendo que uiState.articles es la lista de EnfermedadInfantilModel
        if (uiState.articles.isEmpty()) {
            // Podrías tener un estado de carga específico para los artículos
            // if (uiState.isArticlesLoading) { CircularProgressIndicator() } else ...
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay artículos disponibles en este momento.")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // Muestra 2 columnas, puedes ajustar
                contentPadding = PaddingValues(8.dp), // Espacio alrededor de la retícula
                verticalArrangement = Arrangement.spacedBy(12.dp), // Espacio vertical
                horizontalArrangement = Arrangement.spacedBy(12.dp) // Espacio horizontal
            ) {
                items(
                    items = uiState.articles,
                    // Es MUY recomendable usar un ID único y estable si tu modelo lo tiene
                    // key = { article -> article.id }
                    key = { article -> article.titulo } // Usando título como key temporalmente
                ) { article ->
                    ArticuloGridItem(
                        articulo = article,
                        onItemClick = {
                            // Pasa el título como ID. Si tienes un ID real, úsalo.
                            onArticuloClick(article.titulo)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ArticuloGridItem(
    articulo: EnfermedadInfantilModel,
    onItemClick: () -> Unit, // Lambda para cuando se hace clic en este item
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick), // Hace toda la tarjeta clicable
        shape = RoundedCornerShape(12.dp), // Bordes redondeados para la tarjeta
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // Sombra sutil
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize() // Para que la columna ocupe la tarjeta
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(articulo.urlImage)
                    .crossfade(true)
                    // TODO: Añade drawables de placeholder y error
                    // .placeholder(R.drawable.placeholder_articulo)
                    // .error(R.drawable.error_articulo)
                    .build(),
                contentDescription = "Imagen de ${articulo.titulo}",
                contentScale = ContentScale.Crop, // Para que la imagen llene el espacio
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Mantiene la imagen cuadrada, ajusta según necesidad
                    // No necesitas .clip() aquí si la Card ya tiene forma y la imagen llena
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                onState = { state -> // Log para depurar carga de imagen
                    Log.d(
                        "ArticuloGridItem",
                        "Artículo: ${articulo.titulo}, Estado Imagen: $state"
                    )
                    if (state is AsyncImagePainter.State.Error) {
                        Log.e(
                            "ArticuloGridItem",
                            "Error al cargar imagen para ${articulo.titulo}: " +
                                    state.result.throwable.localizedMessage,
                            state.result.throwable
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = articulo.titulo,
                style = MaterialTheme.typography.titleSmall, // Estilo para el título en la tarjeta
                textAlign = TextAlign.Center,
                maxLines = 2, // Evita que títulos muy largos rompan el diseño
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(4.dp)) // Pequeño espacio al final de la tarjeta
        }
    }
}
