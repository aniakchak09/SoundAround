package licenta.soundaround.social.data

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import licenta.soundaround.core.SupabaseConfig
import licenta.soundaround.social.domain.model.Conversation
import licenta.soundaround.social.domain.model.FriendRequest
import licenta.soundaround.social.domain.model.Message
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SocialRepository {
    private val client = SupabaseConfig.client
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private fun currentUserId() = client.auth.currentUserOrNull()?.id

    private suspend fun cleanupExpiredConversations() {
        try {
            val now = isoFormat.format(Date())
            client.from("conversations").delete {
                filter {
                    lt("expires_at", now)
                    eq("is_persistent", false)
                }
            }
        } catch (e: Exception) {
            Log.e("SocialRepository", "Cleanup failed: ${e.message}")
        }
    }

    suspend fun sendPing(toUserId: String, trackTitle: String?, trackArtist: String?): String? {
        return try {
            val fromUserId = currentUserId() ?: return null
            val now = isoFormat.format(Date())
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.add(Calendar.HOUR_OF_DAY, 24)
            val expiresAt = isoFormat.format(cal.time)

            val result = client.from("conversations")
                .insert(
                    ConversationInsertDto(
                        userOneId = fromUserId,
                        userTwoId = toUserId,
                        isPersistent = false,
                        expiresAt = expiresAt,
                        initialTrackTitle = trackTitle,
                        initialTrackArtist = trackArtist,
                        lastMessageAt = now
                    )
                ) { select() }
                .decodeSingle<ConversationDto>()

            result.id
        } catch (e: Exception) {
            Log.e("SocialRepository", "sendPing failed: ${e.message}")
            null
        }
    }

    suspend fun loadConversations(): List<Conversation> {
        return try {
            cleanupExpiredConversations()
            val userId = currentUserId() ?: return emptyList()

            client.from("conversations")
                .select(
                    Columns.raw(
                        "*, user_one:profiles!conversations_user_one_id_fkey(username), user_two:profiles!conversations_user_two_id_fkey(username)"
                    )
                ) {
                    filter {
                        or {
                            eq("user_one_id", userId)
                            eq("user_two_id", userId)
                        }
                    }
                }
                .decodeList<ConversationDto>()
                .sortedByDescending { it.lastMessageAt }
                .map { it.toDomain(userId) }
        } catch (e: Exception) {
            Log.e("SocialRepository", "loadConversations failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun loadMessages(conversationId: String): List<Message> {
        return try {
            client.from("messages")
                .select {
                    filter { eq("conversation_id", conversationId) }
                }
                .decodeList<MessageDto>()
                .sortedBy { it.sentAt }
                .map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("SocialRepository", "loadMessages failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun sendMessage(conversationId: String, content: String): Boolean {
        return try {
            val senderId = currentUserId() ?: return false
            client.from("messages").insert(
                MessageInsertDto(
                    conversationId = conversationId,
                    senderId = senderId,
                    content = content
                )
            )
            true
        } catch (e: Exception) {
            Log.e("SocialRepository", "sendMessage failed: ${e.message}")
            false
        }
    }

    suspend fun makePersistent(conversationId: String): Boolean {
        return try {
            client.from("conversations").update({
                set("is_persistent", true)
                set("expires_at", null as String?)
            }) {
                filter { eq("id", conversationId) }
            }
            true
        } catch (e: Exception) {
            Log.e("SocialRepository", "makePersistent failed: ${e.message}")
            false
        }
    }

    suspend fun sendFriendRequest(toUserId: String, conversationId: String): Boolean {
        return try {
            val fromUserId = currentUserId() ?: return false
            client.from("friendships").insert(
                FriendshipInsertDto(userId = fromUserId, friendId = toUserId)
            )
            // Keep the conversation alive while waiting for acceptance
            makePersistent(conversationId)
            true
        } catch (e: Exception) {
            Log.e("SocialRepository", "sendFriendRequest failed: ${e.message}")
            false
        }
    }

    suspend fun getPendingRequests(): List<FriendRequest> {
        return try {
            val userId = currentUserId() ?: return emptyList()
            client.from("friendships")
                .select { filter { eq("friend_id", userId); eq("status", "pending") } }
                .decodeList<FriendRequestDto>()
                .map { dto ->
                    val username = getUsernameForId(dto.userId)
                    FriendRequest(fromUserId = dto.userId, fromUsername = username ?: dto.userId)
                }
        } catch (e: Exception) {
            Log.e("SocialRepository", "getPendingRequests failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun acceptFriendRequest(fromUserId: String): Boolean {
        return try {
            val myId = currentUserId() ?: return false
            client.from("friendships").update({ set("status", "accepted") }) {
                filter { eq("user_id", fromUserId); eq("friend_id", myId) }
            }
            // Make all conversations between the two users persistent
            for ((one, two) in listOf(fromUserId to myId, myId to fromUserId)) {
                try {
                    client.from("conversations").update({
                        set("is_persistent", true)
                        set("expires_at", null as String?)
                    }) {
                        filter { eq("user_one_id", one); eq("user_two_id", two) }
                    }
                } catch (_: Exception) {}
            }
            true
        } catch (e: Exception) {
            Log.e("SocialRepository", "acceptFriendRequest failed: ${e.message}")
            false
        }
    }

    suspend fun declineFriendRequest(fromUserId: String): Boolean {
        return try {
            val myId = currentUserId() ?: return false
            client.from("friendships").delete {
                filter { eq("user_id", fromUserId); eq("friend_id", myId) }
            }
            true
        } catch (e: Exception) {
            Log.e("SocialRepository", "declineFriendRequest failed: ${e.message}")
            false
        }
    }

    private suspend fun getUsernameForId(userId: String): String? {
        return try {
            client.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<ProfileRef>()
                ?.username
                ?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}
