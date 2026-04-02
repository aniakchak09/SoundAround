package licenta.soundaround.auth.data

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import licenta.soundaround.auth.domain.model.VisibilityMode
import licenta.soundaround.core.SupabaseConfig
import licenta.soundaround.core.toUserMessage

@Serializable
private data class VisibilityDto(
    @SerialName("privacy_mode") val visibilityMode: VisibilityMode? = VisibilityMode.PUBLIC
)

sealed interface AuthResponse {
    data object Success : AuthResponse
    data class Error(val message: String) : AuthResponse
}

class AuthRepository {
    private val client = SupabaseConfig.client

    private suspend fun isUsernameTaken(username: String, excludeId: String? = null): Boolean {
        return try {
            val matches = client.from("profiles")
                .select { filter { ilike("username", username) } }
                .decodeList<ProfileDto>()
            if (excludeId != null) matches.any { it.id != excludeId } else matches.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    fun signUp(email: String, pass: String, username: String, bio: String, lastFmUsername: String): Flow<AuthResponse> = flow {
        Log.d("AuthRepository", "Attempting sign-up for $email")

        if (username.isNotBlank() && isUsernameTaken(username)) {
            emit(AuthResponse.Error("Username already taken. Please choose a different one."))
            return@flow
        }

        try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = pass
                data = buildJsonObject {
                    put("username", username)
                    put("bio", bio)
                    put("lastfm_username", lastFmUsername)
                }
            }

            Log.d("AuthRepository", "Sign-up auth successful, profile will be created by DB trigger")
            emit(AuthResponse.Success)
        } catch (e: RestException) {
            Log.d("AuthRepository", "Sign-up failed for $email: ${e.message}, status: ${e.statusCode}")
            try { client.auth.signOut() } catch (_: Exception) {}
            emit(AuthResponse.Error(checkSignUpErrors(e)))
        } catch (e: Exception) {
            Log.d("AuthRepository", "Sign-up failed for $email: ${e.message}")
            try { client.auth.signOut() } catch (_: Exception) {}
            emit(AuthResponse.Error(e.toUserMessage()))
        }
    }

    private fun checkSignUpErrors(e: RestException): String {
        return when (e.message) {
            "User already registered." -> "This email is already registered. Try logging in."
            "Password should be at least 6 characters." -> "Password must be at least 6 characters long."
            "Password should contain at least one character of each: abcdefghijklmnopqrstuvwxyz, ABCDEFGHIJKLMNOPQRSTUVWXYZ, 0123456789, !@#\$%^&*()_+-=[]{};':\"|<>?,./`~." -> "Password must include uppercase, lowercase, number, and special character."
            "Unable to validate email address: invalid format" -> "Invalid email format. Please enter a valid email."
            else -> e.toUserMessage()
        }
    }

