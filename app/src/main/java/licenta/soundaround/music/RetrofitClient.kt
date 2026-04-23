package licenta.soundaround.music

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://ws.audioscrobbler.com/2.0/"

    fun createService(okHttpClient: OkHttpClient): LastFmService =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LastFmService::class.java)
}
