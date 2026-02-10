package com.licenta.soundaround.data.repository

import com.licenta.soundaround.data.remote.SpotifyApi
import retrofit2.Response

class SpotifyRepository(
    private val spotifyApi: SpotifyApi
) {

    suspend fun getCurrentlyPlaying() =
        spotifyApi.getCurrentlyPlaying()

    suspend fun getTopArtists() =
        spotifyApi.getTopArtists()
}
