package licenta.soundaround.music.data

import android.util.Log
import licenta.soundaround.BuildConfig
import licenta.soundaround.music.ItunesService
import licenta.soundaround.music.LastFmService
import licenta.soundaround.music.domain.model.Track
import licenta.soundaround.music.domain.repository.MusicRepository


class MusicRepositoryImpl(
    private val api: LastFmService,
    private val itunesApi: ItunesService
) : MusicRepository {

    override suspend fun getCurrentTrack(username: String): Track? {
        return try {
            val response = api.getRecentTracks(username, BuildConfig.LASTFM_API_KEY)
            response.recenttracks.track.firstOrNull()?.toDomain()
        } catch (e: Exception) {
            Log.e("MusicRepositoryImpl", "Error fetching current track: ${e.message}")
            null
        }
    }

    override suspend fun getRecentTracks(username: String, limit: Int): List<Track> {
        return try {
            val response = api.getRecentTracks(username, BuildConfig.LASTFM_API_KEY, limit)
            response.recenttracks.track.map { it.toDomain() }
        } catch (e: Exception) {
            Log.e("MusicRepositoryImpl", "Error fetching recent tracks: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getPreviewUrl(artist: String, track: String): String? {
        return try {
            val term = "$artist $track"
            val response = itunesApi.search(term = term)
            response.results.firstOrNull()?.previewUrl
        } catch (e: Exception) {
            Log.e("MusicRepositoryImpl", "Error fetching iTunes preview: ${e.message}")
            null
        }
    }
}