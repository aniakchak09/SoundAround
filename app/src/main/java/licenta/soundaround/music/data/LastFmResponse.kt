package licenta.soundaround.music.data

data class LastFmResponse(
    val recenttracks: RecentTracksContainer
)

data class RecentTracksContainer(
    val track: List<LastFmTrackDto>
)