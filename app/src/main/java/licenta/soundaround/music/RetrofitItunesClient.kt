package licenta.soundaround.music

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitItunesClient {
    private const val BASE_URL = "https://itunes.apple.com/"

    fun createService(okHttpClient: OkHttpClient): ItunesService =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ItunesService::class.java)
}
