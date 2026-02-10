package com.licenta.soundaround.data.remote

import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://api.spotify.com/v1/"

    fun create(accessToken: String): SpotifyApi {

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request: Request = chain.request()
                    .newBuilder()
                    .addHeader(
                        "Authorization",
                        "Bearer $accessToken"
                    )
                    .build()

                chain.proceed(request)
            }
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(SpotifyApi::class.java)
    }
}
