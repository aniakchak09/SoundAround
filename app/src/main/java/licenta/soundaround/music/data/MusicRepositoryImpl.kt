package licenta.soundaround.music.data

import android.util.Log
import licenta.soundaround.BuildConfig
import licenta.soundaround.music.LastFmService
import licenta.soundaround.music.domain.model.Track
import licenta.soundaround.music.domain.repository.MusicRepository


class MusicRepositoryImpl(
    private val api: LastFmService
) : MusicRepository {

    override suspend fun getCurrentTrack(username: String): Track? {
        return try {
            val response = api.getRecentTracks(username, BuildConfig.LASTFM_API_KEY)

            // 1. Get the first track from the list
            // 2. Map it to the clean domain model using .toDomain()
            response.recenttracks.track.firstOrNull()?.toDomain()

        } catch (e: Exception) {
            Log.e("MusicRepositoryImpl", "Error fetching current track: ${e.message}")
            null // You can handle specific errors here later
        }
    }
}