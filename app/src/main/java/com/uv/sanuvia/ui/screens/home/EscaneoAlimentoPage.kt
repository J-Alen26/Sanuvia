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
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt // Icono más estándar para cámara
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
    onTomarFoto: (Uri) -> Unit, // Lambda para pasar el Uri al ViewModel
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    val lazyListState = rememberLazyListState() // Para controlar el scroll de la lista
    val scope = rememberCoroutineScope() // Para lanzar coroutines (ej. para scroll)

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempImageUri?.let { uri ->
                Log.d("EscaneoAlimentosPage", "Foto tomada: $uri")
                onTomarFoto(uri) // Llama a la lambda con el Uri
            } ?: Log.e("EscaneoAlimentosPage", "Error: Uri temporal era null")
        } else {
            Log.e("EscaneoAlimentosPage", "Error al tomar foto o cancelado")
        }
    }

    // Efecto para hacer scroll al inicio de la lista (que visualmente es abajo
    // cuando reverseLayout = true) cuando se añade un nuevo escaneo.
    // Esto asume que los nuevos escaneos se AÑADEN AL PRINCIPIO de uiState.misEscaneos
    // en el ViewModel.
    LaunchedEffect(uiState.misEscaneos.size) {
        if (uiState.misEscaneos.isNotEmpty()) {
            scope.launch {
                // Anima el scroll al primer ítem de la lista de datos (índice 0),
                // que con reverseLayout = true, es el que está más abajo en la pantalla.
                lazyListState.animateScrollToItem(0)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) { // Box para permitir la superposición del FAB
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Text(
                "Escaneos de Alimentos",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .padding(16.dp) // Padding para el título
                    .align(Alignment.CenterHorizontally)
            )

            // Indicador de carga mientras se procesa una imagen nueva
            if (uiState.isScanning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
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
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.misEscaneos.isEmpty() && !uiState.isScanning) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        "Toma una foto para analizar un alimento.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // LazyColumn para mostrar los escaneos
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f) // Ocupa el espacio disponible
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = 80.dp // Aumentado para el FAB
                    ),
                    // --- CAMBIO AQUÍ: reverseLayout = true ---
                    reverseLayout = true // Los items se componen de abajo hacia arriba
                ) {
                    // Asumimos que uiState.misEscaneos ya tiene los más nuevos al principio (índice 0)
                    items(
                        items = uiState.misEscaneos, // Usa la lista en su orden (nuevo primero)
                        key = { escaneo -> escaneo.idEscaneo } // Key para optimización
                    ) { escaneo ->
                        EscaneoItem(escaneo = escaneo) // Usa el Composable con estilo de chat
                    }
                }
            }
        }

        // FloatingActionButton para tomar la foto
        FloatingActionButton(
            onClick = {
                val uri = context.createImageUri()
                tempImageUri = uri
                takePictureLauncher.launch(uri)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Camera, // Icono actualizado
                contentDescription = "Tomar foto de alimento"
            )
        }

    }
}
