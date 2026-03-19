package licenta.soundaround.auth.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import licenta.soundaround.auth.data.AuthRepository
import licenta.soundaround.auth.data.ProfileDto
import licenta.soundaround.music.data.LastFmUserDto
import licenta.soundaround.music.data.TopArtistDto
import licenta.soundaround.music.data.TopTrackDto
import licenta.soundaround.music.domain.model.Track
import licenta.soundaround.music.domain.repository.MusicRepository
import licenta.soundaround.social.data.SocialRepository

class MyProfileViewModel(
    private val authRepo: AuthRepository,
    private val musicRepository: MusicRepository,
    private val socialRepository: SocialRepository
) : ViewModel() {

    var profile by mutableStateOf<ProfileDto?>(null)
        private set
    var lastFmInfo by mutableStateOf<LastFmUserDto?>(null)
        private set
    var topArtists by mutableStateOf<List<TopArtistDto>>(emptyList())
        private set
    var topTracks by mutableStateOf<List<TopTrackDto>>(emptyList())
        private set
    var recentTracks by mutableStateOf<List<Track>>(emptyList())
        private set
    var friends by mutableStateOf<List<Pair<String, String>>>(emptyList())
        private set
    var artistImages by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var trackImages by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var joinDate by mutableStateOf<String?>(null)
        private set

    init {
        load()
    }

    fun unfriend(userId: String) {
        viewModelScope.launch {
            socialRepository.unfriend(userId)
            friends = socialRepository.getFriends()
        }
    }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            val p = authRepo.getProfile()
            profile = p

            // Get join date from auth user
            val user = authRepo.getCurrentUser()
            joinDate = user?.createdAt?.toString()

            // Load friends
            launch { friends = socialRepository.getFriends() }

            val lastFm = p?.lastFmUsername?.takeIf { it.isNotBlank() }
            if (!lastFm.isNullOrBlank()) {
                launch { lastFmInfo = musicRepository.getUserInfo(lastFm) }
                launch {
                    val tracks = musicRepository.getTopTracks(lastFm)
                    topTracks = tracks
                    trackImages = tracks
                        .map { t -> async { "${t.name}_${t.artist.name}" to musicRepository.getTrackImageUrl(t.artist.name, t.name) } }
                        .awaitAll()
                        .mapNotNull { (key, url) -> url?.let { key to it } }
                        .toMap()
                }
                launch { recentTracks = musicRepository.getRecentTracks(lastFm, 10) }
                launch {
                    val artists = musicRepository.getTopArtists(lastFm)
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
