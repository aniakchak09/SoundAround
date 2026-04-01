package licenta.soundaround.map.domain.model

data class UserLocation(
    val userId: String,
    val username: String?,
    val lastFmUsername: String?,
    val trackName: String?,
    val artistName: String?,
    val albumArt: String?,
    val isPlaying: Boolean,
    val lat: Double,
    val lng: Double,
    val lastSeenAt: String?
)
