//package com.licenta.soundaround.api
//
//import com.google.gson.annotations.SerializedName
//
//data class LastFmResponse(val recenttracks: RecentTracks)
//data class RecentTracks(val track: List<Track>)
//data class Track(
//    val name: String,
//    val artist: Artist,
//    @SerializedName("@attr") val attr: TrackAttributes?
//)
//data class Artist(@SerializedName("#text") val name: String)
//data class TrackAttributes(val nowplaying: String?)