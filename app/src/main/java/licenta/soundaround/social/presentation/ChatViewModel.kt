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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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
    var expiresLabel by mutableStateOf<String?>(null)
        private set

    private val _toastMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val toastMessage: SharedFlow<String> = _toastMessage

    private val isoFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS+00:00",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss"
    ).map { pattern ->
        SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
    }

    init {
        startPolling()
        if (!initialIsPersistent) fetchAndTickExpiry()
    }

    private fun fetchAndTickExpiry() {
        viewModelScope.launch {
            val expiresAt = repository.getConversationExpiresAt(conversationId) ?: return@launch
            val expiryDate = isoFormats.firstNotNullOfOrNull { fmt ->
                runCatching { fmt.parse(expiresAt) }.getOrNull()
            } ?: return@launch
            while (true) {
                val secondsLeft = (expiryDate.time - Date().time) / 1000
                expiresLabel = formatTimeLeft(secondsLeft)
                if (secondsLeft <= 0) break
                delay(1_000L)
            }
        }
    }

    private fun formatTimeLeft(seconds: Long): String {
        if (seconds <= 0) return "Chat expired"
        val d = seconds / 86400
        val h = (seconds % 86400) / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return when {
            d > 0 -> "Expires in ${d}d ${h}h"
            h > 0 -> "Expires in ${h}h ${m}m"
            m > 0 -> "Expires in ${m}m ${s}s"
            else  -> "Expires in ${s}s"
        }
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
