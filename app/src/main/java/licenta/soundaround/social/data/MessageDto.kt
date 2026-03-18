package licenta.soundaround.social.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import licenta.soundaround.social.domain.model.Message

@Serializable
data class MessageDto(
    val id: String = "",
    @SerialName("conversation_id") val conversationId: String = "",
    @SerialName("sender_id") val senderId: String = "",
    val content: String = "",
    @SerialName("sent_at") val sentAt: String = ""
)

@Serializable
data class MessageInsertDto(
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val content: String
)

fun MessageDto.toDomain() = Message(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    content = content,
    sentAt = sentAt
)
