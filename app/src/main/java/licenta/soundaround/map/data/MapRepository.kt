package licenta.soundaround.map.data

import io.github.jan.supabase.postgrest.from
import licenta.soundaround.core.SupabaseConfig
import licenta.soundaround.map.domain.model.UserLocation

class MapRepository {
    private val client = SupabaseConfig.client

    suspend fun getActiveUsers(): List<UserLocation> {
        return client.from("locations")
            .select()
            .decodeList<UserLocationDto>()
            .mapNotNull { it.toDomain() }
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
