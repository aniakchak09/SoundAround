package com.licenta.soundaround.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
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

    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val service: LastFmService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // Add the client here
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LastFmService::class.java)
    }
}