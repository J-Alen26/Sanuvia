package com.uv.sanuvia.ui.screens // O el paquete correcto de tu UI

import android.Manifest
import android.content.Context
import android.net.Uri // Importa Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn // Para la lista de escaneos
import androidx.compose.foundation.lazy.items // Para la lista de escaneos
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Article // Icono para artículos
import androidx.compose.material.icons.filled.CameraAlt // Icono para tomar foto
import androidx.compose.material.icons.filled.LocationOn // Icono para ubicación
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.PagerState // Import PagerState
import com.google.accompanist.pager.rememberPagerState
import com.google.firebase.auth.FirebaseAuth
import com.uv.sanuvia.BuildConfig // Necesario para authority del FileProvider
// Asegúrate que la ruta a tu ViewModel y Modelo sean correctas
import com.uv.sanuvia.data.repository.EscaneoAlimento
import com.uv.sanuvia.ui.screens.HomeScreenViewModel // O la ruta correcta
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Objects
import kotlinx.coroutines.CoroutineScope // Para el scope en PagerIndicator
import kotlinx.coroutines.launch // Para la coroutine de scrollToPage


// --- ProfileAvatar Composable ---
@Composable
fun ProfileAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp // Ajustado tamaño para TopAppBar
) {
    val user = FirebaseAuth.getInstance().currentUser
    val photoUri = user?.photoUrl

    if (photoUri == null) {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Avatar placeholder",
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(photoUri)
                .crossfade(true)
                // Añade placeholders/error para mejor UX
                // .placeholder(R.drawable.ic_avatar_placeholder)
                // .error(R.drawable.ic_avatar_placeholder)
                .build(),
            contentDescription = "Avatar de usuario",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    }
}

// --- Función auxiliar para crear Uri temporal ---
fun Context.createImageUri(): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val imageFileName = "JPEG_${timeStamp}_"
    val storageDir = File(cacheDir, "images") // Guarda en directorio cache/images/
    storageDir.mkdirs() // Crea el directorio si no existe
    val imageFile = File.createTempFile(
        imageFileName, /* prefix */
        ".jpg", /* suffix */
        storageDir      /* directory */
    )
    // IMPORTANTE: Asegúrate que las authorities coincidan con tu AndroidManifest.xml
    return FileProvider.getUriForFile(this, "${BuildConfig.APPLICATION_ID}.provider", imageFile)
}


