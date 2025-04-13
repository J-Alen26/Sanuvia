package com.uv.kooxi.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.uv.kooxi.ui.screens.HomeScreen
import com.uv.kooxi.ui.screens.LoginScreen
import com.uv.kooxi.ui.screens.RegisterScreen
import com.uv.kooxi.ui.screens.UserEditScreen


@Composable
fun NavGraph(startDestination: String = "login") {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToHome = { navController.navigate("home") }
            )
        }
        composable("register") {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToHome = {navController.navigate("home")}
            )
        }
        composable("home") {
            HomeScreen(
                onLogout = {navController.navigate("login")},
                onEditProfile = {navController.navigate("editProfile")}
            )

        }
        composable("editProfile") {
            UserEditScreen(

            )
            
        }
    }
}