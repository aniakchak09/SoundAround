package com.licenta.soundaround.api

import retrofit2.http.GET
import retrofit2.http.Query

interface LastFmService {
    @GET("2.0/?method=user.getrecenttracks&format=json&limit=1")
    suspend fun getNowPlaying(
        @Query("user") username: String,
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
data class Artist(val text: String) { @com.google.gson.annotations.SerializedName("#text") var name: String = text }
data class Image(val text: String) { @com.google.gson.annotations.SerializedName("#text") var url: String = text }
data class Attributes(val nowplaying: String)