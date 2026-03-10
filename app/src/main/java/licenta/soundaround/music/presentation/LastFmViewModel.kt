package licenta.soundaround.music.presentation

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import licenta.soundaround.auth.data.AuthRepository
import licenta.soundaround.music.data.MusicRepositoryImpl
import licenta.soundaround.music.domain.model.Track

class LastFmViewModel(
    private val repository: MusicRepositoryImpl,
    private val authRepo: AuthRepository
) : ViewModel() {

    var trackInfo by mutableStateOf<Track?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            // Read username once
            val username = try {
                authRepo.getProfile()?.lastFmUsername
            } catch (e: Exception) {
                Log.e("LastFmViewModel", "Failed to load profile: ${e.message}")
                null
            }

            if (username.isNullOrBlank()) {
                errorMessage = "No Last.fm username set. Please update your profile."
                return@launch
            }

            // First fetch — show loading indicator only on initial load
            isLoading = true
            fetchAndUpdate(username)
            isLoading = false

            // Periodic polling — never wipes the displayed track on failure
            while (true) {
                delay(20_000L)
                fetchAndUpdate(username)
            }
        }
    }

    private suspend fun fetchAndUpdate(username: String) {
        try {
            val result = repository.getCurrentTrack(username)
            if (result != null) {
                trackInfo = result
                errorMessage = null
            }
            // If result is null (nothing returned), keep the last known track visible
        } catch (e: Exception) {
            Log.e("LastFmViewModel", "Error fetching track: ${e.message}")
            // Keep last known track; just surface a subtle error
            errorMessage = "Could not refresh. Showing last known track."
        }
    }
}