package licenta.soundaround.social.domain.model

data class Conversation(
    val id: String,
    val otherUserId: String,
    val otherUsername: String,
    val isPersistent: Boolean,
    val expiresAt: String?,
    val initialTrackTitle: String?,
    val initialTrackArtist: String?,
    val lastMessageAt: String?
)
