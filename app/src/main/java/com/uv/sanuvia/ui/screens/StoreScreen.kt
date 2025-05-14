package com.uv.sanuvia.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter // implementation("io.coil-kt:coil-compose:2.6.0")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    storeViewModel: StoreScreenViewModel = viewModel()
    // Si necesitas el ComentariosViewModel para otra cosa, puedes mantenerlo
    // comentariosViewModel: ComentariosViewModel = viewModel()
) {
    val state by storeViewModel.state.collectAsState()

    Scaffold(

    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Aplicar padding del Scaffold
                .padding(horizontal = 8.dp), // Padding horizontal general
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp) // Espacio arriba y abajo del contenido
        ) {
            // 1. Área para ofrecer un nuevo cultivo
            item {
                OfrecerCultivoCard(
                    state = state,
                    onNombreChange = storeViewModel::onNombreCultivoCambiado,
                    onDescripcionChange = storeViewModel::onDescripcionCultivoCambiada,
                    onPrecioChange = storeViewModel::onPrecioCultivoCambiado,
                    onUnidadChange = storeViewModel::onUnidadCultivoCambiada,
                    onPublicarClick = storeViewModel::iniciarPublicacionCultivo,
                    onLimpiarMensaje = storeViewModel::limpiarMensaje
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. Título de la sección de lista
            item {
                Text(
                    "Consume local, consume cultivos de Coatzacoalcos: ",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 8.dp), // Padding para el título
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
                Divider(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
            }

            // 3. Lista de cultivos
            when {
                state.isLoading && state.cultivos.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                            Text("Cargando cultivos locales...", modifier = Modifier.padding(top = 60.dp))
                        }
                    }
                }
                state.cultivos.isEmpty() && !state.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 50.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Agriculture, contentDescription = "Sin cultivos", modifier = Modifier.size(48.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Aún no hay cultivos ofrecidos.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.Gray
                                )
                                Text(
                                    "¡Anímate a ser el primero en ofrecer tus productos de Coatzacoalcos!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
                else -> {
                    items(state.cultivos, key = { it.id }) { cultivo ->
                        CultivoLocalCard(cultivo = cultivo)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    // Diálogo de pago simulado
    if (state.mostrandoDialogoPago) {
        SimularPagoDialog(
            onDismissRequest = { storeViewModel.cerrarDialogoPago() },
            onConfirmPago = { storeViewModel.procesarPagoParaPublicar(true) },
            onCancelPago = { storeViewModel.procesarPagoParaPublicar(false) }
        )
    }
}

@Composable
fun OfrecerCultivoCard(
    state: StoreScreenState,
    onNombreChange: (String) -> Unit,
    onDescripcionChange: (String) -> Unit,
    onPrecioChange: (String) -> Unit,
    onUnidadChange: (String) -> Unit,
    onPublicarClick: () -> Unit,
    onLimpiarMensaje: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp), // Un poco de padding para que no pegue a los bordes
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Color blanco o superficie del tema
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Ofrece tus Cultivos 😊",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            OutlinedTextField(
                value = state.nombreNuevoCultivo,
                onValueChange = onNombreChange,
                label = { Text("Nombre del Cultivo (ej: Mango Ataulfo)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.descripcionNuevoCultivo,
                onValueChange = onDescripcionChange,
                label = { Text("Breve descripción (ej: Dulce y jugoso, cosechado hoy)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                OutlinedTextField(
                    value = state.precioNuevoCultivo,
                    onValueChange = onPrecioChange,
                    label = { Text("Precio") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.PriceCheck, contentDescription = "Precio") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Podrías usar un ExposedDropdownMenuBox para seleccionar unidades
                OutlinedTextField(
                    value = state.unidadNuevoCultivo,
                    onValueChange = onUnidadChange, // Aquí deberías tener una lista de unidades o permitir texto libre
                    label = { Text("Unidad") }, // Ej: kg, pieza, manojo
                    modifier = Modifier.weight(0.7f),
                    singleLine = true
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (state.isPublicandoCultivo) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            }

            state.mensajePublicacion?.let {
                Text(
                    text = it,
                    color = if (it.contains("éxito")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LaunchedEffect(it) {
                    kotlinx.coroutines.delay(4000)
                    onLimpiarMensaje()
                }
            }

            Button(
                onClick = onPublicarClick,
                modifier = Modifier.align(Alignment.End),
                enabled = !state.isPublicandoCultivo,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)) // Un verde azulado
            ) {
                Icon(Icons.Filled.Sell, contentDescription = "Ofrecer Cultivo", modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Ofrecer Cultivo")
            }
        }
    }
}


@Composable
fun CultivoLocalCard(cultivo: CultivoLocalItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = cultivo.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = cultivo.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Precio: ${cultivo.precioPorUnidad}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ofrecido por: ${cultivo.vendedorNombre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "Origen: ${cultivo.ubicacion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

            }
        }
    }
}

@Composable
fun SimularPagoDialog( // Misma función de diálogo de pago que antes
    onDismissRequest: () -> Unit,
    onConfirmPago: () -> Unit,
    onCancelPago: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Confirmar Publicación de Cultivo") },
        text = { Text("Para ofrecer tu cultivo en el mercado, se simulará un pequeño cargo de publicación de $0.99 MXN. ¿Deseas continuar con el proceso?") },
        confirmButton = {
            Button(
                onClick = onConfirmPago,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)) // Verde más oscuro
            ) {
                Text("Sí, Pagar y Ofrecer")
            }
        },
        dismissButton = {
            Button(
                onClick = onCancelPago,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text("Cancelar")
            }
        }
    )
}