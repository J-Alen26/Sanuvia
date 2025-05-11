package com.uv.sanuvia.ui.screens.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items // Import para items en LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
// Asegúrate que este import apunta a tu CultivoInfo.kt con el campo urlImagen
import com.uv.sanuvia.data.repository.CultivoInfo // Cambiado desde data.repository a data.model

@Composable
fun UbicacionCultivosPage(
    uiState: HomeScreenState,
    onRetryFetchLocation: () -> Unit,
    onCultivoClick: (cultivo: CultivoInfo) -> Unit, // Lambda para clic en cultivo
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 16.dp), // Padding general
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Título y manejo de estado de ubicación (sin cambios significativos)
        if (uiState.direccionUsuario != null) {
            Text(
                text = "Cultivos en: ${uiState.direccionUsuario}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        } else {
            Text(
                "Ubicación y Cultivos",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }


        if (uiState.isLocationLoading && uiState.direccionUsuario == null) {
            CircularProgressIndicator()
            Text(
                "Obteniendo tu ubicación...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else if (uiState.locationError != null && uiState.direccionUsuario == null) {
            Text(
                "Error de ubicación: ${uiState.locationError}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRetryFetchLocation, modifier = Modifier.padding(top = 8.dp)) {
                Text("Reintentar")
            }
        } else if (uiState.direccionUsuario == null && !uiState.isLocationLoading) {
            Text(
                "No se pudo determinar tu ubicación actual.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRetryFetchLocation, modifier = Modifier.padding(top = 8.dp)) {
                Text("Obtener Ubicación")
            }
        }


        // --- Mostrar Cultivos en Retícula ---
        if (uiState.isCultivosLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
            Text(
                "Buscando cultivos...",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else if (uiState.cultivosError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Error al cargar cultivos: ${uiState.cultivosError}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        } else if (uiState.cultivos.isEmpty() && uiState.direccionUsuario != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("No se encontraron cultivos para esta área.")
        } else if (uiState.cultivos.isNotEmpty()) {
            // Usamos LazyVerticalGrid para la retícula
            LazyVerticalGrid(
                columns = GridCells.Fixed(2), // 2 columnas, puedes ajustar
                contentPadding = PaddingValues(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize() // Para que ocupe el espacio disponible
            ) {
                items(uiState.cultivos, key = { cultivo -> cultivo.nombre }) { cultivo ->
                    CultivoGridItem(
                        cultivo = cultivo,
                        onItemClick = { onCultivoClick(cultivo) } // Pasa el objeto cultivo completo
                    )
                }
            }
        }
    }
}

@Composable
fun CultivoGridItem(
    cultivo: CultivoInfo,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick), // Hace la tarjeta clicable
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize() // La columna ocupa la tarjeta
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(cultivo.urlImagen)
                    .crossfade(true)
                    // TODO: Añade placeholders y error drawables
                    // .placeholder(R.drawable.ic_placeholder_cultivo_grid)
                    // .error(R.drawable.ic_error_cultivo_grid)
                    .build(),
                contentDescription = "Imagen de ${cultivo.nombre}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Imagen cuadrada
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                onState = { state ->
                    Log.d(
                        "CultivoGridItem",
                        "Cultivo: ${cultivo.nombre}, Estado Imagen: $state"
                    )
                    if (state is AsyncImagePainter.State.Error) {
                        Log.e(
                            "CultivoGridItem",
                            "Error al cargar imagen para ${cultivo.nombre}: " +
                                    state.result.throwable.localizedMessage,
                            state.result.throwable
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = cultivo.nombre,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            // La descripción detallada se mostrará en la pantalla de detalle
        }
    }
}
