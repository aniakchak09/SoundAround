package licenta.soundaround.social.domain.model

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val sentAt: String
)
