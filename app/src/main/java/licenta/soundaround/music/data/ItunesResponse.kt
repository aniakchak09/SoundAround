package licenta.soundaround.music.data

import com.google.gson.annotations.SerializedName

data class ItunesResponse(
    val resultCount: Int,
    val results: List<ItunesTrackDto>
)

data class ItunesTrackDto(
    val trackName: String?,
    val artistName: String?,
    @SerializedName("previewUrl") val previewUrl: String?
)
