package licenta.soundaround.music

import licenta.soundaround.music.data.LastFmResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface LastFmService {
    @GET("?method=user.getrecenttracks&format=json")
    suspend fun getRecentTracks(
        @Query("user") username: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 1 // limit=1 gives us the "Now Playing" track
    ): LastFmResponse // This is a wrapper for your LastFmTrack list
}