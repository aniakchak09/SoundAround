package com.licenta.soundaround.data.remote

import com.licenta.soundaround.data.model.SpotifyCodeRequest
import com.licenta.soundaround.data.model.SpotifyTokenResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface SpotifyAuthApi {

    @POST("spotify/token")
    suspend fun exchangeCode(
        @Body request: SpotifyCodeRequest
    ): SpotifyTokenResponse
}
