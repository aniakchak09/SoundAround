package licenta.soundaround.music.presentation

import android.media.MediaPlayer
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
import licenta.soundaround.core.LocationProvider
import licenta.soundaround.music.domain.model.Track
import licenta.soundaround.music.domain.repository.MusicRepository
import licenta.soundaround.presence.data.PresenceRepository

class LastFmViewModel(
    private val repository: MusicRepository,
    private val authRepo: AuthRepository,
    private val presenceRepository: PresenceRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    var trackInfo by mutableStateOf<Track?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    var previewUrl by mutableStateOf<String?>(null)
        private set
    var isPreviewPlaying by mutableStateOf(false)
        private set
    var isPreviewLoading by mutableStateOf(false)
        private set

    private var mediaPlayer: MediaPlayer? = null
    private var lastPreviewTrackKey: String? = null

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
                val trackKey = "${result.artist}|${result.title}"
                if (trackKey != lastPreviewTrackKey) {
                    lastPreviewTrackKey = trackKey
                    stopPreview()
                    previewUrl = null
                    fetchPreviewUrl(result.artist, result.title)
                }
                trackInfo = result
                errorMessage = null

                if (visibility != VisibilityMode.INVISIBLE) {
                    val location = locationProvider.getLastLocation()
                    presenceRepository.publish(result, location?.first, location?.second)
                }
            }
        } catch (e: Exception) {
            Log.e("LastFmViewModel", "Error fetching track: ${e.message}")
            errorMessage = "Could not refresh. Showing last known track."
        }
    }

    private suspend fun fetchPreviewUrl(artist: String, track: String) {
        isPreviewLoading = true
        previewUrl = try {
            repository.getPreviewUrl(artist, track)
        } catch (e: Exception) {
            Log.e("LastFmViewModel", "Error fetching preview: ${e.message}")
            null
        }
        isPreviewLoading = false
    }

    fun togglePreview() {
        val url = previewUrl ?: return
        if (isPreviewPlaying) {
            stopPreview()
        } else {
            startPreview(url)
        }
    }

    private fun startPreview(url: String) {
        stopPreview()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(url)
            setOnPreparedListener {
                start()
                isPreviewPlaying = true
            }
            setOnCompletionListener {
                isPreviewPlaying = false
            }
            setOnErrorListener { _, _, _ ->
                isPreviewPlaying = false
                true
            }
            prepareAsync()
        }
    }

    private fun stopPreview() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
        isPreviewPlaying = false
    }

    override fun onCleared() {
        super.onCleared()
        stopPreview()
    }
}
