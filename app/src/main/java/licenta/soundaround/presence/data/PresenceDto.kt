package licenta.soundaround.presence.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PresenceDto(
    @SerialName("user_id") val userId: String,
    @SerialName("track_name") val trackName: String,
    @SerialName("artist_name") val artistName: String,
    @SerialName("album_art") val albumArt: String,
    @SerialName("is_playing") val isPlaying: Boolean,
    @SerialName("synced_at") val syncedAt: String,
    val lat: Double? = null,
    val lng: Double? = null
)
