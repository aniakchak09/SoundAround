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
    @SerialName("privacy_mode") val privacyMode: VisibilityMode? = VisibilityMode.PUBLIC,
    @SerialName("lastfm_username") val lastFmUsername: String? = null
)

@Serializable
private data class UserProfileDetailsDto(
    val bio: String? = null,
    @SerialName("lastfm_username") val lastFmUsername: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("privacy_mode") val privacyMode: VisibilityMode? = VisibilityMode.PUBLIC
)

@Serializable
private data class LocationsLastSeenDto(
    @SerialName("last_seen_at") val lastSeenAt: String? = null
)

data class UserProfileInfo(
    val bio: String?,
    val lastFmUsername: String?,
    val avatarUrl: String?,
    val lastSeenAt: String?
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
            .select(Columns.raw("id,username,privacy_mode,lastfm_username")) {
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
            .map { user ->
                user.copy(
                    username = profiles[user.userId]?.username?.takeIf { it.isNotBlank() },
                    lastFmUsername = profiles[user.userId]?.lastFmUsername?.takeIf { it.isNotBlank() }
                )
            }
    }

    suspend fun getCurrentUserLastFmUsername(): String? {
        val userId = client.auth.currentUserOrNull()?.id ?: return null
        return try {
            client.from("profiles")
                .select(Columns.raw("lastfm_username")) {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<UserProfileDetailsDto>()
                ?.lastFmUsername
                ?.takeIf { it.isNotBlank() }
        } catch (_: Exception) { null }
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

    suspend fun getUserProfileDetails(userId: String): UserProfileInfo {
        return try {
            val dto = client.from("profiles")
                .select(Columns.raw("bio,lastfm_username,avatar_url,privacy_mode")) {
                    filter { eq("id", userId) }
                }
                .decodeSingleOrNull<UserProfileDetailsDto>()
            val lastSeenAt = try {
                client.from("locations")
                    .select(Columns.raw("last_seen_at")) {
                        filter { eq("user_id", userId) }
                    }
                    .decodeSingleOrNull<LocationsLastSeenDto>()
                    ?.lastSeenAt
            } catch (_: Exception) { null }

            val currentUserId = client.auth.currentUserOrNull()?.id
            val privacyMode = dto?.privacyMode ?: VisibilityMode.PUBLIC
            val showLastFm = when (privacyMode) {
                VisibilityMode.PUBLIC -> true
                VisibilityMode.FRIENDS_ONLY,
                VisibilityMode.INVISIBLE -> {
                    if (currentUserId == null) false
                    else currentUserId == userId || getFriendIds(currentUserId).contains(userId)
                }
            }

            UserProfileInfo(
                bio = dto?.bio?.takeIf { it.isNotBlank() },
                lastFmUsername = dto?.lastFmUsername?.takeIf { it.isNotBlank() && showLastFm },
                avatarUrl = dto?.avatarUrl?.takeIf { it.isNotBlank() },
                lastSeenAt = lastSeenAt
            )
        } catch (e: Exception) {
            UserProfileInfo(null, null, null, null)
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
