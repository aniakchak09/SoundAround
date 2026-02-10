package com.licenta.soundaround.auth

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse

class SpotifyAuthManager(
    private val context: Context
) {

    companion object {
        const val CLIENT_ID = "YOUR_SPOTIFY_CLIENT_ID"
        const val REDIRECT_URI = "soundaround://callback"
        const val AUTH_REQUEST_CODE = 1337
    }

    private var codeVerifier: String? = null

    // Inside SpotifyAuthManager.kt
    fun login(activity: Activity) {
        // 1. Generate the random verifier and the hashed challenge
        codeVerifier = PkceUtil.generateCodeVerifier()
        val codeChallenge = PkceUtil.generateCodeChallenge(codeVerifier!!)

        val request = AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.CODE, REDIRECT_URI)
            .setScopes(arrayOf("user-read-currently-playing", "user-top-read"))
            // 2. Attach these to the request
            .setCustomParam("code_challenge", codeChallenge)
            .setCustomParam("code_challenge_method", "S256")
            .build()

//        AuthorizationClient.openLoginActivity(activity, AUTH_REQUEST_CODE, request)
        val intent = AuthorizationClient.createLoginActivityIntent(activity, request)
        intent.putExtra("type", "TOKEN") // This forces a web flow in some SDK versions
        activity.startActivityForResult(intent, AUTH_REQUEST_CODE)
    }

    fun handleAuthResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        onCodeReceived: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d("SPOTIFY_AUTH", "Handling auth result")
        if (requestCode != AUTH_REQUEST_CODE) return

        val response = AuthorizationClient.getResponse(resultCode, data)

        when (response.type) {
            AuthorizationResponse.Type.CODE -> {
                Log.d("SPOTIFY_AUTH", "Authorization code: ${response.code}")
                Log.d("PKCE", "Using verifier: $codeVerifier")
                onCodeReceived(response.code, codeVerifier!!)
            }
            AuthorizationResponse.Type.ERROR -> {
                Log.d("SPOTIFY_AUTH", "Authorization error: ${response.error}")
                onError(response.error ?: "Authorization error")
            }
            else -> {
                onError("Authorization cancelled")
            }
        }
    }
}
