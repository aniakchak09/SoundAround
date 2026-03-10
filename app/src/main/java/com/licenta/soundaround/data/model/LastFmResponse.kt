package com.licenta.soundaround.data.model

import com.google.gson.annotations.SerializedName

data class LastFmResponse(
    val recenttracks: RecentTracks
)

data class RecentTracks(
    val track: List<LastFmTrack>
)

data class LastFmTrack(
    val title: String,
    val artist: ArtistInfo,
    @SerializedName("@attr") val attr: TrackAttributes? = null
)

data class ArtistInfo(
    @SerializedName("#text") val name: String
)

data class TrackAttributes(
    val nowplaying: String? = "false"
)