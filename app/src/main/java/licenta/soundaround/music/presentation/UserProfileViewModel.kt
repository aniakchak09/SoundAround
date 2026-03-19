package licenta.soundaround.music.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import licenta.soundaround.map.data.MapRepository
import licenta.soundaround.music.data.TopArtistDto
import licenta.soundaround.music.data.TopTrackDto
import licenta.soundaround.music.domain.model.Track
import licenta.soundaround.music.domain.repository.MusicRepository

class UserProfileViewModel(
    private val userId: String,
    val displayUsername: String,
    private val mapRepository: MapRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    var bio by mutableStateOf<String?>(null)
        private set
    var lastFmUsername by mutableStateOf<String?>(null)
        private set
    var topArtists by mutableStateOf<List<TopArtistDto>>(emptyList())
        private set
    var topTracks by mutableStateOf<List<TopTrackDto>>(emptyList())
        private set
    var recentTracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var artistImages by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var trackImages by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var isLoading by mutableStateOf(true)
        private set

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            isLoading = true
            val (profileBio, profileLastFm) = mapRepository.getUserProfileDetails(userId)
            bio = profileBio
            lastFmUsername = profileLastFm

            if (!profileLastFm.isNullOrBlank()) {
                launch {
                    val tracks = musicRepository.getTopTracks(profileLastFm)
                    topTracks = tracks
                    trackImages = tracks
                        .map { t -> async { "${t.name}_${t.artist.name}" to musicRepository.getTrackImageUrl(t.artist.name, t.name) } }
                        .awaitAll()
                        .mapNotNull { (key, url) -> url?.let { key to it } }
                        .toMap()
                }
                launch { recentTracks = musicRepository.getRecentTracks(profileLastFm, 10) }
                launch {
                    val artists = musicRepository.getTopArtists(profileLastFm)
                    topArtists = artists
                    artistImages = artists
                        .map { artist -> async { artist.name to musicRepository.getArtistImageUrl(artist.name) } }
                        .awaitAll()
                        .mapNotNull { (name, url) -> url?.let { name to it } }
                        .toMap()
                }
            }
            isLoading = false
        }
    }
}
