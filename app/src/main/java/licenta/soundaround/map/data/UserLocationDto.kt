package licenta.soundaround.map.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import licenta.soundaround.map.domain.model.UserLocation

@Serializable
data class LocationProfileRef(val username: String = "")

@Serializable
data class UserLocationDto(
    @SerialName("user_id") val userId: String,
    @SerialName("track_name") val trackName: String? = null,
    @SerialName("artist_name") val artistName: String? = null,
    @SerialName("album_art") val albumArt: String? = null,
    @SerialName("is_playing") val isPlaying: Boolean = false,
    val lat: Double? = null,
    val lng: Double? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null
)

fun UserLocationDto.toDomain(): UserLocation? {
    val lat = lat ?: return null
    val lng = lng ?: return null
    return UserLocation(
        userId = userId,
        username = null,
        lastFmUsername = null,
        trackName = trackName,
        artistName = artistName,
        albumArt = albumArt,
        isPlaying = isPlaying,
        lat = lat,
        lng = lng,
        lastSeenAt = lastSeenAt
    )
}
