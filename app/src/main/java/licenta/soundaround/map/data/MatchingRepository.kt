package licenta.soundaround.map.data

import licenta.soundaround.BuildConfig
import licenta.soundaround.map.domain.model.UserLocation
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MatchingRepository(private val okHttpClient: OkHttpClient) {

    private val functionUrl =
        "${BuildConfig.SUPABASE_URL}/functions/v1/compatibility-score"

    // Cache scores: key = "myUsername|theirUserId", value = score
    private val scoreCache = mutableMapOf<String, Float>()

    suspend fun computeScores(
        myUsername: String,
        users: List<UserLocation>
    ): Map<String, Float> = withContext(Dispatchers.IO) {
        val toCompute = users.filter {
            !it.lastFmUsername.isNullOrBlank() &&
            !scoreCache.containsKey("$myUsername|${it.userId}")
        }
        if (toCompute.isEmpty()) {
            return@withContext scoreCache
                .filter { (k, _) -> k.startsWith("$myUsername|") }
                .mapKeys { (k, _) -> k.removePrefix("$myUsername|") }
        }

        try {
            val usernamesArray = JSONArray()
            for (user in toCompute) {
                usernamesArray.put(JSONObject().apply {
                    put("userId", user.userId)
                    put("lastFmUsername", user.lastFmUsername)
                })
            }
            val body = JSONObject().apply {
                put("myUsername", myUsername)
                put("usernames", usernamesArray)
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(functionUrl)
                .post(body)
                .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "{}")
                val scores = json.optJSONObject("scores")
                if (scores != null) {
                    for (userId in scores.keys()) {
                        scoreCache["$myUsername|$userId"] = scores.getDouble(userId).toFloat()
                    }
                }
            }
        } catch (_: Exception) {}

        scoreCache
            .filter { (k, _) -> k.startsWith("$myUsername|") }
            .mapKeys { (k, _) -> k.removePrefix("$myUsername|") }
    }

    /** Full reset — use only on explicit user-triggered refresh. */
    fun clearUserCache() {
        scoreCache.clear()
    }

    /**
     * Evicts scores for users who are no longer in the active list.
     */
    fun evictUsersNotIn(myUsername: String, activeUserIds: Set<String>) {
        scoreCache.keys.toList().forEach { key ->
            if (key.startsWith("$myUsername|")) {
                val userId = key.removePrefix("$myUsername|")
                if (userId !in activeUserIds) scoreCache.remove(key)
            }
        }
    }
}
