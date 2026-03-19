package licenta.soundaround.music.data

import com.google.gson.annotations.SerializedName

data class TopArtistsResponse(val topartists: TopArtistsContainer)
data class TopArtistsContainer(val artist: List<TopArtistDto> = emptyList())
data class TopArtistDto(
    val name: String,
    val playcount: String,
    val image: List<LastFmImageDto> = emptyList()
)

data class TopTracksResponse(val toptracks: TopTracksContainer)
data class TopTracksContainer(val track: List<TopTrackDto> = emptyList())
data class TopTrackDto(
    val name: String,
    val artist: TopTrackArtistDto,
    val playcount: String,
    val image: List<LastFmImageDto> = emptyList()
)

data class TopTrackArtistDto(val name: String)
