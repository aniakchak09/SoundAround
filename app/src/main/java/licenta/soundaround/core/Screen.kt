package licenta.soundaround.core

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object LastFmTest : Screen("lastfm_test")
    object Profile : Screen("profile")
}