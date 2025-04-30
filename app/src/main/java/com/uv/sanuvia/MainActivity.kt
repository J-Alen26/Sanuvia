package com.uv.sanuvia
import com.google.firebase.auth.FirebaseAuth

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.widget.Toast
import com.uv.sanuvia.navigation.NavGraph
import com.uv.sanuvia.ui.theme.KooxiTheme

class MainActivity : ComponentActivity() {
    private val locationPermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                Toast.makeText(this, "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Solicita permisos de ubicación usando el nuevo Activity Result API
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        // Determina si ya hay sesión activa
        val isUserLoggedIn = FirebaseAuth.getInstance().currentUser != null
        setContent {
            KooxiTheme {
                NavGraph(isUserLoggedIn = isUserLoggedIn)
            }
        }
    }
}