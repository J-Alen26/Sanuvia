package com.uv.sanuvia.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel // Para obtener ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.uv.sanuvia.ui.screens.HomeScreen
import com.uv.sanuvia.ui.screens.LoginScreen
import com.uv.sanuvia.ui.screens.RegisterScreen
import com.uv.sanuvia.ui.screens.UserEditScreen
import com.uv.sanuvia.ui.screens.HomeScreenViewModel
import com.uv.sanuvia.ui.screens.ArticuloDetalleScreen
import com.uv.sanuvia.ui.screens.CultivoDetalleScreen



// Constantes para los argumentos de navegación para evitar errores de tipeo
const val ARG_ARTICULO_ID = "articuloId"
const val ARG_CULTIVO_NOMBRE = "cultivoNombre"

@Composable
fun NavGraph(isUserLoggedIn: Boolean) {
    val navController = rememberNavController()
    // Define el destino inicial basado en el estado de login del usuario
    val startDestination = if (isUserLoggedIn) NavScreen.Home.route else NavScreen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(NavScreen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(NavScreen.Register.route) },
                onNavigateToHome = {
                    navController.navigate(NavScreen.Home.route) {
                        popUpTo(NavScreen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(NavScreen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigate(NavScreen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(NavScreen.Home.route) {
            // Se obtiene la instancia del ViewModel aquí.
            // Puede ser compartida por composables anidados si tienen el mismo NavBackStackEntry como owner.
            val homeViewModel: HomeScreenViewModel = viewModel()
            HomeScreen(
                homeViewModel = homeViewModel, // Pasa la instancia del ViewModel
                onLogout = {
                    navController.navigate(NavScreen.Login.route) {
                        popUpTo(NavScreen.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onEditProfile = { navController.navigate(NavScreen.EditProfile.route) },
                onNavigateToArticuloDetail = { articuloId ->
                    // Navega a la ruta de detalle del artículo, pasando el articuloId.
                    // Es importante que articuloId sea seguro para URLs.
                    navController.navigate("${NavScreen.ArticuloDetail.routePrefix}/$articuloId")
                },
                // --- LAMBDA PARA NAVEGAR AL DETALLE DEL CULTIVO ---
                onNavigateToCultivoDetail = { cultivoNombre ->
                    // Navega a la ruta de detalle del cultivo, pasando el nombre del cultivo.
                    // Considera URL-encoding para cultivoNombre si puede contener caracteres especiales.
                    navController.navigate("${NavScreen.CultivoDetail.routePrefix}/$cultivoNombre")
                }
                // El modifier se puede omitir si no necesitas uno específico aquí,
                // o pasar Modifier si es necesario desde un llamador superior.
                // modifier = Modifier.fillMaxSize() // Ejemplo
            )
        }
        composable(NavScreen.EditProfile.route) {
            UserEditScreen(
                onNavigateToLogin = {
                    navController.navigate(NavScreen.Login.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToHome = { navController.popBackStack() } // Regresa a la pantalla anterior (Home)
            )
        }

        // --- RUTA PARA LA PANTALLA DE DETALLE DEL ARTÍCULO ---
        composable(
            route = NavScreen.ArticuloDetail.route, // Usa la ruta definida en NavScreen
            arguments = listOf(navArgument(ARG_ARTICULO_ID) { type = NavType.StringType })
        ) { backStackEntry ->
            val articuloId = backStackEntry.arguments?.getString(ARG_ARTICULO_ID)

            // Obtiene el ViewModel que tiene la lista de artículos.
            // Para arquitecturas más grandes, ArticuloDetalleScreen podría tener su propio ViewModel.
            val homeViewModel: HomeScreenViewModel = viewModel(
                // Considera el ViewModelStoreOwner correcto si necesitas compartir la instancia exacta
                // de HomeScreen. Por defecto, esto puede crear una nueva o reusar una si el scope es el mismo.
            )
            val uiState by homeViewModel.state.collectAsState()

            if (articuloId != null) {
                // Busca el artículo en la lista del uiState.
                // Cambia 'it.titulo' por 'it.id' si tienes un campo ID más fiable.
                val articuloSeleccionado = uiState.articles.find { it.titulo == articuloId }

                if (articuloSeleccionado != null) {
                    ArticuloDetalleScreen(
                        articulo = articuloSeleccionado,
                        onNavigateBack = { navController.popBackStack() }
                    )
                } else {
                    ErrorScreen(message = "Artículo no encontrado (ID: $articuloId).")
                }
            } else {
                ErrorScreen(message = "ID de artículo no válido.")
            }
        }

        // --- NUEVA RUTA PARA LA PANTALLA DE DETALLE DEL CULTIVO ---
        composable(
            route = NavScreen.CultivoDetail.route, // Usa la ruta definida en NavScreen
            arguments = listOf(navArgument(ARG_CULTIVO_NOMBRE) { type = NavType.StringType })
        ) { backStackEntry ->
            val cultivoNombre = backStackEntry.arguments?.getString(ARG_CULTIVO_NOMBRE)

            // Obtiene el ViewModel que tiene la lista de cultivos.
            val homeViewModel: HomeScreenViewModel = viewModel()
            val uiState by homeViewModel.state.collectAsState()

            if (cultivoNombre != null) {
                // Busca el cultivo en la lista del uiState usando el nombre.
                val cultivoSeleccionado = uiState.cultivos.find { it.nombre == cultivoNombre }

                if (cultivoSeleccionado != null) {
                    CultivoDetalleScreen( // Asume que tienes este Composable
                        cultivo = cultivoSeleccionado,
                        onNavigateBack = { navController.popBackStack() }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                ErrorScreen(message = "Nombre de cultivo no válido.")
            }
        }
    }
}

// Define tus rutas como un sealed class para mejor organización y evitar errores de tipeo
sealed class NavScreen(val route: String, val routePrefix: String? = null) {
    object Login : NavScreen("login")
    object Register : NavScreen("register")
    object Home : NavScreen("home")
    object EditProfile : NavScreen("editProfile")
    object ArticuloDetail : NavScreen("articulo_detalle/{$ARG_ARTICULO_ID}", "articulo_detalle")
    object CultivoDetail : NavScreen("cultivo_detalle/{$ARG_CULTIVO_NOMBRE}", "cultivo_detalle")
    // Puedes añadir más pantallas aquí
}

// Un Composable simple para mostrar errores de navegación
@Composable
fun ErrorScreen(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message)
    }
}
