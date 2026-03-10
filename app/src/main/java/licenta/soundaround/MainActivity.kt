package licenta.soundaround

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import licenta.soundaround.music.LastFmService
import licenta.soundaround.music.RetrofitClient
import licenta.soundaround.ui.theme.SoundAroundTheme
import licenta.soundaround.music.domain.repository.MusicRepository
import licenta.soundaround.core.AppNav
import licenta.soundaround.core.SupabaseConfig

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseConfig.init(applicationContext)

        enableEdgeToEdge()
        setContent {
            SoundAroundTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    AppNav()
                }
            }
        }
    }
}
