package com.licenta.soundaround.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface LastFmService {
    @GET("2.0/?method=user.getrecenttracks&format=json")
    suspend fun getRecentTracks(
        @Query("user") user: String,
        @Query("api_key") apiKey: String
    ): LastFmResponse
}

// Data models to parse the complex Last.fm JSON response
data class LastFmResponse(val recenttracks: RecentTracks)
data class RecentTracks(val track: List<Track>)
data class Track(
    val name: String,
    val artist: Artist,
    val image: List<Image>,
    @com.google.gson.annotations.SerializedName("@attr") val attr: Attributes?
)
data class Artist(
    @SerializedName("#text") val name: String // Handles the # symbol
)

data class Image(
    @SerializedName("#text") val url: String, // Handles the # symbol
    val size: String
)

data class Attributes(
    val nowplaying: String? = "false"
)