package licenta.soundaround.map.presentation

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

class MapViewModel(
    private val repository: MapRepository,
    private val locationProvider: LocationProvider
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
        selectedUser = user
        // Lazy-load username if not already present
        if (user.username == null) {
            viewModelScope.launch {
                val username = repository.getUsernameForId(user.userId)
                if (username != null) {
                    selectedUser = user.copy(username = username)
                }
            }
        }
    }

    fun dismissUser() {
        selectedUser = null
    }
}
