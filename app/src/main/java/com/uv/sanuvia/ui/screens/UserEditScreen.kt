package com.uv.sanuvia.ui.screens // O el paquete correcto de tu UI

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Icono para atrás
import androidx.compose.material.icons.filled.Edit // Example icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
// Asegúrate de tener el ViewModel correcto importado
// import com.uv.sanuvia.viewmodels.UserEditScreenViewModel // Cambia si es necesario

// Reemplaza con la ruta real a tu ViewModel si es diferente
// Asegúrate de que la ruta a tu State sea correcta


@OptIn(ExperimentalMaterial3Api::class) // Necesario para TopAppBar
@Composable
fun UserEditScreen(
    // Lambdas para navegación, inyectadas desde el grafo de navegación
    onNavigateToLogin: () -> Unit, // Para después de borrar cuenta
    onNavigateToHome: () -> Unit,  // Para el botón de regresar
    viewModel: UserEditScreenViewModel = viewModel() // Usa el ViewModel correcto
) {
    // --- Estado y Contexto ---
    val uiState by viewModel.state.collectAsState() // Observa el estado del ViewModel
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // --- Launchers ---
    // Launcher para seleccionar imagen de la galería
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        // Llama al ViewModel si se selecciona una Uri
        uri?.let { viewModel.actualizarFotoPerfil(it) }
    }

    // --- Estado local de la UI ---
    // Guarda el nombre de usuario editable, inicializado desde el estado del ViewModel
    var editableUsername by remember(uiState.username) { mutableStateOf(uiState.username) }
    // Controla la visibilidad del diálogo de confirmación para borrar cuenta
    var showDeleteDialog by remember { mutableStateOf(false) }

    // --- Efectos (LaunchedEffect) ---
    // Muestra mensajes (errores o éxito) en el Snackbar
    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearMessages() // Limpia el mensaje después de mostrarlo
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            viewModel.clearMessages() // Limpia el mensaje después de mostrarlo
        }
    }

    // Navega a Login si la cuenta se elimina con éxito
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage == "Cuenta eliminada.") {
            onNavigateToLogin() // Ejecuta la navegación a Login
        }
    }

    // --- UI Principal con Scaffold ---
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Host para los Snackbars
        topBar = {
            // Barra superior con título y botón de regreso
            TopAppBar(
                title = { Text("Editar Perfil") },
                navigationIcon = {
                    // Botón para ejecutar la navegación de regreso
                    IconButton(onClick = onNavigateToHome) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                }
                // Puedes añadir 'actions = { ... }' si necesitas más iconos a la derecha
            )
        }
    ) { paddingValues -> // paddingValues contiene el espacio ocupado por TopAppBar/BottomBar
        // Contenido principal de la pantalla
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Aplica el padding del Scaffold ¡IMPORTANTE!
                .padding(16.dp) // Tu padding adicional
                .verticalScroll(rememberScrollState()), // Permite scroll si el contenido excede la pantalla
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // --- Sección de Imagen de Perfil ---
            Box(contentAlignment = Alignment.BottomEnd) {
                // Prepara el pintor de Coil para cargar la imagen asíncronamente
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(uiState.photoUrl) // Usa la URL del estado del ViewModel
                        .crossfade(true)
                        // Considera añadir .placeholder() y .error() con drawables
                        // .placeholder(R.drawable.default_avatar)
                        // .error(R.drawable.default_avatar)
                        .build(),
                    contentScale = ContentScale.Crop // Asegura que la imagen llene el círculo
                )

                // Muestra la imagen
                Image(
                    painter = painter,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape) // Forma circular
                        .background(MaterialTheme.colorScheme.surfaceVariant) // Fondo mientras carga
                        .clickable { // Permite cambiar la foto al hacer clic en la imagen
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                )

                // Icono de Edición superpuesto
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Cambiar foto",
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(6.dp)
                        .align(Alignment.BottomEnd), // Posiciona en la esquina inferior derecha
                    tint = MaterialTheme.colorScheme.onPrimary // Color del icono
                )

                // Indicador de carga sobre la imagen (si aplica)
                // Se podría vincular a un estado booleano específico para la carga de la foto
                if (uiState.isLoading && painter.state is AsyncImagePainter.State.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(120.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            .padding(30.dp)
                            .align(Alignment.Center)
                    )
                }
            } // Fin Box Imagen

            Spacer(modifier = Modifier.height(16.dp)) // Espacio aumentado

            // Muestra el nombre de usuario actual (no editable aquí)
            Text(
                text = uiState.username,
                style = MaterialTheme.typography.headlineSmall // Estilo más grande
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Sección para Editar Nombre de Usuario ---
            OutlinedTextField(
                value = editableUsername,
                onValueChange = { editableUsername = it },
                label = { Text("Nombre de usuario") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = uiState.error?.contains("nombre") == true // Ejemplo de marcar error
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para Guardar Cambios en el nombre
            Button(
                onClick = { viewModel.actualizarUsuario(editableUsername) },
                modifier = Modifier.fillMaxWidth(),
                // Habilitado solo si el nombre cambió Y no hay otra operación en curso
                enabled = uiState.username != editableUsername && !uiState.isLoading
            ) {
                // Muestra indicador de progreso si se está guardando el nombre
                // (Necesitarías un estado más granular que solo uiState.isLoading general)
                // if (uiState.isSavingUsername) { // Ejemplo con estado específico
                if (uiState.isLoading && uiState.username != editableUsername) { // Estimación
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary // Color sobre el botón
                    )
                } else {
                    Text("Guardar Cambios")
                }
            } // Fin Botón Guardar

            // Empuja el botón de eliminar hacia abajo
            Spacer(modifier = Modifier.weight(1f))

            // --- Botón Eliminar Cuenta ---
            Button(
                onClick = { showDeleteDialog = true }, // Abre el diálogo de confirmación
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                enabled = !uiState.isLoading // Deshabilitado si cualquier operación está en curso
            ) {
                // Muestra texto diferente si está cargando (ej. durante la eliminación)
                // if (uiState.isDeleting) { // Ejemplo con estado específico
                if (uiState.isLoading) { // Usa isLoading general por ahora
                    Text("Procesando...")
                } else {
                    Text("Eliminar Cuenta")
                }
            }
            Spacer(modifier = Modifier.height(8.dp)) // Pequeño espacio al final

        } // Fin Column Principal
    } // Fin Scaffold

    // --- Diálogo de Confirmación para Eliminar Cuenta ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false }, // Cierra si se toca fuera
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar tu cuenta? Esta acción es permanente y no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false // Cierra el diálogo
                        viewModel.eliminarCuenta() // Llama al ViewModel para eliminar
                        // La navegación a Login se maneja con LaunchedEffect observando el estado
                    },
                    // Color rojo para el botón de confirmar eliminación
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false } // Simplemente cierra el diálogo
                ) {
                    Text("Cancelar")
                }
            }
        )
    } // Fin AlertDialog
} // Fin Composable UserEditScreen

// Puedes añadir un Preview si quieres
// @Preview(showBackground = true)
// @Composable
// fun UserEditScreenPreview() {
//     // Necesitarías un ViewModel de previsualización o pasar estados falsos
//     UserEditScreen(onNavigateToLogin = {}, onNavigateToHome = {})
// }