    fun signIn(email: String, pass: String): Flow<AuthResponse> = flow {
        try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = pass
            }
            Log.d("AuthRepository", "Sign-in successful for $email")
            emit(AuthResponse.Success)
        } catch (e: RestException) {
            val message = when (e.message) {
                "Invalid login credentials" -> "Invalid email or password. Please try again."
                else -> e.toUserMessage()
            }
            Log.d("AuthRepository", "Sign-in failed for $email: ${e.message}, status: ${e.statusCode}")
            emit(AuthResponse.Error(message))
        } catch (e: Exception) {
            Log.d("AuthRepository", "Sign-in failed for $email: ${e.message}")
            emit(AuthResponse.Error(e.toUserMessage()))
        }
    }

    suspend fun sendResetOtp(email: String): AuthResponse {
        return try {
            client.auth.signInWith(OTP) {
                this.email = email
                createUser = false
            }
            AuthResponse.Success
        } catch (e: Exception) {
            Log.e("AuthRepository", "sendResetOtp failed: ${e.message}")
            AuthResponse.Error("Could not send reset code. Check that the email is correct.")
        }
    }

    suspend fun verifyResetOtp(email: String, token: String): AuthResponse {
        return try {
            client.auth.verifyEmailOtp(
                type = OtpType.Email.EMAIL,
                email = email,
                token = token
            )
            AuthResponse.Success
        } catch (e: Exception) {
            Log.e("AuthRepository", "verifyResetOtp failed: ${e.message}")
            AuthResponse.Error("Invalid or expired code. Please try again.")
        }
    }

    suspend fun updatePassword(newPassword: String): AuthResponse {
        return try {
            client.auth.updateUser { password = newPassword }
            try { client.auth.signOut() } catch (_: Exception) {}
            AuthResponse.Success
        } catch (e: Exception) {
            Log.e("AuthRepository", "updatePassword failed: ${e.message}")
            AuthResponse.Error(e.toUserMessage())
        }
    }

    suspend fun uploadAvatar(imageBytes: ByteArray): String? {
        return try {
            val id = client.auth.currentUserOrNull()?.id ?: return null
            val path = "$id/avatar.jpg"
            client.storage.from("avatars").upload(path, imageBytes) { upsert = true }
            client.storage.from("avatars").publicUrl(path)
        } catch (e: Exception) {
            Log.e("Auth", "Avatar upload failed: ${e.message}")
            null
        }
    }

    suspend fun updateAvatarUrl(url: String): Boolean {
        return try {
            val id = client.auth.currentUserOrNull()?.id ?: return false
            client.from("profiles").update({ set("avatar_url", url) }) {
                filter { eq("id", id) }
            }
            true
        } catch (e: Exception) {
            Log.e("Auth", "Update avatar url failed: ${e.message}")
            false
        }
    }

    suspend fun signOut() {
        try {
            client.auth.signOut()
            Log.d("AuthRepository", "Sign-out successful")
        } catch (e: Exception) {
            Log.d("AuthRepository", "Sign-out failed: ${e.message}")
        }
    }

    fun getCurrentUser() = client.auth.currentUserOrNull()

    fun getCurrentSession() = client.auth.currentSessionOrNull()

    fun observeSession() = client.auth.sessionStatus

    suspend fun isUserLoggedIn(): Boolean {
        return try {
            client.auth.retrieveUserForCurrentSession(updateSession = true)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getProfile(): ProfileDto? {
        return try {
            val id = client.auth.currentUserOrNull()?.id
            if (id != null) {
                client.from("profiles").select {
                    filter {
                        eq("id", id)
                    }
                }.decodeSingleOrNull<ProfileDto>()
            } else {
                Log.e("Auth", "No user logged in to fetch profile")
                null
            }
        } catch (e: Exception) {
            Log.e("Auth", "Fetch profile failed", e)
            null
        }
    }

    suspend fun updateUsername(username: String): String? {
        return try {
            val id = client.auth.currentUserOrNull()?.id
            if (id != null) {
                if (isUsernameTaken(username, excludeId = id)) return "Username already taken. Please choose a different one."
                client.from("profiles").update({
                    set("username", username)
                }) {
                    filter { eq("id", id) }
                }
                null
            } else {
                Log.e("Auth", "No user logged in to update username")
                "Not logged in."
            }
        } catch (e: Exception) {
            Log.e("Auth", "Update username failed", e)
            "Failed to update username."
        }
    }

    suspend fun updateBio(bio: String): Boolean {
        return try {
            val id = client.auth.currentUserOrNull()?.id
            if (id != null) {
                client.from("profiles").update({
                    set("bio", bio)
                }) {
                    filter { eq("id", id) }
                }
                true
            } else {
                Log.e("Auth", "No user logged in to update bio")
                false
            }
        } catch (e: Exception) {
            Log.e("Auth", "Update bio failed", e)
            false
        }
    }

    suspend fun getVisibilityMode(): VisibilityMode {
        return try {
            val id = client.auth.currentUserOrNull()?.id ?: return VisibilityMode.PUBLIC
            val result = client.from("profiles").select {
                filter { eq("id", id) }
            }.decodeSingle<VisibilityDto>()
            result.visibilityMode ?: VisibilityMode.PUBLIC
        } catch (e: Exception) {
            Log.e("Auth", "getVisibilityMode failed, defaulting to PUBLIC: ${e.message}")
            VisibilityMode.PUBLIC
        }
    }

    suspend fun updateVisibilityMode(mode: VisibilityMode): Boolean {
        return try {
            val id = client.auth.currentUserOrNull()?.id ?: return false
            client.from("profiles").update({
                set("privacy_mode", mode.name.lowercase())
            }) {
                filter { eq("id", id) }
            }
            true
        } catch (e: Exception) {
            Log.e("Auth", "Update visibility mode failed", e)
            false
        }
    }

    suspend fun updateLastFm(lastFmUsername: String): String? {
        return try {
            val id = client.auth.currentUserOrNull()?.id ?: return "Not logged in."
            client.from("profiles").update({
                set("lastfm_username", lastFmUsername)
            }) {
                filter { eq("id", id) }
            }
            null
        } catch (e: Exception) {
            Log.e("Auth", "Update Last.fm failed", e)
            if (e.message?.contains("unique", ignoreCase = true) == true ||
                e.message?.contains("duplicate", ignoreCase = true) == true) {
                "That last.fm username is already linked to another account."
            } else {
                "Failed to update last.fm username. Try again."
            }
        }
    }
}
