package licenta.soundaround.core

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import licenta.soundaround.music.presentation.LastFmViewModel
import licenta.soundaround.music.RetrofitClient
import licenta.soundaround.music.data.MusicRepositoryImpl
import licenta.soundaround.music.presentation.LastFmScreen
import licenta.soundaround.auth.data.AuthRepository
import licenta.soundaround.auth.presentation.LoginScreen
import licenta.soundaround.auth.presentation.ManageProfileScreen
import licenta.soundaround.auth.presentation.SignUpScreen

object AppContainer {
    val authRepository: AuthRepository by lazy {
        AuthRepository()
    }

    val trackRepository: MusicRepositoryImpl by lazy {
        MusicRepositoryImpl(RetrofitClient.lastFmService)
    }
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val authRepo = AppContainer.authRepository
    var startRoute by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val isLoggedIn = authRepo.isUserLoggedIn()
        startRoute = if (isLoggedIn) Screen.LastFmTest.route else Screen.Login.route
    }

    startRoute?.let { destination ->
        Log.d("AppNav", "Starting at route: $destination")
        NavHost(navController = navController, startDestination = destination) {
            composable(Screen.Login.route) {
                LoginScreen(
                    authRepo = authRepo,
                    onLoginSuccess = { navController.navigate(Screen.LastFmTest.route) },
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
                )
            }

            composable(Screen.SignUp.route) {
                SignUpScreen(
                    authRepo = authRepo,
                    onSignUpSuccess = { navController.navigate(Screen.LastFmTest.route) }
                )
            }

            composable(Screen.LastFmTest.route) {
                val viewModel: LastFmViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return LastFmViewModel(AppContainer.trackRepository, authRepo) as T
                        }
                    }
                )

                LastFmScreen(
                    viewModel = viewModel,
                    authRepo = authRepo,
                    onNavToProfile = { navController.navigate(Screen.Profile.route) },
                    onSignOut = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Profile.route) {
                ManageProfileScreen(
                    authRepo = authRepo,
                    onSuccess = { navController.navigate(Screen.LastFmTest.route) }
                )
            }
        }
    }
}
