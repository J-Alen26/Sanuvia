package com.uv.sanuvia.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uv.sanuvia.R // Asegúrate que R se importa correctamente para el drawable

// Asume que LoginScreenViewModel, LoginState y PasswordResetState están definidos
// y se importan correctamente.

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: LoginScreenViewModel = viewModel() // Inyecta o usa el viewModel por defecto
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) } // Para errores de validación local

    val loginState by viewModel.loginState.collectAsState()
    val passwordResetState by viewModel.passwordResetState.collectAsState()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Efecto para navegar a Home si el login es exitoso
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            onNavigateToHome()
            viewModel.resetLoginState() // Limpia el estado después de navegar
        }
    }
    // Efecto para limpiar errores locales si el estado del ViewModel cambia
    LaunchedEffect(loginState, passwordResetState) {
        if (loginState !is LoginState.Idle || passwordResetState !is PasswordResetState.Idle) {
            localError = null
        }
    }


    Scaffold(
        // No usamos TopAppBar para un look más moderno, el logo tomará protagonismo
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                // Un degradado sutil de fondo podría verse bien
                // .background(
                //     brush = Brush.verticalGradient(
                //         colors = listOf(
                //             MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                //             MaterialTheme.colorScheme.surface
                //         )
                //     )
                // )
                .background(MaterialTheme.colorScheme.surface) // O un color sólido
                .padding(horizontal = 24.dp) // Padding horizontal general
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()) // Permite scroll si el contenido es mucho
                    .imePadding(), // Añade padding cuando el teclado aparece
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // --- Logo de la App ---
                Image(
                    painter = painterResource(id = R.drawable.ppogo), // Tu logo
                    contentDescription = "Logo de la App Sanuvia",
                    modifier = Modifier
                        .size(120.dp) // Ajusta el tamaño según tu logo
                        .padding(bottom = 32.dp)
                        .clip(RoundedCornerShape(22.dp))
                )

                Text(
                    text = "Bienvenido de Nuevo",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Inicia sesión para continuar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                // --- Campo de Correo Electrónico ---
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        localError = null // Limpia error local al escribir
                        if (loginState is LoginState.Error) viewModel.resetLoginState()
                    },
                    label = { Text("Correo Electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp), // Bordes redondeados
                    leadingIcon = { // Icono al inicio del campo
                        Icon(Icons.Default.Email, contentDescription = "Icono de correo")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next // Acción para ir al siguiente campo
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    isError = localError?.contains("correo") == true || loginState is LoginState.Error
                )
                Spacer(modifier = Modifier.height(16.dp))

                // --- Campo de Contraseña ---
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        localError = null // Limpia error local al escribir
                        if (loginState is LoginState.Error) viewModel.resetLoginState()
                    },
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { // Icono al inicio
                        Icon(Icons.Default.Lock, contentDescription = "Icono de candado")
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done // Acción para finalizar
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide() // Oculta el teclado
                            if (email.isNotBlank() && password.isNotBlank()) {
                                viewModel.login(email, password)
                            } else {
                                localError = "Correo y contraseña son requeridos."
                            }
                        }
                    ),
                    trailingIcon = { // Icono para mostrar/ocultar contraseña
                        val image = if (passwordVisible)
                            Icons.Filled.VisibilityOff
                        else Icons.Filled.Visibility
                        val description = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, description)
                        }
                    },
                    singleLine = true,
                    isError = localError?.contains("contraseña") == true || loginState is LoginState.Error
                )
                Spacer(modifier = Modifier.height(8.dp))

                // --- Botón Olvidaste Contraseña (alineado a la derecha) ---
                TextButton(
                    onClick = {
                        if (email.isBlank()) {
                            localError = "Introduce tu correo para recuperar la contraseña."
                        } else {
                            viewModel.recoverPassword(email)
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier.align(Alignment.End) // Alinea a la derecha
                ) {
                    Text("¿Olvidaste tu contraseña?")
                }
                Spacer(modifier = Modifier.height(24.dp))

                // --- Botón Principal de Iniciar Sesión ---
                Button(
                    onClick = {
                        keyboardController?.hide() // Oculta el teclado
                        if (email.isBlank() || password.isBlank()) {
                            localError = "Por favor, ingresa correo y contraseña."
                        } else {
                            viewModel.login(email, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp), // Altura estándar para botones
                    shape = RoundedCornerShape(12.dp), // Bordes redondeados
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (loginState is LoginState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary // Color del indicador sobre el botón
                        )
                    } else {
                        Text("Iniciar Sesión", fontSize = 16.sp)
                    }
                }

                // --- Mensaje de Error (del ViewModel o local) ---
                val errorMessageToShow = when {
                    localError != null -> localError
                    loginState is LoginState.Error -> (loginState as LoginState.Error).message
                    else -> null
                }

                if (errorMessageToShow != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessageToShow,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // --- Feedback de Recuperación de Contraseña ---
                when (val state = passwordResetState) {
                    is PasswordResetState.Loading -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                    is PasswordResetState.Success -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Correo de recuperación enviado. Revisa tu bandeja de entrada.",
                            color = MaterialTheme.colorScheme.primary, // Color de éxito
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        // Limpia el estado después de un momento o cuando el usuario interactúe
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(4000) // Muestra por 4 segundos
                            viewModel.resetPasswordResetState()
                        }
                    }
                    is PasswordResetState.Error -> {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> Spacer(modifier = Modifier.height(24.dp)) // Espacio si no hay feedback
                }


                // --- Botón para Registrarse ---
                Row(
                    modifier = Modifier.padding(top = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "¿No tienes una cuenta?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onNavigateToRegister) {
                        Text(
                            "Regístrate",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } // Fin Column principal
        } // Fin Box principal
    } // Fin Scaffold
}

// --- Preview (Opcional, pero útil) ---
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun LoginScreenPreview() {
    // Envuelve en tu tema para que los colores y tipografía se apliquen
    // Asume que tienes un SanuviaTheme o similar
    // SanuviaTheme {
    MaterialTheme { // Usando MaterialTheme por defecto para el preview
        LoginScreen(
            onNavigateToRegister = {},
            onNavigateToHome = {}
            // Puedes pasar un ViewModel de prueba si es necesario para el estado inicial
        )
    }
}
