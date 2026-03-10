package com.licenta.soundaround.ui.main.lastfm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.licenta.soundaround.data.model.Track
import com.licenta.soundaround.data.repository.TrackRepositoryImpl
import kotlinx.coroutines.launch

class LastFmViewModel(private val repository: TrackRepositoryImpl) : ViewModel() {
    var username by mutableStateOf("")
    var trackInfo by mutableStateOf<Track?>(null)
    var isLoading by mutableStateOf(false)
    var nowPLaying by mutableStateOf<Track?>(null)
    var recentTracks by mutableStateOf<List<Track>>(emptyList())

    fun fetchNowPlaying() {
        viewModelScope.launch {
            isLoading = true
            try {
                nowPLaying = repository.getNowPlaying(username)
                trackInfo = nowPLaying
            } catch (e: Exception) {
                trackInfo = null
            } finally {
                isLoading = false
            }
        }
    }

    fun fetchRecentTracks() {
        viewModelScope.launch {
            isLoading = true
            try {
                recentTracks = repository.getRecentTracks(username)
            } catch (e: Exception) {
                trackInfo = null
            } finally {
                isLoading = false
            }
        }
    }
}