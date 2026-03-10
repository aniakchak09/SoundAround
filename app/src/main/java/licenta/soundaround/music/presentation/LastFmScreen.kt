package licenta.soundaround.music.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import licenta.soundaround.auth.data.AuthRepository

@Composable
fun LastFmScreen(
    viewModel: LastFmViewModel,
    authRepo: AuthRepository,
    onNavToProfile: () -> Unit,
    onSignOut: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Last.fm", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(20.dp))

        when {
            viewModel.isLoading -> {
                CircularProgressIndicator()
            }
            viewModel.trackInfo != null -> {
                val track = viewModel.trackInfo!!
                Card(modifier = Modifier.fillMaxWidth()) {
                    val prefix = if (track.isNowPlaying) "Now playing" else "Last played"
                    Text(
                        text = "$prefix: ${track.title} - ${track.artist}",
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    AsyncImage(
                        model = track.imageUrl,
                        contentDescription = "Album Art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(200.dp)
                            .align(Alignment.CenterHorizontally)
                            .padding(16.dp)
                    )
                }
                // Show a subtle refresh error below the card if polling failed
                viewModel.errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                Text(
                    text = viewModel.errorMessage
                        ?: "No track information available.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))


        Button(onClick = onNavToProfile) {
            Text("Edit profile")
        }

        Button(onClick = {
            scope.launch {
                authRepo.signOut()
                onSignOut()
            }
        }) {
            Text("Sign Out")
        }
    }
}