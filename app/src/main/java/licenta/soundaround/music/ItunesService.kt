package licenta.soundaround.music

import licenta.soundaround.music.data.ItunesResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ItunesService {
    @GET("search")
    suspend fun search(
        @Query("term") term: String,
        @Query("entity") entity: String = "song",
        @Query("media") media: String = "music",
        @Query("limit") limit: Int = 5
    ): ItunesResponse
}
