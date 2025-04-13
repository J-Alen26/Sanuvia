package com.uv.kooxi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.uv.kooxi.navigation.NavGraph
import com.uv.kooxi.ui.theme.KooxiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KooxiTheme {
                NavGraph() // Inicia con la pantalla de Login
            }
        }
    }
}