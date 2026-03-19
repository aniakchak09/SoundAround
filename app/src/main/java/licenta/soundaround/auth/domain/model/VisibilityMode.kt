package licenta.soundaround.auth.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class VisibilityMode {
    @SerialName("public") PUBLIC,
    @SerialName("friends_only") FRIENDS_ONLY,
    @SerialName("invisible") INVISIBLE
}
