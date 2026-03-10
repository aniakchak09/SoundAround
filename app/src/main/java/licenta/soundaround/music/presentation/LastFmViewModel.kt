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
import licenta.soundaround.auth.domain.model.VisibilityMode
import licenta.soundaround.music.domain.model.Track
import licenta.soundaround.music.domain.repository.MusicRepository
import licenta.soundaround.presence.data.PresenceRepository

class LastFmViewModel(
    private val repository: MusicRepository,
    private val authRepo: AuthRepository,
    private val presenceRepository: PresenceRepository
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
            val profile = try {
                authRepo.getProfile()
            } catch (e: Exception) {
                Log.e("LastFmViewModel", "Failed to load profile: ${e.message}")
                null
            }

            val username = profile?.lastFmUsername
            if (username.isNullOrBlank()) {
                errorMessage = "No Last.fm username set. Please update your profile."
                return@launch
            }

            val visibility = authRepo.getVisibilityMode()

            isLoading = true
            fetchAndUpdate(username, visibility)
            isLoading = false

            while (true) {
                delay(20_000L)
                fetchAndUpdate(username, visibility)
            }
        }
    }

    private suspend fun fetchAndUpdate(username: String, visibility: VisibilityMode) {
        try {
            val result = repository.getCurrentTrack(username)
            if (result != null) {
                trackInfo = result
                errorMessage = null

                // Publish presence only if user is not invisible
                if (visibility != VisibilityMode.INVISIBLE) {
                    presenceRepository.publish(result)
                }
            }
        } catch (e: Exception) {
            Log.e("LastFmViewModel", "Error fetching track: ${e.message}")
            errorMessage = "Could not refresh. Showing last known track."
        }
    }
}
