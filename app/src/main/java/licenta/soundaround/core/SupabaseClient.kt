package licenta.soundaround.core

import android.content.Context
import com.russhwolf.settings.SharedPreferencesSettings
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import licenta.soundaround.BuildConfig

object SupabaseConfig {
    lateinit var client: io.github.jan.supabase.SupabaseClient
        private set

    fun init(context: Context) {
        client = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                sessionManager = SettingsSessionManager(
                    SharedPreferencesSettings(context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE))
                )
            }
            install(Postgrest)
            install(Storage)
        }
    }
}