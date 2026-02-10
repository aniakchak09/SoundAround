package com.licenta.soundaround.data.model

data class SpotifyTokenResponse(
    val access_token: String,
    val refresh_token: String?,
    val expires_in: Int
)
