package licenta.soundaround.core

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import licenta.soundaround.auth.data.AuthRepository
import licenta.soundaround.map.data.MapRepository
import licenta.soundaround.map.presentation.MapScreen
import licenta.soundaround.map.presentation.MapViewModel
import licenta.soundaround.music.domain.repository.MusicRepository
import licenta.soundaround.music.presentation.LastFmScreen
import licenta.soundaround.music.presentation.LastFmViewModel
import licenta.soundaround.presence.data.PresenceRepository
import licenta.soundaround.social.data.SocialRepository
import licenta.soundaround.social.presentation.ChatScreen
import licenta.soundaround.social.presentation.ChatViewModel
import licenta.soundaround.social.presentation.ConversationsScreen
import licenta.soundaround.social.presentation.ConversationsViewModel

private const val TAB_NOW_PLAYING = "now_playing"
private const val TAB_MAP = "map"
private const val TAB_CHATS = "chats"
private const val SCREEN_CHAT = "chat/{conversationId}?otherUsername={otherUsername}&isPersistent={isPersistent}&otherUserId={otherUserId}"

@Composable
fun MainScreen(
    authRepo: AuthRepository,
    trackRepository: MusicRepository,
    presenceRepository: PresenceRepository,
    mapRepository: MapRepository,
    socialRepository: SocialRepository,
    onNavToProfile: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val innerNav = rememberNavController()
    val backStack by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val scope = rememberCoroutineScope()

    // Only show bottom bar on tab screens
    val showBottomBar = currentRoute in listOf(TAB_NOW_PLAYING, TAB_MAP, TAB_CHATS)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == TAB_NOW_PLAYING,
                        onClick = { innerNav.navigate(TAB_NOW_PLAYING) { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
                        label = { Text("Now Playing") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == TAB_MAP,
                        onClick = { innerNav.navigate(TAB_MAP) { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                        label = { Text("Map") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == TAB_CHATS,
                        onClick = { innerNav.navigate(TAB_CHATS) { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Chat, contentDescription = null) },
                        label = { Text("Chats") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNav,
            startDestination = TAB_NOW_PLAYING,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(TAB_NOW_PLAYING) {
                val vm: LastFmViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return LastFmViewModel(
                                trackRepository,
                                authRepo,
                                presenceRepository,
                                AppContainer.locationProvider
                            ) as T
                        }
                    }
                )
                LastFmScreen(
                    viewModel = vm,
                    authRepo = authRepo,
                    onNavToProfile = onNavToProfile,
                    onSignOut = onSignOut
                )
            }

            composable(TAB_MAP) {
                val vm: MapViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return MapViewModel(mapRepository, AppContainer.locationProvider) as T
                        }
                    }
                )
                MapScreen(
                    viewModel = vm,
                    onPing = { user ->
                        scope.launch {
                            try {
                                val conversationId = socialRepository.sendPing(
                                    toUserId = user.userId,
                                    trackTitle = user.trackName,
                                    trackArtist = user.artistName
                                )
                                if (conversationId != null) {
                                    val username = user.username ?: ""
                                    innerNav.navigate("chat/$conversationId?otherUsername=$username&isPersistent=false&otherUserId=${user.userId}")
                                } else {
                                    Toast.makeText(context, "Could not start chat. Try again.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "No internet connection.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }

            composable(TAB_CHATS) {
                val vm: ConversationsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ConversationsViewModel(socialRepository) as T
                        }
                    }
                )
                ConversationsScreen(
                    viewModel = vm,
                    onOpenChat = { conversation ->
                        innerNav.navigate(
                            "chat/${conversation.id}?otherUsername=${conversation.otherUsername}&isPersistent=${conversation.isPersistent}&otherUserId=${conversation.otherUserId}"
                        )
                    }
                )
            }

            composable(
                route = SCREEN_CHAT,
                arguments = listOf(
                    navArgument("conversationId") { type = NavType.StringType },
                    navArgument("otherUsername") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("isPersistent") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("otherUserId") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
                val otherUsername = backStackEntry.arguments?.getString("otherUsername") ?: ""
                val isPersistent = backStackEntry.arguments?.getBoolean("isPersistent") ?: false
                val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
                val currentUserId = authRepo.getCurrentUser()?.id ?: ""

                val vm: ChatViewModel = viewModel(
                    key = conversationId,
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ChatViewModel(
                                socialRepository,
                                conversationId,
                                currentUserId,
                                otherUserId,
                                isPersistent
                            ) as T
                        }
                    }
                )
                ChatScreen(
                    viewModel = vm,
                    otherUsername = otherUsername,
                    onBack = { innerNav.popBackStack() }
                )
            }
        }
    }
}
