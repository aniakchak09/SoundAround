package licenta.soundaround.core

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Onboarding : Screen("onboarding")
    object Main : Screen("main")
    object Profile : Screen("profile")
}
