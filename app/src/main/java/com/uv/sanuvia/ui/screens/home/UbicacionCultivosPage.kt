package com.uv.sanuvia.ui.screens.home

import android.service.autofill.OnClickAction
import android.util.Log // Importa Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn // Asegúrate que está si lo usas en la página
import androidx.compose.foundation.lazy.items // Asegúrate que está si lo usas en la página
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
// import androidx.compose.runtime.LaunchedEffect // Ya no es necesario para el painter aquí
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter // Para el tipo en onState
// import coil.compose.rememberAsyncImagePainter // Ya no creamos el painter manualmente aquí
import coil.request.ImageRequest
import com.uv.sanuvia.ui.screens.HomeScreenState // Asegúrate que la ruta es correcta
// Asegúrate que este import apunta a tu CultivoInfo.kt con el campo urlImagen
import com.uv.sanuvia.data.repository.CultivoInfo

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
                style = MaterialTheme.typography.titleSmall
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
                Column {
                    uiState.cultivos.forEachIndexed { index, cultivo ->
                        CultivoItemConImagen(cultivo = cultivo) // Llama al Composable corregido
                        if (index < uiState.cultivos.size - 1) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
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
    // Log para ver la URL que se está intentando cargar
    Log.d("CultivoItemView", "Cultivo: ${cultivo.nombre}, URL Imagen: ${cultivo.urlImagen}")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {




        // --- CORRECCIÓN AQUÍ: Usar AsyncImage con 'model' y 'onState' ---
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(cultivo.urlImagen) // La URL de la imagen del cultivo
                .crossfade(true)
                .build(),
            contentDescription = "Imagen de ${cultivo.nombre}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            // Callback para observar el estado de la carga
            onState = { state ->
                Log.d("CultivoItemView", "Cultivo: ${cultivo.nombre}, Estado de AsyncImage: $state")
                if (state is AsyncImagePainter.State.Error) {
                    Log.e(
                        "CultivoItemView",
                        "Error al cargar imagen para ${cultivo.nombre}: ${state.result.throwable.localizedMessage}",
                        state.result.throwable
                    )
                }
            }
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cultivo.nombre,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(4.dp))


            Text(
            text = cultivo.descripcion,
            style = MaterialTheme.typography.bodyMedium
            )
        }

    }
}
