package licenta.soundaround.music

import licenta.soundaround.music.data.ArtistTopTagsResponse
import licenta.soundaround.music.data.LastFmResponse
import licenta.soundaround.music.data.TopArtistsResponse
import licenta.soundaround.music.data.TopTracksResponse
import licenta.soundaround.music.data.UserInfoResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface LastFmService {
    @GET("?method=user.getrecenttracks&format=json")
    suspend fun getRecentTracks(
        @Query("user") username: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 1
    ): LastFmResponse

    @GET("?method=user.gettopartists&format=json")
    suspend fun getTopArtists(
        @Query("user") username: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 6,
        @Query("period") period: String = "overall"
    ): TopArtistsResponse

    @GET("?method=user.gettoptracks&format=json")
    suspend fun getTopTracks(
        @Query("user") username: String,
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 6,
        @Query("period") period: String = "overall"
    ): TopTracksResponse

    @GET("?method=artist.gettoptags&format=json&autocorrect=1")
    suspend fun getArtistTopTags(
        @Query("artist") artist: String,
        @Query("api_key") apiKey: String
    ): ArtistTopTagsResponse

    @GET("?method=user.getinfo&format=json")
    suspend fun getUserInfo(
        @Query("user") username: String,
        @Query("api_key") apiKey: String
    ): UserInfoResponse
}
