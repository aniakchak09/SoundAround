package licenta.soundaround.auth.data

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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

    fun signUp(email: String, pass: String, username: String, bio: String, lastFmUsername: String): Flow<AuthResponse> = flow {
        Log.d("AuthRepository", "Attempting sign-up for $email")
        try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = pass
            }

            val id = client.auth.currentUserOrNull()?.id
            Log.d("AuthRepository", "Sign-up auth successful, user id: $id")

            if (id == null) {
                emit(AuthResponse.Error("Sign-up failed: could not get user ID"))
                return@flow
            }

            try {
                client.auth.retrieveUserForCurrentSession(updateSession = true)
                Log.d("AuthRepository", "Session retrieved, inserting profile...")
                client.from("profiles").insert(
                    ProfileDto(id = id, username = username, bio = bio, lastFmUsername = lastFmUsername)
                )
                Log.d("AuthRepository", "Profile inserted successfully")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Profile insert failed: ${e.message}", e)
                emit(AuthResponse.Error("Profile creation failed: ${e.message}"))
                return@flow
            }

            emit(AuthResponse.Success)
        } catch (e: RestException) {
            Log.d("AuthRepository", "Sign-up failed for $email: ${e.message}, status: ${e.statusCode}")
            emit(AuthResponse.Error(checkSignUpErrors(e)))
        } catch (e: Exception) {
            Log.d("AuthRepository", "Sign-up failed for $email: ${e.message}")
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
                }.decodeSingle<ProfileDto>()
            } else {
                Log.e("Auth", "No user logged in to fetch profile")
                null
            }
        } catch (e: Exception) {
            Log.e("Auth", "Fetch profile failed", e)
            null
        }
    }

    suspend fun updateUsername(username: String): Boolean {
        return try {
            val id = client.auth.currentUserOrNull()?.id
            if (id != null) {
                client.from("profiles").update({
                    set("username", username)
                }) {
                    filter { eq("id", id) }
                }
                true
            } else {
                Log.e("Auth", "No user logged in to update username")
                false
            }
        } catch (e: Exception) {
            Log.e("Auth", "Update username failed", e)
            false
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

    suspend fun updateLastFm(lastFmUsername: String): Boolean {
        return try {
            val id = client.auth.currentUserOrNull()?.id
            if (id != null) {
                client.from("profiles").update({
                    set("lastfm_username", lastFmUsername)
                }) {
                    filter { eq("id", id) }
                }
                true
            } else {
                Log.e("Auth", "No user logged in to update Last.fm")
                false
            }
        } catch (e: Exception) {
            Log.e("Auth", "Update Last.fm failed", e)
            false
        }
    }
}
