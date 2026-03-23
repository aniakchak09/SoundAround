package licenta.soundaround.social.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import licenta.soundaround.social.domain.model.Conversation

@Serializable
data class ProfileRef(val username: String = "")

@Serializable
data class ExpiresAtDto(@SerialName("expires_at") val expiresAt: String? = null)

@Serializable
data class ReadStatusDto(
    @SerialName("user_one_id") val userOneId: String = "",
    @SerialName("user_one_last_read_at") val userOneLastReadAt: String? = null,
    @SerialName("user_two_last_read_at") val userTwoLastReadAt: String? = null
)

@Serializable
data class TypingStatusDto(
    @SerialName("user_one_id") val userOneId: String = "",
    @SerialName("user_one_typing_at") val userOneTypingAt: String? = null,
    @SerialName("user_two_typing_at") val userTwoTypingAt: String? = null
)

@Serializable
data class ConversationDto(
    val id: String = "",
    @SerialName("user_one_id") val userOneId: String = "",
    @SerialName("user_two_id") val userTwoId: String = "",
    @SerialName("user_one") val userOne: ProfileRef? = null,
    @SerialName("user_two") val userTwo: ProfileRef? = null,
    @SerialName("is_persistent") val isPersistent: Boolean = false,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("initial_track_title") val initialTrackTitle: String? = null,
    @SerialName("initial_track_artist") val initialTrackArtist: String? = null,
    @SerialName("my_initial_track_title") val myInitialTrackTitle: String? = null,
    @SerialName("my_initial_track_artist") val myInitialTrackArtist: String? = null,
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("last_message_content") val lastMessageContent: String? = null,
    @SerialName("user_one_last_read_at") val userOneLastReadAt: String? = null,
    @SerialName("user_two_last_read_at") val userTwoLastReadAt: String? = null
)

@Serializable
data class ConversationInsertDto(
    @SerialName("user_one_id") val userOneId: String,
    @SerialName("user_two_id") val userTwoId: String,
    @SerialName("is_persistent") val isPersistent: Boolean,
    @SerialName("expires_at") val expiresAt: String?,
    @SerialName("initial_track_title") val initialTrackTitle: String?,
    @SerialName("initial_track_artist") val initialTrackArtist: String?,
    @SerialName("my_initial_track_title") val myInitialTrackTitle: String?,
    @SerialName("my_initial_track_artist") val myInitialTrackArtist: String?,
    @SerialName("last_message_at") val lastMessageAt: String
)

fun ConversationDto.toDomain(currentUserId: String): Conversation {
    val isUserOne = userOneId == currentUserId
    val otherUserId = if (isUserOne) userTwoId else userOneId
    val otherUsername = if (isUserOne) userTwo?.username else userOne?.username
    val myTrackTitle = if (isUserOne) myInitialTrackTitle else initialTrackTitle
    val myTrackArtist = if (isUserOne) myInitialTrackArtist else initialTrackArtist
    val theirTrackTitle = if (isUserOne) initialTrackTitle else myInitialTrackTitle
    val theirTrackArtist = if (isUserOne) initialTrackArtist else myInitialTrackArtist
    val myLastReadAt = if (isUserOne) userOneLastReadAt else userTwoLastReadAt
    val isUnread = lastMessageAt != null && (myLastReadAt == null || lastMessageAt > myLastReadAt)
    return Conversation(
        id = id,
        otherUserId = otherUserId,
        otherUsername = otherUsername ?: otherUserId,
        isPersistent = isPersistent,
        expiresAt = expiresAt,
        myInitialTrackTitle = myTrackTitle,
        myInitialTrackArtist = myTrackArtist,
        theirInitialTrackTitle = theirTrackTitle,
        theirInitialTrackArtist = theirTrackArtist,
        lastMessageAt = lastMessageAt,
        lastMessageContent = lastMessageContent,
        isUnread = isUnread
    )
}
