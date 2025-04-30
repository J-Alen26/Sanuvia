package com.uv.sanuvia.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf("") }

    // Use the LoginScreenViewModel
    val viewModel: LoginScreenViewModel = viewModel()
    val loginState by viewModel.loginState.collectAsState()
    val passwordResetState by viewModel.passwordResetState.collectAsState()

    // Navigate to home if login is successful
    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            onNavigateToHome()
        }
    }

    // Determine which error message to show: local error vs. error from ViewModel
    val errorMessageToShow = if (localError.isNotEmpty()) {
        localError
    } else if (loginState is LoginState.Error) {
        (loginState as LoginState.Error).message
    } else {
        ""
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Inicio de Sesión") })
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
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (email.isEmpty() || password.isEmpty()) {
                        localError = "Por favor ingresa todos los campos."
                    } else {
                        viewModel.login(email, password)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (loginState is LoginState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Iniciar Sesión")
                }
            }
            if (errorMessageToShow.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessageToShow, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onNavigateToRegister) {
                Text("¿No tienes una cuenta? Regístrate")
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    if (email.isEmpty()) {
                        localError = "Por favor, introduce tu correo para recuperar la contraseña."
                    } else {
                        viewModel.recoverPassword(email)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("¿Olvidaste tu contraseña?")
            }

            when (passwordResetState) {
                is PasswordResetState.Loading -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                is PasswordResetState.Success -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Correo de recuperación enviado", color = MaterialTheme.colorScheme.primary)
                }
                is PasswordResetState.Error -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text((passwordResetState as PasswordResetState.Error).message, color = MaterialTheme.colorScheme.error)
                }
                else -> {}
            }
        }
    }
}