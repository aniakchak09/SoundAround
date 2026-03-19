package licenta.soundaround.social.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import licenta.soundaround.social.domain.model.Conversation

@Serializable
data class ProfileRef(val username: String = "")

@Serializable
data class ExpiresAtDto(@SerialName("expires_at") val expiresAt: String? = null)

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
    @SerialName("last_message_at") val lastMessageAt: String? = null
)

@Serializable
data class ConversationInsertDto(
    @SerialName("user_one_id") val userOneId: String,
    @SerialName("user_two_id") val userTwoId: String,
    @SerialName("is_persistent") val isPersistent: Boolean,
    @SerialName("expires_at") val expiresAt: String?,
    @SerialName("initial_track_title") val initialTrackTitle: String?,
    @SerialName("initial_track_artist") val initialTrackArtist: String?,
    @SerialName("last_message_at") val lastMessageAt: String
)

fun ConversationDto.toDomain(currentUserId: String): Conversation {
    val isUserOne = userOneId == currentUserId
    val otherUserId = if (isUserOne) userTwoId else userOneId
    val otherUsername = if (isUserOne) userTwo?.username else userOne?.username
    return Conversation(
        id = id,
        otherUserId = otherUserId,
        otherUsername = otherUsername ?: otherUserId,
        isPersistent = isPersistent,
        expiresAt = expiresAt,
        initialTrackTitle = initialTrackTitle,
        initialTrackArtist = initialTrackArtist,
        lastMessageAt = lastMessageAt
    )
}
