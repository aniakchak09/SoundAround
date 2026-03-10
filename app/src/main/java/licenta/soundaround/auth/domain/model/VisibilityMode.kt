package licenta.soundaround.auth.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class VisibilityMode {
    @SerialName("PUBLIC") PUBLIC,
    @SerialName("FRIENDS_ONLY") FRIENDS_ONLY,
    @SerialName("INVISIBLE") INVISIBLE
}
