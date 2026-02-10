package com.licenta.soundaround.data.model

data class Track(
    val id: String,
    val name: String,
    val artists: List<Artist>
)
