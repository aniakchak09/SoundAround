package licenta.soundaround.social.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FriendRequestDto(
    @SerialName("user_id") val userId: String = "",
    @SerialName("friend_id") val friendId: String = "",
    val status: String = "pending"
)

@Serializable
data class FriendshipInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("friend_id") val friendId: String,
    val status: String = "pending"
)
