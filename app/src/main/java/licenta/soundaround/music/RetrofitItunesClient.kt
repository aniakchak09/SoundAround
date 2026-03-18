package licenta.soundaround.music

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitItunesClient {
    private const val BASE_URL = "https://itunes.apple.com/"

    val itunesService: ItunesService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ItunesService::class.java)
    }
}