// --- Pantalla Principal ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeScreenViewModel = viewModel(), // Inyecta o usa viewModel()
    onLogout: () -> Unit = {},
    onEditProfile: () -> Unit = {}
) {
    val context = LocalContext.current
    // Observa el estado completo del ViewModel
    val uiState by homeViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // --- Launchers para permisos y cámara ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results: Map<String, Boolean> ->
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            Log.d("HomeScreen", "Permiso ubicación concedido, obteniendo ubicación...")
            homeViewModel.fetchLocation()
        } else {
            Log.w("HomeScreen", "Permiso ubicación denegado.")
            // TODO: Mostrar mensaje al usuario indicando por qué se necesita el permiso
        }
    }

    // Launcher para la cámara que guarda en un Uri
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempImageUri?.let { uri ->
                Log.d("HomeScreen", "Foto tomada exitosamente: $uri")
                homeViewModel.procesarImagenAlimento(uri) // Llama al ViewModel con el Uri
            } ?: Log.e("HomeScreen", "Error: Uri temporal era null después de tomar foto")
        } else {
            Log.e("HomeScreen", "Error al tomar la foto o el usuario canceló")
            // Opcional: mostrar mensaje de error si falla la captura
        }
    }

    // --- Efectos ---
    // Solicitar permisos al iniciar (si no se han concedido)
    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Mostrar Snackbar cuando haya un error de escaneo
    LaunchedEffect(uiState.scanError) {
        uiState.scanError?.let { errorMsg ->
            snackbarHostState.showSnackbar(
                message = errorMsg,
                duration = SnackbarDuration.Short
            )
            homeViewModel.limpiarErrorEscaneo() // Limpia el error después de mostrarlo
        }
    }
    // Mostrar Snackbar para error de ubicación (opcional)
    LaunchedEffect(uiState.locationError) {
        uiState.locationError?.let { errorMsg ->
            snackbarHostState.showSnackbar(message = errorMsg, duration = SnackbarDuration.Short)
            // Podrías añadir viewModel.limpiarErrorUbicacion()
        }
    }


    // --- UI Principal ---
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Añadir SnackbarHost
        topBar = {
            TopAppBar(
                title = { Text("Sanuvia") }, // Reemplaza con tu título
                actions = {
                    // Usamos el ProfileAvatar composable
                    IconButton(onClick = { menuExpanded = true }) {
                        ProfileAvatar() // Usa tu composable
                    }
                    // Menú desplegable
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar perfil") },
                            onClick = {
                                menuExpanded = false
                                onEditProfile() // Navega a la pantalla de edición
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cerrar sesión") },
                            onClick = {
                                menuExpanded = false
                                // Considera mostrar un dialogo de confirmación aquí
                                homeViewModel.logout() // Llama al logout del ViewModel
                                onLogout()       // Ejecuta la acción de navegación/limpieza
                            }
                        )
                    }
                }
            )
        }
        // Podrías añadir un bottomBar si prefieres el indicador ahí
        // bottomBar = { PagerIndicator(...) }
    ) { innerPadding ->

        // Estado del Pager y Coroutine Scope para navegación
        val pagerState = rememberPagerState(initialPage = 1) // Iniciar en página de escaneo (index 1)
        val scope = rememberCoroutineScope() // Necesario para scrollToPage

        // Columna principal para alojar Pager y el Indicador debajo
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Aplica padding del Scaffold
        ) {

            // --- Indicador de Página ---
            PagerIndicator(
                pagerState = pagerState,
                pageCount = 3, // Debe coincidir con el count del Pager
                modifier = Modifier
                    .align(Alignment.CenterHorizontally) // Centra la fila de iconos
                    .padding(vertical = 8.dp) // Espacio arriba y abajo
                // .navigationBarsPadding() // Padding opcional para barras de sistema inferiores
                // .imePadding() // Padding opcional si hay teclados
            ) { pageIndex -> // Lambda para navegar al hacer clic en icono (opcional)
                // Navega a la página clicada usando una coroutine
                scope.launch {
                    pagerState.scrollToPage(pageIndex)
                }
            }
            // --- Fin Indicador de Página ---
            // El Pager ocupa el espacio restante
            HorizontalPager(
                count = 3, // Número de páginas
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Hace que el Pager ocupe todo el espacio vertical disponible
            )

            { page ->
                // --- Contenido de cada página (sin cambios internos) ---
                when (page) {
                    // --- Página 0: Ubicación / Otra Info ---
                    0 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Información Ubicación", style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(16.dp))

                            if (uiState.isLocationLoading) {
                                CircularProgressIndicator()
                            } else {
                                uiState.userLocation?.let {
                                    Text("Ubicación actual:")
                                    Text("Lat: ${it.latitude}")
                                    Text("Lon: ${it.longitude}")
                                } ?: run {
                                    Text("Ubicación no disponible.")
                                    uiState.locationError?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
                                    Button(onClick = { homeViewModel.fetchLocation() }) {
                                        Text("Reintentar obtener ubicación")
                                    }
                                }
                                // Aquí podrías mostrar los resultados de consultarCultivosPorUbicacion
                            }
                        } // Fin Column Página 0
                    } // Fin Página 0

                    // --- Página 1: Escaneo de Alimentos ---
                    1 -> {
                        Box(modifier = Modifier.fillMaxSize()) { // Box para posicionar FAB
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(0.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Mis Escaneos", style = MaterialTheme.typography.headlineSmall)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Indicador de carga general del proceso de escaneo
                                if (uiState.isScanning) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Procesando imagen...")
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // Lista de escaneos previos
                                if (uiState.isScanListLoading && uiState.misEscaneos.isEmpty()) {
                                    CircularProgressIndicator() // Indicador si está cargando la lista inicial
                                } else if (uiState.misEscaneos.isEmpty() && !uiState.isScanListLoading) {
                                    Text("Aún no has realizado ningún escaneo.")
                                } else {
                                    LazyColumn(modifier = Modifier.weight(1f)) { // LazyColumn para eficiencia
                                        items(uiState.misEscaneos) { escaneo ->
                                            EscaneoItem(escaneo = escaneo) // Composable para mostrar cada item
                                            Divider()
                                        }
                                    }
                                }
                            } // Fin Column Página 1

                            // FAB para iniciar la cámara
                            FloatingActionButton(
                                onClick = {
                                    // Crea un Uri temporal ANTES de lanzar la cámara
                                    val uri = context.createImageUri()
                                    tempImageUri = uri // Guarda el Uri temporalmente
                                    takePictureLauncher.launch(uri) // Lanza la cámara para guardar en ese Uri
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp) // Ajusta padding si usas BottomBar
                            ) {
                                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Tomar foto alimento")
                            }
                        } // Fin Box Página 1
                    } // Fin Página 1

                    // --- Página 2: Artículos / Enfermedades ---
                    2 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text("Artículos de Salud", style = MaterialTheme.typography.headlineSmall)
                            Spacer(modifier = Modifier.height(16.dp))

                            if (uiState.articles.isEmpty()) {
                                Text("No hay artículos disponibles.")
                            } else {
                                uiState.articles.forEach { article ->
                                    Text(
                                        text = article.titulo,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    Text(
                                        text = article.descripcion,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        } // Fin Column Página 2
                    } // Fin Página 2
                } // Fin when(page)
            } // Fin HorizontalPager


        } // Fin Column Principal
    } // Fin Scaffold
} // Fin HomeScreen

