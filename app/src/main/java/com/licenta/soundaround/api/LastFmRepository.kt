package com.licenta.soundaround.api

class LastFmRepository(private val service: LastFmService) {
    suspend fun getCurrentSong(username: String, apiKey: String): Track? {
        return try {
            val response = service.getNowPlaying(username, apiKey)
            val latestTrack = response.recenttracks.track.firstOrNull()
            // Last.fm adds a special flag if the song is currently playing
            if (latestTrack?.attr?.nowplaying == "true") latestTrack else null
        } catch (e: Exception) {
            null
        }
    }
}