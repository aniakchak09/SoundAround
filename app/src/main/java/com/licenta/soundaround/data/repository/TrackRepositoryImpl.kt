package com.licenta.soundaround.data.repository

import com.licenta.soundaround.api.LastFmService
import android.util.Log
import com.licenta.soundaround.data.model.Track

class TrackRepositoryImpl(
    private val lastFmService: LastFmService
) {
    private val apiKey = "bb981c5f4ae7e23af12fd342bbe690be" // Use your actual key

    suspend fun getNowPlaying(username: String): Track? {
        return try {
            // 1. Fetch from the client you already have
            val response = lastFmService.getRecentTracks(username, apiKey)
            val latest = response.recenttracks.track.firstOrNull()

            Log.d("REPOSITORY", "Response from Last.fm: $latest")

            // 2. Map messy API data to your clean Track model
            Track(
                title = latest?.name ?: "Unknown Title",
                artist = latest?.artist?.name ?: "Unknown Artist",
                albumArtUrl = latest?.image?.lastOrNull()?.url, // Get the largest image
                isNowPlaying = latest?.attr?.nowplaying == "true",
                previewUrl = null // We will fill this from iTunes later
            )
        } catch (e: Exception) {
            Log.e("REPOSITORY", "Error fetching track: ${e.message}")
            null
        }
    }

    suspend fun getRecentTracks(username: String): List<Track> {
        return try {
            val response = lastFmService.getRecentTracks(username, apiKey)
            response.recenttracks.track.map { apiTrack ->
                Track(
                    title = apiTrack.name,
                    artist = apiTrack.artist.name,
                    isNowPlaying = apiTrack.attr?.nowplaying == "true",
                    albumArtUrl = apiTrack.image.lastOrNull()?.url,
                    previewUrl = null
                )
            }
        } catch (e: Exception) {
            Log.e("REPOSITORY", "Error fetching recent tracks: ${e.message}")
            emptyList()
        }
    }
}