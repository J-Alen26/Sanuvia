package com.uv.sanuvia.ui.screens.home


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.uv.sanuvia.ui.screens.HomeScreenState
import com.uv.sanuvia.data.repository.CultivoInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

@Composable
fun UbicacionCultivosPage(
    uiState: HomeScreenState,
    onRetryFetchLocation: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Scroll para toda la página
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Cultivos por Ubicación",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- Mostrar el String de la Dirección ---
        if (uiState.isLocationLoading) {
            CircularProgressIndicator()
            Text(
                "Obteniendo tu ubicación...",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else if (uiState.locationError != null) {
            Text(
                "Error de ubicación: ${uiState.locationError}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRetryFetchLocation, modifier = Modifier.padding(top = 8.dp)) {
                Text("Reintentar")
            }
        } else if (uiState.direccionUsuario != null) {
            Text(
                text = "Cultivos comunes en: ${uiState.direccionUsuario}",
                style = MaterialTheme.typography.titleMedium
            )
            // --- Mostrar Cultivos ---
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
            } else if (uiState.cultivos.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("No se encontraron cultivos para esta área.")
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                // Usamos Column en lugar de LazyColumn porque toda la página ya es scrollable
                // Si esperas MUCHOS cultivos, podrías necesitar un LazyColumn con altura fija.
                uiState.cultivos.forEach { cultivo ->
                    CultivoItemConImagen(cultivo = cultivo) // Reutiliza el Composable
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        } else {
            Text(
                "No se pudo determinar tu ubicación actual.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onRetryFetchLocation, modifier = Modifier.padding(top = 8.dp)) {
                Text("Obtener Ubicación")
            }
        }
    }
}

@Composable
fun CultivoItemConImagen(cultivo: CultivoInfo, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top // Para que el texto no se centre si la imagen es alta
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(cultivo.urlImagen)
                .crossfade(true)
                // .placeholder(R.drawable.ic_placeholder_crop)
                // .error(R.drawable.ic_error_crop)
                .build(),
            contentDescription = "Imagen de ${cultivo.nombre}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cultivo.nombre,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = cultivo.descripcion,
                style = MaterialTheme.typography.bodyMedium
                // Quita maxLines si quieres ver toda la descripción
            )
        }
    }
}
