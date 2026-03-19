package licenta.soundaround.map.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import licenta.soundaround.auth.domain.model.VisibilityMode
import licenta.soundaround.core.SupabaseConfig
import licenta.soundaround.map.domain.model.UserLocation

@Serializable
private data class PrivacyProfileDto(
    val id: String,
    val username: String? = null,
    @SerialName("privacy_mode") val privacyMode: VisibilityMode? = VisibilityMode.PUBLIC
)

@Serializable
private data class FriendIdDto(
    @SerialName("user_id") val userId: String = "",
    @SerialName("friend_id") val friendId: String = ""
)

class MapRepository {
    private val client = SupabaseConfig.client

    suspend fun getActiveUsers(): List<UserLocation> {
        val currentUserId = client.auth.currentUserOrNull()?.id

        val rawLocations = client.from("locations")
            .select()
            .decodeList<UserLocationDto>()
            .mapNotNull { it.toDomain() }
            .filter { it.userId != currentUserId }

        if (currentUserId == null) return rawLocations

        val userIds = rawLocations.map { it.userId }.distinct()
        if (userIds.isEmpty()) return emptyList()

        val profiles = client.from("profiles")
            .select(Columns.raw("id,username,privacy_mode")) {
                filter { isIn("id", userIds) }
            }
            .decodeList<PrivacyProfileDto>()
            .associateBy { it.id }

        val friendIds = getFriendIds(currentUserId)

        return rawLocations
            .filter { user ->
                when (profiles[user.userId]?.privacyMode ?: VisibilityMode.PUBLIC) {
                    VisibilityMode.PUBLIC -> true
                    VisibilityMode.FRIENDS_ONLY -> user.userId in friendIds
                    VisibilityMode.INVISIBLE -> false
                }
            }
            .map { user -> user.copy(username = profiles[user.userId]?.username?.takeIf { it.isNotBlank() }) }
    }

    private suspend fun getFriendIds(userId: String): Set<String> {
        return try {
            val asSender = client.from("friendships")
                .select(Columns.raw("user_id,friend_id")) {
                    filter { eq("user_id", userId); eq("status", "accepted") }
                }
                .decodeList<FriendIdDto>()
                .map { it.friendId }

            val asReceiver = client.from("friendships")
                .select(Columns.raw("user_id,friend_id")) {
                    filter { eq("friend_id", userId); eq("status", "accepted") }
                }
                .decodeList<FriendIdDto>()
                .map { it.userId }

            (asSender + asReceiver).toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    suspend fun getUsernameForId(userId: String): String? {
        return try {
            client.from("profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<LocationProfileRef>()
                ?.username
                ?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }
}
