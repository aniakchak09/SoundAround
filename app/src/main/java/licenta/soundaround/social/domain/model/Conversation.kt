package licenta.soundaround.social.domain.model

data class Conversation(
    val id: String,
    val otherUserId: String,
    val otherUsername: String,
    val isPersistent: Boolean,
    val expiresAt: String?,
    val myInitialTrackTitle: String?,
    val myInitialTrackArtist: String?,
    val theirInitialTrackTitle: String?,
    val theirInitialTrackArtist: String?,
    val lastMessageAt: String?,
    val lastMessageContent: String?,
    val isUnread: Boolean
)
