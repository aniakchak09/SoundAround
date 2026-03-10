package com.licenta.soundaround.data.repository

import android.util.Log
import com.licenta.soundaround.api.LastFmService
import com.licenta.soundaround.data.model.Track

private val API_KEY = "bb981c5f4ae7e23af12fd342bbe690be"

class MusicRepository(private val lastFmService: LastFmService) {

    suspend fun getNowPlaying(username: String): Track? {
        return try {
            // Use your existing interface
            val response = lastFmService.getRecentTracks(username, API_KEY)

            // Get the first track (the most recent or currently playing)
            val latest = response.recenttracks.track.firstOrNull() ?: return null

            // Map the API data to our clean Domain model
            Track(
                title = latest.name,
                artist = latest.artist.name,
                albumArtUrl = latest.image.lastOrNull()?.url, // Get the largest image
                isNowPlaying = latest.attr?.nowplaying == "true",
                previewUrl = null // We will fill this from iTunes later
            )
        } catch (e: Exception) {
            Log.e("REPO_ERROR", "Failed to fetch from Last.fm: ${e.message}")
            null
        }
    }

    suspend fun getRecentTracks(username: String): List<Track> {
        return try {
            val response = lastFmService.getRecentTracks(username, API_KEY)
            response.recenttracks.track.map { track ->
                Track(
                    title = track.name,
                    artist = track.artist.name,
                    albumArtUrl = track.image.lastOrNull()?.url,
                    isNowPlaying = track.attr?.nowplaying == "true",
                    previewUrl = null
                )
            }
        } catch (e: Exception) {
            Log.e("REPO_ERROR", "Failed to fetch recent tracks: ${e.message}")
            emptyList()
        }
    }
}