package licenta.soundaround.music.domain.repository

import licenta.soundaround.music.data.LastFmUserDto
import licenta.soundaround.music.data.TopArtistDto
import licenta.soundaround.music.data.TopTrackDto
import licenta.soundaround.music.domain.model.Track

interface MusicRepository {
    suspend fun getCurrentTrack(username: String): Track?
    suspend fun getRecentTracks(username: String, limit: Int = 50): List<Track>
    suspend fun getPreviewUrl(artist: String, track: String): String?
    suspend fun getTopArtists(username: String): List<TopArtistDto>
    suspend fun getTopTracks(username: String): List<TopTrackDto>
    suspend fun getUserInfo(username: String): LastFmUserDto?
    suspend fun getArtistImageUrl(artistName: String): String?
    suspend fun getTrackImageUrl(artist: String, track: String): String?
}