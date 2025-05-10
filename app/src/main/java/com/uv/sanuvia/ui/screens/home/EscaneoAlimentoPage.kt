package com.uv.sanuvia.ui.screens.home


import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.uv.sanuvia.ui.screens.HomeScreenState
import com.uv.sanuvia.ui.screens.common.EscaneoItem
import com.uv.sanuvia.ui.screens.common.createImageUri

@Composable
fun EscaneoAlimentosPage(
    uiState: HomeScreenState,
    onTomarFoto: (Uri) -> Unit, // Lambda para pasar el Uri al ViewModel
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

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

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Mis Escaneos", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isScanning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Procesando imagen...")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.isScanListLoading && uiState.misEscaneos.isEmpty()) {
                CircularProgressIndicator()
            } else if (uiState.misEscaneos.isEmpty() && !uiState.isScanListLoading) {
                Text("Aún no has realizado ningún escaneo.")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),) {
                    items(uiState.misEscaneos) { escaneo ->
                        EscaneoItem(escaneo = escaneo)
                        HorizontalDivider()
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                val uri = context.createImageUri()
                tempImageUri = uri
                takePictureLauncher.launch(uri)
            },
            modifier = Modifier
                .size(110.dp)
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = "Tomar foto alimento",

            )
        }
    }
}
