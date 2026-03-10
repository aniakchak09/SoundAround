package licenta.soundaround.music.domain.model

data class Track(
    val title: String,
    val artist: String,
    val imageUrl: String,
    val isNowPlaying: Boolean
)