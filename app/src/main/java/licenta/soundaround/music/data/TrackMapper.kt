package licenta.soundaround.music.data

import licenta.soundaround.music.domain.model.Track

fun LastFmTrackDto.toDomain(): Track {
    return Track(
        title = this.name,
        artist = this.artist.name,
        // We look for the "extralarge" image; if missing, we take any or empty string
        imageUrl = this.image.find { it.size == "extralarge" }?.url ?: "",
        isNowPlaying = this.attributes?.nowplaying == "true"
    )
}