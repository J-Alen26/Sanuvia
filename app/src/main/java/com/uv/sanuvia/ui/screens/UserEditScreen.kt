package com.uv.sanuvia.ui.screens

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
import com.uv.sanuvia.R // Assuming you have a default avatar drawable

@Composable
fun UserEditScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: UserEditScreenViewModel = viewModel() // Use default viewModel()
) {
    val userState by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Use the new Photo Picker API
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.actualizarFotoPerfil(it) }
    }

    // State for the editable username, initialized from the view model state
    var editableUsername by remember(userState.username) { mutableStateOf(userState.username) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Effect to show snackbars for errors or success messages
    LaunchedEffect(userState.error, userState.successMessage) {
        userState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessages() // Clear the message after showing
        }
        userState.successMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearMessages() // Clear the message after showing
        }
    }

    // Effect to navigate back to login after successful deletion
    LaunchedEffect(userState.successMessage) {
        if (userState.successMessage == "Cuenta eliminada.") {
            // Delay briefly to allow snackbar to show if desired
            // kotlinx.coroutines.delay(1500)
            onNavigateToLogin()
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .padding(16.dp) // Add own padding
                .verticalScroll(rememberScrollState()), // Make content scrollable
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            // Profile Image Section
            Box(contentAlignment = Alignment.BottomEnd) {
                val painter = rememberAsyncImagePainter(
                    model = ImageRequest.Builder(context)
                        .data(userState.photoUrl) // Use state URL or default
                        .crossfade(true)
                        .build(),
                    contentScale = ContentScale.Crop // Crop to fit the circle
                )

                Image(
                    painter = painter,
                    contentDescription = "Foto de perfil",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant) // Placeholder bg
                        .clickable {
                            // Launch photo picker
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                )

                // Edit Icon Overlay
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Cambiar foto",
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(6.dp)
                        .align(Alignment.BottomEnd), // Position at bottom right
                    tint = MaterialTheme.colorScheme.onPrimary
                )

                // Show loading indicator over the image while uploading
                if (userState.isLoading && painter.state is AsyncImagePainter.State.Loading) { // Or a specific loading flag for photo upload
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(120.dp) // Match image size
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape) // Semi-transparent background
                            .padding(30.dp) // Padding inside the circle
                            .align(Alignment.Center) // Center the indicator
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(userState.username)

            Spacer(modifier = Modifier.height(24.dp))

            // Username Display/Edit Section
            OutlinedTextField(
                value = editableUsername,
                onValueChange = { editableUsername = it },
                label = { Text("Nombre de usuario") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = { viewModel.actualizarUsuario(editableUsername) },
                modifier = Modifier.fillMaxWidth(),
                enabled = userState.username != editableUsername && !userState.isLoading // Enable only if changed and not loading
            ) {
                if (userState.isLoading && userState.username != editableUsername) { // Show specific loading for this action
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color=MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Guardar Cambios")
                }
            }


            Spacer(modifier = Modifier.weight(1f)) // Pushes delete button to bottom

            // Delete Account Button
            Button(
                onClick = { showDeleteDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                enabled = !userState.isLoading // Disable if any operation is loading
            ) {
                if (userState.isLoading) { // Show generic loading indicator if needed
                    // Optional: Could check a specific 'isDeleting' flag
                    Text("Procesando...")
                } else {
                    Text("Eliminar Cuenta")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))


        } // End Column
    } // End Scaffold

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar Eliminación") },
            text = { Text("¿Estás seguro de que deseas eliminar tu cuenta? Esta acción es permanente y no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.eliminarCuenta()
                        // Navigation is now handled by LaunchedEffect listening to state changes
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}