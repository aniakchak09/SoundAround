package licenta.soundaround.music.data

import com.google.gson.annotations.SerializedName

data class UserInfoResponse(val user: LastFmUserDto)
data class LastFmUserDto(
    val name: String = "",
    val playcount: String = "0",
    val image: List<LastFmImageDto> = emptyList(),
    val registered: LastFmRegisteredDto? = null
)
data class LastFmRegisteredDto(
    @SerializedName("#text") val timestamp: String = "0"
)
