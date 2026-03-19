package licenta.soundaround.music.data

import android.util.Log
import licenta.soundaround.BuildConfig
import licenta.soundaround.music.ItunesService
import licenta.soundaround.music.LastFmService
import licenta.soundaround.music.domain.model.Track
import licenta.soundaround.music.domain.repository.MusicRepository
import licenta.soundaround.music.data.LastFmUserDto
import licenta.soundaround.music.data.TopArtistDto
import licenta.soundaround.music.data.TopTrackDto


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

    override suspend fun getTopArtists(username: String): List<TopArtistDto> {
        return try {
            api.getTopArtists(username, BuildConfig.LASTFM_API_KEY).topartists.artist
        } catch (e: Exception) {
            Log.e("MusicRepositoryImpl", "Error fetching top artists: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getTopTracks(username: String): List<TopTrackDto> {
        return try {
            api.getTopTracks(username, BuildConfig.LASTFM_API_KEY).toptracks.track
        } catch (e: Exception) {
            Log.e("MusicRepositoryImpl", "Error fetching top tracks: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getUserInfo(username: String): LastFmUserDto? {
        return try {
            api.getUserInfo(username, BuildConfig.LASTFM_API_KEY).user
        } catch (e: Exception) {
            Log.e("MusicRepositoryImpl", "Error fetching user info: ${e.message}")
            null
        }
    }

    override suspend fun getTrackImageUrl(artist: String, track: String): String? {
        return try {
            itunesApi.search(term = "$artist $track", entity = "song", limit = 1)
                .results.firstOrNull()?.artworkUrl100
        } catch (e: Exception) {
            Log.e("MusicRepositoryImpl", "Error fetching track image: ${e.message}")
            null
        }
    }

    override suspend fun getArtistImageUrl(artistName: String): String? {
        return try {
            itunesApi.search(term = artistName, entity = "musicArtist", limit = 1)
                .results.firstOrNull()?.artworkUrl100
        } catch (e: Exception) {
            Log.e("MusicRepositoryImpl", "Error fetching artist image: ${e.message}")
            null
        }
    }
}