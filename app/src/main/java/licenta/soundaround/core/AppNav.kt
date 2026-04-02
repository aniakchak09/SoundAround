package licenta.soundaround.core

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import licenta.soundaround.auth.data.AuthRepository
import licenta.soundaround.auth.presentation.LoginScreen
import licenta.soundaround.auth.presentation.ManageProfileScreen
import licenta.soundaround.auth.presentation.OnboardingScreen
import licenta.soundaround.auth.presentation.SignUpScreen
import licenta.soundaround.map.data.MapRepository
import licenta.soundaround.map.data.MatchingRepository
import licenta.soundaround.music.RetrofitClient
import licenta.soundaround.music.RetrofitItunesClient
import licenta.soundaround.music.data.MusicRepositoryImpl
import licenta.soundaround.presence.data.PresenceRepository
import licenta.soundaround.social.data.SocialRepository

object AppContainer {
    lateinit var locationProvider: LocationProvider
        private set

    fun init(context: Context) {
        locationProvider = LocationProvider(context)
    }

    val authRepository: AuthRepository by lazy { AuthRepository() }

    val trackRepository: MusicRepositoryImpl by lazy {
        MusicRepositoryImpl(RetrofitClient.lastFmService, RetrofitItunesClient.itunesService)
    }

    val presenceRepository: PresenceRepository by lazy { PresenceRepository() }

    val mapRepository: MapRepository by lazy { MapRepository() }

    val socialRepository: SocialRepository by lazy { SocialRepository() }

    val matchingRepository: MatchingRepository by lazy { MatchingRepository(trackRepository) }
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val authRepo = AppContainer.authRepository
    var startRoute by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val isLoggedIn = authRepo.isUserLoggedIn()
        startRoute = if (isLoggedIn) Screen.Main.route else Screen.Login.route
    }

    startRoute?.let { destination ->
        Log.d("AppNav", "Starting at route: $destination")
        NavHost(navController = navController, startDestination = destination) {
            composable(Screen.Login.route) {
                LoginScreen(
                    authRepo = authRepo,
                    onLoginSuccess = { navController.navigate(Screen.Main.route) },
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
                )
            }

            composable(Screen.SignUp.route) {
                SignUpScreen(
                    authRepo = authRepo,
                    onSignUpSuccess = { navController.navigate(Screen.Onboarding.route) },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Main.route) {
                MainScreen(
                    authRepo = authRepo,
                    trackRepository = AppContainer.trackRepository,
                    presenceRepository = AppContainer.presenceRepository,
                    mapRepository = AppContainer.mapRepository,
                    socialRepository = AppContainer.socialRepository,
                    matchingRepository = AppContainer.matchingRepository,
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
                    onSuccess = { navController.popBackStack() }
                )
            }

        }
    }
}
