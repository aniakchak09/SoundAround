package licenta.soundaround.music.data

import android.media.Image
import com.google.gson.annotations.SerializedName
import java.util.jar.Attributes

// Data class representing a track from the Last.fm API response
data class LastFmTrackDto(
    val name: String,
    val artist: LastFmArtistDto,
    val image: List<LastFmImageDto>,
    @SerializedName("@attr") val attributes: LastFmAttributesDto?
)

data class LastFmArtistDto(
    @SerializedName("#text") val name: String
)

data class LastFmImageDto(
    @SerializedName("#text") val url: String,
    val size: String // "small", "medium", "large", "extralarge"
)

data class LastFmAttributesDto(
    val nowplaying: String // Last.fm returns this as a string "true"
)