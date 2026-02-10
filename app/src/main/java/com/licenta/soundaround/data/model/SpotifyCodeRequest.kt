package com.licenta.soundaround.data.model

data class SpotifyCodeRequest(
    val code: String,
    val code_verifier: String
)
