package com.emon.proxagallery.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.emon.proxagallery.ui.theme.ProxaGalleryTheme

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val factory = remember(context) { GalleryViewModelFactory(context.applicationContext) }
    val viewModel: GalleryViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        modifier = modifier,
        uiState = uiState,
        onPhotosAccessGranted = viewModel::loadPhotos
    )
}

@Composable
private fun HomeScreenContent(
    uiState: GalleryUiState,
    onPhotosAccessGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_MEDIA_IMAGES
    ) == PackageManager.PERMISSION_GRANTED
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onPhotosAccessGranted()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Proxa Gallery",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Search your memories, not your folders.",
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(50.dp))
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search photos...") }
        )
        when {
            uiState.isLoading -> CircularProgressIndicator()
            uiState.error != null -> Text(uiState.error)
        }
        Text(text = "${uiState.photos.size} photos loaded")
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.height(240.dp)
        ) {
            items(uiState.photos, key = { photo -> photo.id }) { photo ->
                AsyncImage(
                    model = photo.uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Gray)
                )
            }
        }
        Button(
            onClick = {
                if (hasPermission) {
                    onPhotosAccessGranted()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
        ) {
            Text("Open Gallery")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    ProxaGalleryTheme {
        HomeScreenContent(
            uiState = GalleryUiState(isLoading = false),
            onPhotosAccessGranted = {}
        )
    }
}
