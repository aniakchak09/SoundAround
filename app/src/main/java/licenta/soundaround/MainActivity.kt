package licenta.soundaround

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import licenta.soundaround.core.AppContainer
import licenta.soundaround.core.AppNav
import licenta.soundaround.core.SupabaseConfig
import licenta.soundaround.ui.theme.SoundAroundTheme
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseConfig.init(applicationContext)
        AppContainer.init(applicationContext)
        MapLibre.getInstance(applicationContext)

        enableEdgeToEdge()
        setContent {
            SoundAroundTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            AppContainer.presenceRepository.touchLastSeen()
        }
    }
}
