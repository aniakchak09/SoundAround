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
import licenta.soundaround.core.LocationProvider
import licenta.soundaround.map.data.MapRepository
import licenta.soundaround.map.domain.model.UserLocation
import licenta.soundaround.music.domain.model.Track
import licenta.soundaround.music.domain.repository.MusicRepository
import licenta.soundaround.presence.data.PresenceRepository

class LastFmViewModel(
    private val repository: MusicRepository,
    private val authRepo: AuthRepository,
    private val presenceRepository: PresenceRepository,
    private val locationProvider: LocationProvider,
    private val mapRepository: MapRepository
) : ViewModel() {

    var trackInfo by mutableStateOf<Track?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var recentTracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var nearbyUsers by mutableStateOf<List<UserLocation>>(emptyList())
        private set
    var currentTrackTags by mutableStateOf<List<String>>(emptyList())
        private set

    val currentTrackRepeatCount: Int get() = trackInfo?.let { current ->
        recentTracks.count { !it.isNowPlaying && it.title == current.title && it.artist == current.artist }
    } ?: 0

    private var lastTagsFetchedArtist: String? = null


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

            isLoading = true
            fetchAndUpdate(username)
            isLoading = false

            launch { loadExtras(username) }

            while (true) {
                delay(20_000L)
                fetchAndUpdate(username)
            }
        }
    }

    private suspend fun loadExtras(username: String) {
        try {
            val recent = repository.getRecentTracks(username, 50)
            recentTracks = recent
            nearbyUsers = try {
                mapRepository.getActiveUsers()
                    .sortedByDescending { it.isPlaying }
                    .take(3)
            } catch (_: Exception) { emptyList() }
        } catch (e: Exception) {
            Log.e("LastFmViewModel", "Error loading extras: ${e.message}")
        }
    }

    private suspend fun fetchAndUpdate(username: String) {
        try {
            val result = repository.getCurrentTrack(username)
            if (result != null) {
                trackInfo = result
                errorMessage = null

                if (result.artist != lastTagsFetchedArtist) {
                    lastTagsFetchedArtist = result.artist
                    currentTrackTags = try {
                        repository.getArtistTopTags(result.artist)
                            .take(4)
                            .map { it.name }
                    } catch (_: Exception) { emptyList() }
                }

                val visibility = authRepo.getVisibilityMode()
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
}
