package licenta.soundaround.social.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import licenta.soundaround.core.toUserMessage
import licenta.soundaround.social.data.SocialRepository
import licenta.soundaround.social.domain.model.Message

class ChatViewModel(
    private val repository: SocialRepository,
    val conversationId: String,
    private val currentUserId: String,
    private val otherUserId: String,
    initialIsPersistent: Boolean = false
) : ViewModel() {

    var messages by mutableStateOf<List<Message>>(emptyList())
        private set
    var isSending by mutableStateOf(false)
        private set
    var isPersistent by mutableStateOf(initialIsPersistent)
        private set
    var friendRequestSent by mutableStateOf(false)
        private set

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<String> = _toastMessage

    init {
        startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                try {
                    messages = repository.loadMessages(conversationId)
                } catch (e: Exception) {
                    _toastMessage.tryEmit(e.toUserMessage())
                }
                delay(3_000L)
            }
        }
    }

    fun sendMessage(content: String) {
        viewModelScope.launch {
            isSending = true
            val ok = repository.sendMessage(conversationId, content)
            if (ok) {
                try {
                    messages = repository.loadMessages(conversationId)
                } catch (e: Exception) {
                    _toastMessage.tryEmit(e.toUserMessage())
                }
            } else {
                _toastMessage.tryEmit("Failed to send message. Check your connection.")
            }
            isSending = false
        }
    }

    fun sendFriendRequest() {
        viewModelScope.launch {
            if (repository.sendFriendRequest(otherUserId, conversationId)) {
                friendRequestSent = true
                _toastMessage.tryEmit("Friend request sent!")
            } else {
                _toastMessage.tryEmit("Failed to send friend request. Try again.")
            }
        }
    }

    fun isMyMessage(message: Message) = message.senderId == currentUserId
}
