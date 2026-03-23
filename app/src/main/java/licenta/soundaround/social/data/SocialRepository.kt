package licenta.soundaround.social.data

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import licenta.soundaround.auth.data.ProfileDto
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

    suspend fun sendPing(
        toUserId: String,
        trackTitle: String?,
        trackArtist: String?,
        myTrackTitle: String? = null,
        myTrackArtist: String? = null
    ): String? {
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
                        myInitialTrackTitle = myTrackTitle,
                        myInitialTrackArtist = myTrackArtist,
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

    suspend fun sendMessage(conversationId: String, content: String, isPersistent: Boolean): Boolean {
        return try {
            val senderId = currentUserId() ?: return false
            val now = isoFormat.format(Date())
            client.from("messages").insert(
                MessageInsertDto(
                    conversationId = conversationId,
                    senderId = senderId,
                    content = content
                )
            )
            // Update last_message_at (and expires_at for temp chats) — essential for sorting
            try {
                if (!isPersistent) {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    cal.add(Calendar.HOUR_OF_DAY, 24)
                    client.from("conversations").update({
                        set("last_message_at", now)
                        set("expires_at", isoFormat.format(cal.time))
                    }) { filter { eq("id", conversationId) } }
                } else {
                    client.from("conversations").update({
                        set("last_message_at", now)
                    }) { filter { eq("id", conversationId) } }
                }
            } catch (e: Exception) {
                Log.e("SocialRepository", "sendMessage timestamp update failed: ${e.message}")
            }
            // Update preview text separately — optional, may fail if column not migrated yet
            try {
                client.from("conversations").update({
                    set("last_message_content", content)
                }) { filter { eq("id", conversationId) } }
            } catch (e: Exception) {
                Log.e("SocialRepository", "sendMessage content update failed: ${e.message}")
            }
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
            // Extend expiry by 7 days to keep conversation alive while request is pending,
            // but don't make it persistent — that only happens after acceptance
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.add(Calendar.DAY_OF_YEAR, 7)
            val newExpiry = isoFormat.format(cal.time)
            client.from("conversations").update({
                set("expires_at", newExpiry)
            }) {
                filter { eq("id", conversationId) }
            }
            true
        } catch (e: Exception) {
            Log.e("SocialRepository", "sendFriendRequest failed: ${e.message}")
            false
        }
    }

    suspend fun findConversationWith(userId: String): Conversation? {
        return try {
            val myId = currentUserId() ?: return null
            cleanupExpiredConversations()
            client.from("conversations")
                .select(
                    Columns.raw(
                        "*, user_one:profiles!conversations_user_one_id_fkey(username), user_two:profiles!conversations_user_two_id_fkey(username)"
                    )
                ) {
                    filter {
                        or {
                            eq("user_one_id", myId)
                            eq("user_two_id", myId)
                        }
                    }
                }
                .decodeList<ConversationDto>()
                .filter {
                    (it.userOneId == myId && it.userTwoId == userId) ||
                    (it.userOneId == userId && it.userTwoId == myId)
                }
                .maxByOrNull { it.lastMessageAt ?: "" }
                ?.toDomain(myId)
        } catch (e: Exception) {
            Log.e("SocialRepository", "findConversationWith failed: ${e.message}")
            null
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

    suspend fun markRead(conversationId: String) {
        val userId = currentUserId() ?: return
        val now = isoFormat.format(Date())
        // One of these will match (current user is either user_one or user_two)
        try {
            client.from("conversations").update({ set("user_one_last_read_at", now) }) {
                filter { eq("id", conversationId); eq("user_one_id", userId) }
            }
        } catch (_: Exception) {}
        try {
            client.from("conversations").update({ set("user_two_last_read_at", now) }) {
                filter { eq("id", conversationId); eq("user_two_id", userId) }
            }
        } catch (_: Exception) {}
    }

    suspend fun setTyping(conversationId: String, isTyping: Boolean) {
        val userId = currentUserId() ?: return
        val value = if (isTyping) isoFormat.format(Date()) else null
        try {
            client.from("conversations").update({ set("user_one_typing_at", value) }) {
                filter { eq("id", conversationId); eq("user_one_id", userId) }
            }
        } catch (_: Exception) {}
        try {
            client.from("conversations").update({ set("user_two_typing_at", value) }) {
                filter { eq("id", conversationId); eq("user_two_id", userId) }
            }
        } catch (_: Exception) {}
    }

    suspend fun getOtherIsTyping(conversationId: String): Boolean {
        val userId = currentUserId() ?: return false
        return try {
            val dto = client.from("conversations")
                .select(Columns.raw("user_one_id, user_one_typing_at, user_two_typing_at")) {
                    filter { eq("id", conversationId) }
                }
                .decodeSingleOrNull<TypingStatusDto>() ?: return false
            val theirTypingAt = (if (dto.userOneId == userId) dto.userTwoTypingAt else dto.userOneTypingAt)
                ?: return false
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val clean = theirTypingAt.substringBefore("+").trimEnd('Z').substringBefore(".")
            val typingMs = sdf.parse(clean)?.time ?: return false
            System.currentTimeMillis() - typingMs < 5_000L
        } catch (_: Exception) { false }
    }

    suspend fun blockUser(blockedId: String): Boolean {
        return try {
            val myId = currentUserId() ?: return false
            client.from("blocks").insert(mapOf("blocker_id" to myId, "blocked_id" to blockedId))
            true
        } catch (e: Exception) {
            Log.e("SocialRepository", "blockUser failed: ${e.message}")
            false
        }
    }

    suspend fun reportUser(reportedId: String, reason: String): Boolean {
        return try {
            val myId = currentUserId() ?: return false
            client.from("reports").insert(
                mapOf("reporter_id" to myId, "reported_id" to reportedId, "reason" to reason)
            )
            true
        } catch (e: Exception) {
            Log.e("SocialRepository", "reportUser failed: ${e.message}")
            false
        }
    }

    suspend fun getOtherLastReadAt(conversationId: String): String? {
        val userId = currentUserId() ?: return null
        return try {
            val dto = client.from("conversations")
                .select(Columns.raw("user_one_id, user_one_last_read_at, user_two_last_read_at")) {
                    filter { eq("id", conversationId) }
                }
                .decodeSingleOrNull<ReadStatusDto>() ?: return null
            if (dto.userOneId == userId) dto.userTwoLastReadAt else dto.userOneLastReadAt
        } catch (e: Exception) { null }
    }

    suspend fun getConversationExpiresAt(conversationId: String): String? {
        return try {
            client.from("conversations")
                .select(Columns.raw("expires_at")) {
                    filter { eq("id", conversationId) }
                }
                .decodeSingleOrNull<ExpiresAtDto>()
                ?.expiresAt
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getFriendIds(userId: String): Set<String> {
        return try {
            val asSender = client.from("friendships")
                .select(Columns.raw("friend_id")) {
                    filter { eq("user_id", userId); eq("status", "accepted") }
                }
                .decodeList<FriendRequestDto>()
                .map { it.friendId }

            val asReceiver = client.from("friendships")
                .select(Columns.raw("user_id")) {
                    filter { eq("friend_id", userId); eq("status", "accepted") }
                }
                .decodeList<FriendRequestDto>()
                .map { it.userId }

            (asSender + asReceiver).toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    suspend fun unfriend(friendId: String): Boolean {
        return try {
            val myId = currentUserId() ?: return false
            // Delete both directions (user_id=me,friend_id=them) and (user_id=them,friend_id=me)
            client.from("friendships").delete {
                filter {
                    or {
                        and {
                            eq("user_id", myId)
                            eq("friend_id", friendId)
                        }
                        and {
                            eq("user_id", friendId)
                            eq("friend_id", myId)
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e("SocialRepository", "unfriend failed: ${e.message}")
            false
        }
    }

    suspend fun getFriends(): List<Pair<String, String>> {
        return try {
            val userId = currentUserId() ?: return emptyList()
            val friendIds = getFriendIds(userId).toList()
            if (friendIds.isEmpty()) return emptyList()
            client.from("profiles")
                .select(Columns.raw("id,username")) {
                    filter { isIn("id", friendIds) }
                }
                .decodeList<ProfileDto>()
                .map { it.id to (it.username.takeIf { u -> u.isNotBlank() } ?: it.id) }
        } catch (e: Exception) {
            Log.e("SocialRepository", "getFriends failed: ${e.message}")
            emptyList()
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
