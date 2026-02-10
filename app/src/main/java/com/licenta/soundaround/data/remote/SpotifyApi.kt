package com.licenta.soundaround.data.remote

import com.licenta.soundaround.data.model.CurrentlyPlayingResponse
import com.licenta.soundaround.data.model.TopArtistsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SpotifyApi {

    /**
     * Get the track the user is currently listening to
     * Requires scope: user-read-currently-playing
     */
    @GET("me/player/currently-playing")
    suspend fun getCurrentlyPlaying(): Response<CurrentlyPlayingResponse>

    /**
     * Get user's top artists
     * Requires scope: user-top-read
     */
    @GET("me/top/artists")
    suspend fun getTopArtists(
        @Query("limit") limit: Int = 10
    ): TopArtistsResponse
}
