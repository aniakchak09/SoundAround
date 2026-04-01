package licenta.soundaround.music.data

data class ArtistTopTagsResponse(val toptags: ArtistTagsContainer)
data class ArtistTagsContainer(val tag: List<ArtistTagDto> = emptyList())
data class ArtistTagDto(val name: String, val count: Int = 0)
