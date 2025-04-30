package com.uv.sanuvia.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.collectAsState
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.asImageBitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import androidx.compose.material3.Button
import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
// import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    val user = FirebaseAuth.getInstance().currentUser
    // photoUrl es un Uri? que Firebase almacena en su perfil
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
                .build(),
            contentDescription = "Avatar de usuario",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    }
}



@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPagerApi::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    onLogout: () -> Unit = {},
    onEditProfile: () -> Unit = {}

) {
    val user = FirebaseAuth.getInstance().currentUser
    // photoUrl es un Uri? que Firebase almacena en su perfil
    val photoUri = user?.photoUrl

    // Usa Coil para cargar la imagen; si es null, muestra un placeholder local
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
                .build(),
            contentDescription = "Avatar de usuario",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    }

    val homeViewModel: HomeScreenViewModel = viewModel()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results: Map<String, Boolean> ->
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            homeViewModel.fetchLocation()
        } else {
            // manejo de permiso denegado
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }

    val location by homeViewModel.userLocation.collectAsState()
    LaunchedEffect(location) {
        location?.let {
            homeViewModel.consultarCultivosPorUbicacion()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Raíz y Vida") },
                actions = {
                    if (isLoggingOut) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(40.dp)
                                .padding(8.dp)
                        )
                    } else {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar perfil") },
                            onClick = {
                                menuExpanded = false
                                onEditProfile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cerrar sesión") },
                            onClick = {
                                menuExpanded = false
                                isLoggingOut = true
                                homeViewModel.logout()
                                onLogout()
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val pagerState = rememberPagerState(initialPage = 0)
        HorizontalPager(
            count = 3,
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            ProfileAvatar(modifier = Modifier.padding(16.dp))

                            Spacer(modifier = Modifier.height(16.dp))
                            // Mostrar la ubicación del usuario
                            location?.let {

                                Text(
                                    text = "Tu ubicación: lat = ${it.latitude}, lon = ${it.longitude}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                        }
                    }
                }
                1 -> {
                    // UI para escaneo de alimentos
                    val scanResult by homeViewModel.scanResult.collectAsState()
                    var selectedImage by remember { mutableStateOf<Bitmap?>(null) }
                    val takePictureLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.TakePicturePreview()
                    ) { bitmap: Bitmap? ->
                        selectedImage = bitmap
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))

                            // Preview de la imagen capturada
                            selectedImage?.let { bitmap ->
                                Spacer(modifier = Modifier.height(16.dp))
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(200.dp)
                                        .clip(CircleShape)
                                )
                            }

                            // Botón para escanear una vez tomada la foto
                            selectedImage?.let { bitmap ->
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = {
                                    // Convertir Bitmap a Base64 y disparar escaneo
                                    val stream = ByteArrayOutputStream()
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                    val imageBytes = stream.toByteArray()
                                    val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                                    homeViewModel.scanAlimento(base64)
                                }) {
                                    Text("Escanear alimento")
                                }
                            }

                            // Indicador de carga mientras llega resultado
                            if (selectedImage != null && scanResult == null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                CircularProgressIndicator()
                            }

                            // Mostrar resultado de IA
                            scanResult?.let { result ->
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = result,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        FloatingActionButton(
                            onClick = { takePictureLauncher.launch(null) },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Tomar foto")
                        }
                    }
                }

                2 -> {
                    val articles by homeViewModel.articleContent.collectAsState()
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (articles.isEmpty()) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                            } else {
                                articles.forEach { article ->
                                    Text(
                                        text = article.titulo,
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                    Text(
                                        text = article.descripcion,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}