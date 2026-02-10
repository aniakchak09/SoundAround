package com.licenta.soundaround.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

//interface LastFmService {
//    @GET("2.0/?method=user.getrecenttracks&format=json&limit=1")
//    suspend fun getNowPlaying(
//        @Query("user") user: String,
//        @Query("api_key") apiKey: String
//    ): LastFmResponse
//}

object LastFmClient {
    private const val BASE_URL = "https://ws.audioscrobbler.com/"

    val service: LastFmService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LastFmService::class.java)
    }
}