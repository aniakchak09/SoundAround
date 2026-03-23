package licenta.soundaround.core

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import licenta.soundaround.auth.presentation.MyProfileScreen
import licenta.soundaround.auth.presentation.MyProfileViewModel
import licenta.soundaround.map.data.MapRepository
import licenta.soundaround.map.presentation.MapScreen
import licenta.soundaround.map.presentation.MapViewModel
import licenta.soundaround.music.domain.repository.MusicRepository
import licenta.soundaround.music.presentation.LastFmScreen
import licenta.soundaround.music.presentation.LastFmViewModel
import licenta.soundaround.music.presentation.UserProfileScreen
import licenta.soundaround.music.presentation.UserProfileViewModel
import licenta.soundaround.presence.data.PresenceRepository
import licenta.soundaround.social.data.SocialRepository
import licenta.soundaround.social.presentation.ChatScreen
import licenta.soundaround.social.presentation.ChatViewModel
import licenta.soundaround.social.presentation.ConversationsScreen
import licenta.soundaround.social.presentation.ConversationsViewModel

private const val TAB_NOW_PLAYING = "now_playing"
private const val TAB_MAP = "map"
private const val TAB_CHATS = "chats"
private const val TAB_PROFILE = "profile_tab"
private const val SCREEN_CHAT = "chat/{conversationId}?otherUsername={otherUsername}&isPersistent={isPersistent}&otherUserId={otherUserId}&myTrack={myTrack}&myArtist={myArtist}&theirTrack={theirTrack}&theirArtist={theirArtist}"
private const val SCREEN_USER_PROFILE = "user_profile/{userId}?username={username}"

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

    val conversationsVm: ConversationsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ConversationsViewModel(socialRepository) as T
            }
        }
    )

    // Only show bottom bar on tab screens
    val showBottomBar = currentRoute in listOf(TAB_NOW_PLAYING, TAB_MAP, TAB_CHATS, TAB_PROFILE)

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
                        icon = {
                            BadgedBox(badge = {
                                if (conversationsVm.unreadCount > 0) {
                                    Badge { Text(conversationsVm.unreadCount.toString()) }
                                }
                            }) {
                                Icon(Icons.Filled.Chat, contentDescription = null)
                            }
                        },
                        label = { Text("Chats") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == TAB_PROFILE,
                        onClick = { innerNav.navigate(TAB_PROFILE) { launchSingleTop = true } },
                        icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        label = { Text("Profile") }
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
                LastFmScreen(viewModel = vm)
            }

            composable(TAB_MAP) {
                val vm: MapViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return MapViewModel(mapRepository, AppContainer.locationProvider, trackRepository, socialRepository) as T
                        }
                    }
                )
                MapScreen(
                    viewModel = vm,
                    onPing = { user ->
                        scope.launch {
                            try {
                                val myUser = vm.users.find { it.userId == vm.currentUserId }
                                val conversationId = socialRepository.sendPing(
                                    toUserId = user.userId,
                                    trackTitle = user.trackName,
                                    trackArtist = user.artistName,
                                    myTrackTitle = myUser?.trackName,
                                    myTrackArtist = myUser?.artistName
                                )
                                if (conversationId != null) {
                                    val username = user.username ?: ""
                                    innerNav.navigate(
                                        "chat/$conversationId" +
                                        "?otherUsername=$username" +
                                        "&isPersistent=false" +
                                        "&otherUserId=${user.userId}" +
                                        "&myTrack=${myUser?.trackName ?: ""}" +
                                        "&myArtist=${myUser?.artistName ?: ""}" +
                                        "&theirTrack=${user.trackName ?: ""}" +
                                        "&theirArtist=${user.artistName ?: ""}"
                                    )
                                } else {
                                    Toast.makeText(context, "Could not start chat. Try again.", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "No internet connection.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onGoToConversation = { conversationId, otherUsername, isPersistent, otherUserId ->
                        innerNav.navigate("chat/$conversationId?otherUsername=$otherUsername&isPersistent=$isPersistent&otherUserId=$otherUserId")
                    },
                    onOpenUserProfile = { userId, username ->
                        innerNav.navigate("user_profile/$userId?username=$username")
                    }
                )
            }

            composable(TAB_CHATS) {
                ConversationsScreen(
                    viewModel = conversationsVm,
                    onOpenChat = { conversation ->
                        innerNav.navigate(
                            "chat/${conversation.id}" +
                            "?otherUsername=${conversation.otherUsername}" +
                            "&isPersistent=${conversation.isPersistent}" +
                            "&otherUserId=${conversation.otherUserId}" +
                            "&myTrack=${conversation.myInitialTrackTitle ?: ""}" +
                            "&myArtist=${conversation.myInitialTrackArtist ?: ""}" +
                            "&theirTrack=${conversation.theirInitialTrackTitle ?: ""}" +
                            "&theirArtist=${conversation.theirInitialTrackArtist ?: ""}"
                        )
                    }
                )
            }

            composable(TAB_PROFILE) {
                val vm: MyProfileViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return MyProfileViewModel(authRepo, trackRepository, socialRepository) as T
                        }
                    }
                )
                MyProfileScreen(
                    viewModel = vm,
                    onEditProfile = onNavToProfile,
                    onSignOut = {
                        scope.launch { authRepo.signOut(); onSignOut() }
                    },
                    onViewFriendProfile = { userId, username ->
                        innerNav.navigate("user_profile/$userId?username=$username")
                    }
                )
            }

            composable(
                route = SCREEN_USER_PROFILE,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("username") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                val username = backStackEntry.arguments?.getString("username") ?: ""
                val vm: UserProfileViewModel = viewModel(
                    key = userId,
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return UserProfileViewModel(
                                userId,
                                username,
                                AppContainer.mapRepository,
                                AppContainer.trackRepository
                            ) as T
                        }
                    }
                )
                UserProfileScreen(viewModel = vm, onBack = { innerNav.popBackStack() })
            }

            composable(
                route = SCREEN_CHAT,
                arguments = listOf(
                    navArgument("conversationId") { type = NavType.StringType },
                    navArgument("otherUsername") { type = NavType.StringType; defaultValue = "" },
                    navArgument("isPersistent") { type = NavType.BoolType; defaultValue = false },
                    navArgument("otherUserId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("myTrack") { type = NavType.StringType; defaultValue = "" },
                    navArgument("myArtist") { type = NavType.StringType; defaultValue = "" },
                    navArgument("theirTrack") { type = NavType.StringType; defaultValue = "" },
                    navArgument("theirArtist") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val args = backStackEntry.arguments
                val conversationId = args?.getString("conversationId") ?: return@composable
                val otherUsername = args.getString("otherUsername") ?: ""
                val isPersistent = args.getBoolean("isPersistent")
                val otherUserId = args.getString("otherUserId") ?: ""
                val myTrack = args.getString("myTrack")?.takeIf { it.isNotBlank() }
                val myArtist = args.getString("myArtist")?.takeIf { it.isNotBlank() }
                val theirTrack = args.getString("theirTrack")?.takeIf { it.isNotBlank() }
                val theirArtist = args.getString("theirArtist")?.takeIf { it.isNotBlank() }
                val currentUserId = authRepo.getCurrentUser()?.id ?: ""

                val vm: ChatViewModel = viewModel(
                    key = conversationId,
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ChatViewModel(
                                socialRepository, conversationId, currentUserId, otherUserId,
                                isPersistent, myTrack, myArtist, theirTrack, theirArtist
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
