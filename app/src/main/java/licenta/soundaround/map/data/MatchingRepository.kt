package licenta.soundaround.map.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import licenta.soundaround.music.data.ArtistTagDto
import licenta.soundaround.music.data.TopArtistDto
import licenta.soundaround.music.data.TopTrackDto
import licenta.soundaround.music.domain.repository.MusicRepository
import kotlin.math.sqrt

class MatchingRepository(private val musicRepository: MusicRepository) {

    // Artist tags are stable — keep them across sessions
    private val artistTagsCache = mutableMapOf<String, List<ArtistTagDto>>()

    // User profiles reset when users list refreshes
    private val userProfileCache = mutableMapOf<String, UserMusicProfile?>()

    data class UserMusicProfile(
        val artistVector: Map<String, Float>,   // artist name -> playcount
        val trackVector: Map<String, Float>,    // "track_artist" -> playcount
        val genreVector: Map<String, Float>     // tag name -> weighted score
    )

    /**
     * Returns a 0–1 compatibility score between two last.fm users.
     * Weighted: 40% artists, 35% tracks, 25% genre tags.
     * Returns 0 if either user has no last.fm data.
     */
    suspend fun getCompatibilityScore(myUsername: String, theirUsername: String): Float {
        val mine = loadProfile(myUsername) ?: return 0f
        val theirs = loadProfile(theirUsername) ?: return 0f

        val artistScore = cosineSimilarity(mine.artistVector, theirs.artistVector)
        val trackScore = cosineSimilarity(mine.trackVector, theirs.trackVector)
        val genreScore = cosineSimilarity(mine.genreVector, theirs.genreVector)

        return 0.40f * artistScore + 0.35f * trackScore + 0.25f * genreScore
    }

    private suspend fun loadProfile(username: String): UserMusicProfile? {
        userProfileCache[username]?.let { return it }

        val artists = musicRepository.getTopArtists(username, limit = 50)
        if (artists.isEmpty()) {
            userProfileCache[username] = null
            return null
        }
        val tracks = musicRepository.getTopTracks(username, limit = 50)

        val artistVector = artists.associate {
            it.name.lowercase() to (it.playcount.toFloatOrNull() ?: 0f)
        }
        val trackVector = tracks.associate {
            "${it.name.lowercase()}_${it.artist.name.lowercase()}" to (it.playcount.toFloatOrNull() ?: 0f)
        }
        val genreVector = buildGenreVector(artists)

        return UserMusicProfile(artistVector, trackVector, genreVector)
            .also { userProfileCache[username] = it }
    }

    /**
     * Builds a weighted genre vector from a user's top artists.
     * Each artist's tags are weighted by the user's play count for that artist.
     * Fetches the top 10 artists' tags in parallel.
     */
    private suspend fun buildGenreVector(artists: List<TopArtistDto>): Map<String, Float> =
        coroutineScope {
            val vector = mutableMapOf<String, Float>()
            val mutex = Mutex()

            artists.take(10).map { artist ->
                async {
                    val weight = artist.playcount.toFloatOrNull() ?: 0f
                    val tags = getArtistTags(artist.name)
                    mutex.withLock {
                        tags.take(5).forEach { tag ->
                            val key = tag.name.lowercase()
                            // Weight = user's plays for that artist × tag strength (0–1)
                            vector[key] = (vector[key] ?: 0f) + weight * (tag.count / 100f)
                        }
                    }
                }
            }.forEach { it.await() }

            vector
        }

    private suspend fun getArtistTags(artist: String): List<ArtistTagDto> {
        artistTagsCache[artist]?.let { return it }
        return musicRepository.getArtistTopTags(artist)
            .also { artistTagsCache[artist] = it }
    }

    private fun cosineSimilarity(a: Map<String, Float>, b: Map<String, Float>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        var dot = 0f
        var magA = 0f
        var magB = 0f
        a.forEach { (k, v) ->
            dot += v * (b[k] ?: 0f)
            magA += v * v
        }
        b.values.forEach { v -> magB += v * v }
        val denom = sqrt(magA) * sqrt(magB)
        return if (denom == 0f) 0f else (dot / denom).coerceIn(0f, 1f)
    }

    /** Call when the users list is refreshed to recompute stale scores. */
    fun clearUserCache() {
        userProfileCache.clear()
    }
}
