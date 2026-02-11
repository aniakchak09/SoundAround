package com.licenta.soundaround

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.licenta.soundaround.api.LastFmClient
import com.licenta.soundaround.ui.main.DashboardScreen
import com.licenta.soundaround.ui.main.Screen
import com.licenta.soundaround.ui.main.lastfm.LastFmScreen
import com.licenta.soundaround.ui.theme.SoundAroundTheme
import kotlin.getValue
import com.licenta.soundaround.data.repository.TrackRepositoryImpl
import com.licenta.soundaround.ui.main.lastfm.LastFmViewModel

object AppContainer {
    val trackRepository: TrackRepositoryImpl by lazy {
        TrackRepositoryImpl(LastFmClient.service)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SoundAroundTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(Screen.LastFm.route) {
            val viewModel: LastFmViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return LastFmViewModel(AppContainer.trackRepository) as T
                    }
                }
            )

            LastFmScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
//        composable(Screen.ITunes.route) {
//            ITunesScreen(onBack = { navController.popBackStack() })
//        }
//        composable(Screen.ListenBrainz.route) {
//            ListenBrainzScreen(onBack = { navController.popBackStack() })
//        }
//        composable(Screen.Instructions.route) {
//            InstructionsScreen(onBack = { navController.popBackStack() })
//        }
    }
}
