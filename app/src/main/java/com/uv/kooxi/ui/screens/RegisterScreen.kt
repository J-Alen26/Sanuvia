package com.uv.kooxi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf("") }

    // Instancia el ViewModel para el registro
    val viewModel: RegisterScreenViewModel = viewModel()
    val registrationState by viewModel.registrationState.collectAsState()

    // Navega a las pantallas cuando el registro es exitoso
    LaunchedEffect(registrationState) {
        if (registrationState is RegisterState.Success) {
            onNavigateToLogin()
            onNavigateToHome()
        }
    }

    // Determina el mensaje de error a mostrar: error local o error desde el ViewModel
    val errorMessageToShow = if (localError.isNotEmpty()) {
        localError
    } else if (registrationState is RegisterState.Error) {
        (registrationState as RegisterState.Error).message
    } else {
        ""
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Registro de Cuenta") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Campo para nombre de usuario
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    if (localError.isNotEmpty()) localError = ""
                },
                label = { Text("Nombre de Usuario") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Campo para email
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (localError.isNotEmpty()) localError = ""
                },
                label = { Text("Correo Electrónico") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Campo para contraseña
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (localError.isNotEmpty()) localError = ""
                },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Campo para confirmar contraseña
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    if (localError.isNotEmpty()) localError = ""
                },
                label = { Text("Confirmar Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                        localError = "Por favor, completa todos los campos."
                    } else if (password != confirmPassword) {
                        localError = "Las contraseñas no coinciden."
                    } else {
                        localError = ""
                        viewModel.register(email, password, username)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = registrationState !is RegisterState.Loading
            ) {
                if (registrationState is RegisterState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Registrarse")
                }
            }
            if (errorMessageToShow.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessageToShow,
                    color = if (registrationState is RegisterState.Success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onNavigateToLogin) {
                Text("¿Ya tienes una cuenta? Inicia Sesión")
            }
        }
    }
}