// --- Composable para el Indicador de Página ---
@OptIn(ExperimentalPagerApi::class) // Necesario para pagerState
@Composable
fun PagerIndicator(
    pagerState: PagerState, // Asegúrate que el tipo es correcto (viene de Accompanist)
    pageCount: Int,
    modifier: Modifier = Modifier,
    onIconClick: (Int) -> Unit // Lambda para manejar clics
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Define los iconos para cada página
        val icons = listOf(
            Icons.Filled.LocationOn to "Ubicación", // Icono y descripción para página 0
            Icons.Filled.CameraAlt to "Escanear",    // Icono y descripción para página 1
            Icons.Filled.Article to "Artículos"      // Icono y descripción para página 2
        )

        repeat(pageCount) { index ->
            val isSelected = pagerState.currentPage == index
            val (icon, contentDescription) = icons[index]

            IconButton(onClick = { onIconClick(index) }) { // Llama a la lambda al hacer clic
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription, // Accesibilidad
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary // Color cuando está seleccionada
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant // Color cuando no está seleccionada
                    }
                )
            }
        }
    }
}


// --- Composable auxiliar para mostrar un item de escaneo ---
@Composable
fun EscaneoItem(escaneo: EscaneoAlimento, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(escaneo.urlFoto)
                .crossfade(true)
                // .placeholder(R.drawable.placeholder_image) // Añade placeholders
                // .error(R.drawable.error_image)
                .build(),
            contentDescription = "Imagen escaneada ${escaneo.idEscaneo}", // Descripción más útil
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape) // O usa RoundedCornerShape
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = escaneo.resultado, // Descripción de la IA
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3 // Limita las líneas si es necesario
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Formatear la fecha de forma más amigable
            val formattedDate = remember(escaneo.fechaHora) {
                escaneo.fechaHora?.toDate()?.let {
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it)
                } ?: "Fecha desconocida"
            }
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}


// --- Preview ---
@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    // Para el preview, necesitarías un ViewModel falso o estado inicial
    // Es difícil previsualizar esta pantalla completa sin datos reales.
    MaterialTheme { // Envuelve en un tema para que funcionen los colores
        HomeScreen()
    }
}