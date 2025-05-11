package com.uv.sanuvia.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle // Para nombre de usuario
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
import com.uv.sanuvia.R // Asegúrate que R se importa para el drawable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit, // Aunque el registro exitoso ahora va a Login
    viewModel: RegisterScreenViewModel = viewModel()
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val registrationState by viewModel.registrationState.collectAsState()

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Efecto para manejar el resultado del registro
    LaunchedEffect(registrationState) {
        when (val state = registrationState) {
            is RegisterState.Success -> {
                // Navega a Login después de un registro exitoso para que el usuario inicie sesión
                onNavigateToLogin()
                viewModel.resetRegistrationState() // Limpia el estado
            }
            is RegisterState.Error -> {
                // El error se mostrará en el Text, no es necesario navegar aquí
                // Podrías querer limpiar el error del VM después de un tiempo
                // kotlinx.coroutines.delay(4000)
                // viewModel.resetRegistrationState()
            }
            else -> Unit // Idle, Loading
        }
    }
    // Limpia error local si el estado del VM cambia
    LaunchedEffect(registrationState) {
        if (registrationState !is RegisterState.Idle) {
            localError = null
        }
    }

    Scaffold(
        // Sin TopAppBar para un look más limpio
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .imePadding(), // Ajusta el padding cuando el teclado aparece
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // --- Logo de la App ---
                Image(
                    painter = painterResource(id = R.drawable.ppogo), // Tu logo
                    contentDescription = "Logo de Sanuvia",
                    modifier = Modifier
                        .size(100.dp) // Un poco más pequeño que en Login
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(14.dp))
                )

                Text(
                    text = "Crea tu Cuenta",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Únete a Sanuvia.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // --- Campo de Nombre de Usuario ---
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        localError = null
                        if (registrationState is RegisterState.Error) viewModel.resetRegistrationState()
                    },
                    label = { Text("Nombre de Usuario") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Icono de usuario")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    isError = localError?.contains("nombre", ignoreCase = true) == true || registrationState is RegisterState.Error
                )
                Spacer(modifier = Modifier.height(16.dp))

                // --- Campo de Correo Electrónico ---
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        localError = null
                        if (registrationState is RegisterState.Error) viewModel.resetRegistrationState()
                    },
                    label = { Text("Correo Electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = "Icono de correo")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    isError = localError?.contains("correo", ignoreCase = true) == true || registrationState is RegisterState.Error
                )
                Spacer(modifier = Modifier.height(16.dp))

                // --- Campo de Contraseña ---
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        localError = null
                        if (registrationState is RegisterState.Error) viewModel.resetRegistrationState()
                    },
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = "Icono de candado")
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, if (passwordVisible) "Ocultar" else "Mostrar")
                        }
                    },
                    singleLine = true,
                    isError = localError?.contains("contraseña", ignoreCase = true) == true ||
                            localError?.contains("coinciden", ignoreCase = true) == true ||
                            registrationState is RegisterState.Error
                )
                Spacer(modifier = Modifier.height(16.dp))

                // --- Campo de Confirmar Contraseña ---
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        localError = null
                        if (registrationState is RegisterState.Error) viewModel.resetRegistrationState()
                    },
                    label = { Text("Confirmar Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = "Icono de candado")
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            // Lógica de validación y registro aquí
                            if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                                localError = "Por favor, completa todos los campos."
                            } else if (password != confirmPassword) {
                                localError = "Las contraseñas no coinciden."
                            } else {
                                viewModel.register(email, password, username)
                            }
                        }
                    ),
                    trailingIcon = {
                        val image = if (confirmPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(imageVector = image, if (confirmPasswordVisible) "Ocultar" else "Mostrar")
                        }
                    },
                    singleLine = true,
                    isError = localError?.contains("coinciden", ignoreCase = true) == true || registrationState is RegisterState.Error
                )
                Spacer(modifier = Modifier.height(24.dp))

                // --- Botón de Registrarse ---
                Button(
                    onClick = {
                        keyboardController?.hide()
                        if (username.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                            localError = "Por favor, completa todos los campos."
                        } else if (password != confirmPassword) {
                            localError = "Las contraseñas no coinciden."
                        } else {
                            viewModel.register(email, password, username)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = registrationState !is RegisterState.Loading, // Deshabilitado mientras carga
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 6.dp)
                ) {
                    if (registrationState is RegisterState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Registrarse", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // --- Mensaje de Error ---
                val errorMessageToShow = when {
                    localError != null -> localError
                    registrationState is RegisterState.Error -> (registrationState as RegisterState.Error).message
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
                // Espacio reservado para que el layout no salte si no hay error
                if (errorMessageToShow == null && registrationState !is RegisterState.Loading) {
                    Spacer(modifier = Modifier.height(24.dp + 12.dp)) // Altura aproximada del texto de error + spacer
                }


                // --- Navegación a Iniciar Sesión ---
                Row(
                    modifier = Modifier.padding(top = 32.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "¿Ya tienes una cuenta?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onNavigateToLogin) {
                        Text(
                            "Inicia Sesión",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } // Fin Column principal
        } // Fin Box principal
    } // Fin Scaffold
}

// --- Preview ---
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun RegisterScreenPreview() {
    MaterialTheme {
        RegisterScreen(
            onNavigateToLogin = {},
            onNavigateToHome = {}
        )
    }
}
