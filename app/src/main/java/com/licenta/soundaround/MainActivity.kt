package com.licenta.soundaround

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.licenta.soundaround.api.LastFmClient
import com.licenta.soundaround.auth.SpotifyAuthManager
import com.licenta.soundaround.ui.main.MainViewModel
import com.licenta.soundaround.ui.theme.SoundAroundTheme
import com.spotify.sdk.android.auth.AuthorizationClient
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authManager = SpotifyAuthManager(this)
        viewModel = MainViewModel(authManager)

        // In your MainActivity.kt, update the setContent block:

        setContent {
            SoundAroundTheme {
                // 1. Define the state for the username
                var username by remember { mutableStateOf("") }
                var trackInfo by remember { mutableStateOf("No track synced yet") }
                val scope = rememberCoroutineScope()


                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {

                        // 2. The input field for the username
                        TextField(
                            value = username,
                            onValueChange = { username = it }, // This updates the 'username' variable
                            label = { Text("Last.fm Username") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. The Sync Button
                        Button(
                            onClick = {
                                Log.d("LASTFM", "Syncing music for user: $username")
                                // Here you will call your ViewModel to fetch the song
                                scope.launch {
                                    try {
                                        // REPLACE "YOUR_API_KEY" with the key from Last.fm dashboard
                                        val response = LastFmClient.service.getNowPlaying(username, "bb981c5f4ae7e23af12fd342bbe690be")
                                        val track = response.recenttracks.track.firstOrNull()

                                        if (track?.attr?.nowplaying == "true") {
                                            trackInfo = "Playing: ${track.name} by ${track.artist.name}"
                                        } else {
                                            trackInfo = "Not currently listening."
                                        }
                                    } catch (e: Exception) {
                                        trackInfo = "Error: ${e.message}"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Sync My Music")
                        }

                        Text(
                            text = trackInfo,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }

        handleSpotifyRedirect(intent)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        viewModel.handleAuthResult(
            requestCode,
            resultCode,
            data,
            onCodeReceived = { code, verifier ->
                Log.d("AUTH", "Code: $code")
                Log.d("AUTH", "Verifier: $verifier")
                // next: send to backend
            },
            onError = { error ->
                Log.e("AUTH", error)
            }
        )

        Log.d("AUTH_DEBUG", "Result received! Code: $requestCode")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSpotifyRedirect(intent)
    }

    private fun handleSpotifyRedirect(intent: Intent?) {
        val uri = intent?.data
        if (uri != null && uri.toString().startsWith("soundaround://callback")) {
            // Use the SDK's response parser on the intent data
            val response = AuthorizationClient.getResponse(Activity.RESULT_OK, intent)

            // This is where you call your manager's logic
            viewModel.handleAuthResult(
                SpotifyAuthManager.AUTH_REQUEST_CODE, // Force the code match
                Activity.RESULT_OK,
                intent,
                onCodeReceived = { code, verifier ->
                    Log.d("AUTH_SUCCESS", "Code: $code")
                },
                onError = { Log.e("AUTH_ERROR", it) }
            )
        }
    }
}
