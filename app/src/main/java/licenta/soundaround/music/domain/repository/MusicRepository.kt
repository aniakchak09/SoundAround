package licenta.soundaround.music.domain.repository

import licenta.soundaround.music.domain.model.Track

interface MusicRepository {
    suspend fun getCurrentTrack(username: String): Track?
}