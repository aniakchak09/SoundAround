package licenta.soundaround

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import licenta.soundaround.core.AppContainer
import licenta.soundaround.core.AppNav
import licenta.soundaround.core.SupabaseConfig
import licenta.soundaround.ui.theme.SoundAroundTheme
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseConfig.init(applicationContext)
        AppContainer.init(applicationContext)
        MapLibre.getInstance(applicationContext)

        enableEdgeToEdge()
        setContent {
            SoundAroundTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    AppNav()
                }
            }
        }
    }
}
