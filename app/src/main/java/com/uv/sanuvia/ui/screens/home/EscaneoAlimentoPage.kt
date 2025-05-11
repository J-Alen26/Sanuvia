package com.uv.sanuvia.ui.screens.home

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.uv.sanuvia.ui.screens.HomeScreenState
import com.uv.sanuvia.ui.screens.common.EscaneoItem // Importa el EscaneoItem con estilo chat
import com.uv.sanuvia.ui.screens.common.createImageUri
import kotlinx.coroutines.launch

@Composable
fun EscaneoAlimentosPage(
    uiState: HomeScreenState,
    onTomarFoto: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    val lazyListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempImageUri?.let { uri ->
                Log.d("EscaneoAlimentosPage", "Foto tomada: $uri")
                onTomarFoto(uri)
            } ?: Log.e("EscaneoAlimentosPage", "Error: Uri temporal era null")
        } else {
            Log.e("EscaneoAlimentosPage", "Error al tomar foto o cancelado")
        }
    }

    LaunchedEffect(uiState.misEscaneos.size) {
        if (uiState.misEscaneos.isNotEmpty()) {
            scope.launch {
                // Scroll al nuevo item (índice 0 debido a reverseLayout=true)
                lazyListState.animateScrollToItem(0)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                "Mis Alimentos",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )

            // Indicador de carga mientras se procesa una imagen nueva
            if (uiState.isScanning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    // Esta Row ocupa todo el ancho y centra su contenido (indicador + texto)
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analizando tu alimento...")
                }
            }

            // Lista de escaneos
            if (uiState.isScanListLoading && uiState.misEscaneos.isEmpty() && !uiState.isScanning) {
                // Este Box ocupa todo el espacio restante (weight(1f))
                // y centra el CircularProgressIndicator en ese espacio.
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(), // fillMaxWidth para asegurar que el centrado horizontal del Box funcione
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.misEscaneos.isEmpty() && !uiState.isScanning) {
                // Similar al anterior, para centrar el mensaje de "sin escaneos".
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Toma una foto para ver que hay en tu comida 😊",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = 80.dp // Espacio para el FAB
                    ),
                    reverseLayout = true // Para estilo chat (nuevos abajo)
                ) {
                    items(
                        items = uiState.misEscaneos, // Asumiendo nuevos al principio de la lista
                        key = { escaneo -> escaneo.idEscaneo }
                    ) { escaneo ->
                        EscaneoItem(escaneo = escaneo)
                    }
                }
            }
        } // Fin Column

        FloatingActionButton(
            onClick = {
                val uri = context.createImageUri()
                tempImageUri = uri
                takePictureLauncher.launch(uri)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Tomar foto de alimento"
            )
        }
    } // Fin Box
}
