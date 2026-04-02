package licenta.soundaround.presence.data

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import licenta.soundaround.core.SupabaseConfig
import licenta.soundaround.music.domain.model.Track
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PresenceRepository {
    private val client = SupabaseConfig.client
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun publish(track: Track, lat: Double? = null, lng: Double? = null): Boolean {
        return try {
            val userId = client.auth.currentUserOrNull()?.id ?: return false
            val now = isoFormat.format(Date())
            client.from("locations").upsert(
                PresenceDto(
                    userId = userId,
                    trackName = track.title,
                    artistName = track.artist,
                    albumArt = track.imageUrl,
                    isPlaying = track.isNowPlaying,
                    syncedAt = now,
                    lat = lat,
                    lng = lng
                )
            ) {
                onConflict = "user_id"
            }
            // Only update last_seen_at when GPS is available — never overwrite it with null
            if (lat != null && lng != null) {
                client.from("locations")
                    .update({ set("last_seen_at", now) }) {
                        filter { eq("user_id", userId) }
                    }
            }
            Log.d("PresenceRepository", "Published: ${track.title} by ${track.artist} @ $lat,$lng")
            true
        } catch (e: Exception) {
            Log.e("PresenceRepository", "Publish failed: ${e.message}")
            false
        }
    }
}
