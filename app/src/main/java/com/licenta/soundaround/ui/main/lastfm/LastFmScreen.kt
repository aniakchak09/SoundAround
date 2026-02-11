package com.licenta.soundaround.ui.main.lastfm

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@SuppressLint("UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastFmScreen(
    viewModel: LastFmViewModel = viewModel(),
    onBack: () -> Unit
) {
    // Remembered state that controls which section is visible: "nowPlaying" or "recentTracks" (empty = none)
    var function by remember { mutableStateOf("nowPlaying") }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Last.fm Now Playing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            TextField(
                value = viewModel.username,
                onValueChange = { viewModel.username = it },
                label = { Text("Last.fm Username") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    viewModel.fetchNowPlaying()
                    // when fetching now playing, show now playing section
                    function = "nowPlaying"
                          },
                enabled = !viewModel.isLoading,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(if (viewModel.isLoading) "Loading..." else "Fetch Now Playing")
            }

            Button(
                onClick = {
                    viewModel.fetchRecentTracks()
                    // when fetching recent tracks, show recent tracks section
                    function = "recentTracks"
                          },
                enabled = !viewModel.isLoading,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(if (viewModel.isLoading) "Loading..." else "Fetch Recent Tracks")
            }

            LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
                // Show Now Playing only when function == "nowPlaying"
                if (function == "nowPlaying") {
                    viewModel.nowPLaying?.let { track ->
                        item {
                            // clicking the now playing item toggles the nowPlaying section (hide if already visible)
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .clickable { function = if (function == "nowPlaying") "" else "nowPlaying" }
                                .padding(8.dp)) {
                                // show album art if available
                                track.albumArtUrl?.let { url ->
                                    AsyncImage(
                                        model = url,
                                        contentDescription = "Album art",
                                        modifier = Modifier.size(96.dp).padding(end = 8.dp)
                                    )
                                }

                                Column {
                                    Text("Now Playing:", style = MaterialTheme.typography.titleSmall)
                                    Text("${track.artist} - ${track.title}", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }

                // Show Recent Tracks only when function == "recentTracks"
                if (function == "recentTracks") {
                    items(viewModel.recentTracks) { track ->
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .clickable { function = if (function == "recentTracks") "" else "recentTracks" }
                            .padding(8.dp)) {
                            Text("${track.artist} - ${track.title}", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
             }
         }
     }
 }
