package com.uv.sanuvia.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.uv.sanuvia.ui.screens.HomeScreen
import com.uv.sanuvia.ui.screens.LoginScreen
import com.uv.sanuvia.ui.screens.RegisterScreen
import com.uv.sanuvia.ui.screens.UserEditScreen


@Composable
fun NavGraph(isUserLoggedIn: Boolean) {
    val navController = rememberNavController()
    val startDestination = if (isUserLoggedIn) "home" else "login"
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
                onNavigateToLogin = {navController.navigate("login")}

            )
            
        }
    }
}