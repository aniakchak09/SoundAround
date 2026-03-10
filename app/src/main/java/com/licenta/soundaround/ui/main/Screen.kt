package com.licenta.soundaround.ui.main

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object LastFm : Screen("lastfm")
    object ITunes : Screen("itunes")
    object ListenBrainz : Screen("listenbrainz")
    object Instructions : Screen("instructions")
}