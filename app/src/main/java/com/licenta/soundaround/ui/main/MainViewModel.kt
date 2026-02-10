package com.licenta.soundaround.ui.main

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.licenta.soundaround.auth.SpotifyAuthManager

class MainViewModel(
    private val spotifyAuthManager: SpotifyAuthManager
) : ViewModel() {

    fun onLoginClicked(activity: Activity) {
        spotifyAuthManager.login(activity)
    }

    fun handleAuthResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        onCodeReceived: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        spotifyAuthManager.handleAuthResult(
            requestCode,
            resultCode,
            data,
            onCodeReceived,
            onError
        )
    }
}
