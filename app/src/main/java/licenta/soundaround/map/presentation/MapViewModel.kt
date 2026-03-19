package licenta.soundaround.map.presentation

import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.auth
import licenta.soundaround.core.LocationProvider
import licenta.soundaround.core.SupabaseConfig
import licenta.soundaround.core.toUserMessage
import licenta.soundaround.map.data.MapRepository
import licenta.soundaround.map.domain.model.UserLocation
import licenta.soundaround.music.domain.repository.MusicRepository
import licenta.soundaround.social.data.SocialRepository
import licenta.soundaround.social.domain.model.Conversation

class MapViewModel(
    private val repository: MapRepository,
    private val locationProvider: LocationProvider,
    private val musicRepository: MusicRepository,
    private val socialRepository: SocialRepository
) : ViewModel() {

    var users by mutableStateOf<List<UserLocation>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var selectedUser by mutableStateOf<UserLocation?>(null)
        private set
    var ownLocation by mutableStateOf<Pair<Double, Double>?>(null)
        private set
    val currentUserId: String? = SupabaseConfig.client.auth.currentUserOrNull()?.id

    var existingConversation by mutableStateOf<Conversation?>(null)
        private set

    var previewUrl by mutableStateOf<String?>(null)
        private set
    var isPreviewLoading by mutableStateOf(false)
        private set
    var isPreviewPlaying by mutableStateOf(false)
        private set

    private var mediaPlayer: MediaPlayer? = null

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<String> = _toastMessage

    init {
        fetchOwnLocation()
        startRefreshing()
    }

    private fun fetchOwnLocation() {
        viewModelScope.launch {
            ownLocation = locationProvider.getLastLocation()
        }
    }

    private fun startRefreshing() {
        viewModelScope.launch {
            isLoading = true
            try {
                users = repository.getActiveUsers()
            } catch (e: Exception) {
                _toastMessage.tryEmit(e.toUserMessage())
            }
            isLoading = false

            while (true) {
                delay(30_000L)
                try {
                    users = repository.getActiveUsers()
                } catch (e: Exception) {
                    _toastMessage.tryEmit(e.toUserMessage())
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                users = repository.getActiveUsers()
            } catch (e: Exception) {
                _toastMessage.tryEmit(e.toUserMessage())
            }
            ownLocation = locationProvider.getLastLocation()
        }
    }

    fun selectUser(user: UserLocation) {
        stopPreview()
        previewUrl = null
        existingConversation = null
        selectedUser = user

        viewModelScope.launch {
            if (user.username == null) {
                val username = repository.getUsernameForId(user.userId)
                if (username != null) {
                    selectedUser = user.copy(username = username)
                }
            }
            existingConversation = socialRepository.findConversationWith(user.userId)
        }

        val artist = user.artistName
        val track = user.trackName
        if (!artist.isNullOrBlank() && !track.isNullOrBlank()) {
            viewModelScope.launch {
                isPreviewLoading = true
                previewUrl = try {
                    musicRepository.getPreviewUrl(artist, track)
                } catch (e: Exception) {
                    Log.e("MapViewModel", "Preview fetch failed: ${e.message}")
                    null
                }
                isPreviewLoading = false
            }
        }
    }

    fun dismissUser() {
        stopPreview()
        previewUrl = null
        existingConversation = null
        selectedUser = null
    }

    fun togglePreview() {
        val url = previewUrl ?: return
        if (isPreviewPlaying) stopPreview() else startPreview(url)
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
