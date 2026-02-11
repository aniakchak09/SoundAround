package com.licenta.soundaround.data.model

class Track {
    val title: String
    val artist: String
    val albumArtUrl: String?
    val isNowPlaying: Boolean
    val previewUrl: String?

    constructor(
        title: String,
        artist: String,
        albumArtUrl: String?,
        isNowPlaying: Boolean,
        previewUrl: String?
    ) {
        this.title = title
        this.artist = artist
        this.albumArtUrl = albumArtUrl
        this.isNowPlaying = isNowPlaying
        this.previewUrl = previewUrl
    }
}