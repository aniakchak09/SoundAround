package licenta.soundaround.auth.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val bio: String? = null,
    val username: String = "",
    @SerialName("lastfm_username") val lastFmUsername: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)