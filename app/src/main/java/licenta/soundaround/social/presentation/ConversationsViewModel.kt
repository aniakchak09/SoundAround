package licenta.soundaround.social.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import licenta.soundaround.core.toUserMessage
import licenta.soundaround.social.data.SocialRepository
import licenta.soundaround.social.domain.model.Conversation
import licenta.soundaround.social.domain.model.FriendRequest

class ConversationsViewModel(private val repository: SocialRepository) : ViewModel() {

    var conversations by mutableStateOf<List<Conversation>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var pendingRequests by mutableStateOf<List<FriendRequest>>(emptyList())
        private set

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<String> = _toastMessage

    fun load() {
        viewModelScope.launch {
            isLoading = true
            try {
                conversations = repository.loadConversations()
                pendingRequests = repository.getPendingRequests()
            } catch (e: Exception) {
                _toastMessage.tryEmit(e.toUserMessage())
            }
            isLoading = false
        }
    }

    fun acceptRequest(fromUserId: String) {
        viewModelScope.launch {
            if (repository.acceptFriendRequest(fromUserId)) {
                _toastMessage.tryEmit("Friend added!")
                load()
            } else {
                _toastMessage.tryEmit("Failed to accept. Try again.")
            }
        }
    }

    fun declineRequest(fromUserId: String) {
        viewModelScope.launch {
            if (repository.declineFriendRequest(fromUserId)) {
                pendingRequests = pendingRequests.filter { it.fromUserId != fromUserId }
            } else {
                _toastMessage.tryEmit("Failed to decline. Try again.")
            }
        }
    }
}
