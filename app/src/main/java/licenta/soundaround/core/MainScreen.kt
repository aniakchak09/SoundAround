package licenta.soundaround.core

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import licenta.soundaround.auth.presentation.FriendsScreen
import licenta.soundaround.auth.presentation.MyProfileScreen
import licenta.soundaround.auth.presentation.MyProfileViewModel
import licenta.soundaround.map.data.MapRepository
import licenta.soundaround.map.data.MatchingRepository
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
private const val SCREEN_CHAT = "chat/{conversationId}?otherUsername={otherUsername}&isPersistent={isPersistent}&otherUserId={otherUserId}&myTrack={myTrack}&myArtist={myArtist}&theirTrack={theirTrack}&theirArtist={theirArtist}&isPendingPing={isPendingPing}"
private const val SCREEN_USER_PROFILE = "user_profile/{userId}?username={username}"
private const val SCREEN_FRIENDS = "friends"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    authRepo: AuthRepository,
    trackRepository: MusicRepository,
    presenceRepository: PresenceRepository,
    mapRepository: MapRepository,
    socialRepository: SocialRepository,
    matchingRepository: MatchingRepository,
    onNavToProfile: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val innerNav = rememberNavController()
    val backStack by innerNav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val conversationsVm: ConversationsViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ConversationsViewModel(socialRepository) as T
            }
        }
    )

    val profileVm: MyProfileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MyProfileViewModel(authRepo, AppContainer.trackRepository, socialRepository) as T
            }
        }
    )

    val showOnTabScreen = currentRoute in listOf(TAB_NOW_PLAYING, TAB_MAP, TAB_CHATS, TAB_PROFILE)

    val tabTitle = when (currentRoute) {
        TAB_NOW_PLAYING -> "Now Playing"
        TAB_MAP -> "Nearby"
        TAB_CHATS -> "Chats"
        TAB_PROFILE -> "Profile"
        else -> "SoundAround"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(avatarColor(profileVm.profile?.username ?: "")),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profileVm.profile?.username?.firstOrNull()?.uppercaseChar()?.toString() ?: "♪",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = profileVm.profile?.username?.let { "@$it" } ?: "SoundAround",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.MusicNote, contentDescription = null) },
                    label = { Text("Now Playing") },
                    selected = currentRoute == TAB_NOW_PLAYING,
                    onClick = {
                        innerNav.navigate(TAB_NOW_PLAYING) { launchSingleTop = true }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Map, contentDescription = null) },
                    label = { Text("Nearby") },
                    selected = currentRoute == TAB_MAP,
                    onClick = {
                        innerNav.navigate(TAB_MAP) { launchSingleTop = true }
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Chat, contentDescription = null) },
                    label = { Text("Chats") },
                    selected = currentRoute == TAB_CHATS,
                    onClick = {
                        innerNav.navigate(TAB_CHATS) { launchSingleTop = true }
                        scope.launch { drawerState.close() }
                    },
                    badge = if (conversationsVm.unreadCount > 0) {
                        { Badge { Text(conversationsVm.unreadCount.toString()) } }
                    } else null,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("Profile") },
                    selected = currentRoute == TAB_PROFILE,
                    onClick = {
                        innerNav.navigate(TAB_PROFILE) { launchSingleTop = true }
                        scope.launch { drawerState.close() }
                    },
                    badge = if (profileVm.pendingRequests.isNotEmpty()) {
                        { Badge { Text(profileVm.pendingRequests.size.toString()) } }
                    } else null,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (showOnTabScreen) {
                    TopAppBar(
                        title = { Text(tabTitle, fontWeight = FontWeight.SemiBold) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Open menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        )
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = innerNav,
                startDestination = TAB_MAP,
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
                                    AppContainer.locationProvider,
                                    mapRepository
                                ) as T
                            }
                        }
                    )
                    LastFmScreen(
                        viewModel = vm,
                        unreadCount = conversationsVm.unreadCount,
                        onGoToChats = { innerNav.navigate(TAB_CHATS) { launchSingleTop = true } },
                        onPingUser = { user ->
                            val conversationId = java.util.UUID.randomUUID().toString()
                            innerNav.navigate(
                                "chat/$conversationId" +
                                "?otherUsername=${user.username ?: ""}" +
                                "&isPersistent=false" +
                                "&otherUserId=${user.userId}" +
                                "&myTrack=&myArtist=" +
                                "&theirTrack=${user.trackName ?: ""}" +
                                "&theirArtist=${user.artistName ?: ""}" +
                                "&isPendingPing=true"
                            )
                        }
                    )
                }

                composable(TAB_MAP) {
                    val vm: MapViewModel = viewModel(
                        factory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return MapViewModel(mapRepository, AppContainer.locationProvider, trackRepository, socialRepository, matchingRepository) as T
                            }
                        }
                    )
                    MapScreen(
                        viewModel = vm,
                        onPing = { user ->
                            val conversationId = java.util.UUID.randomUUID().toString()
                            val myUser = vm.users.find { it.userId == vm.currentUserId }
                            val username = user.username ?: ""
                            innerNav.navigate(
                                "chat/$conversationId" +
                                "?otherUsername=$username" +
                                "&isPersistent=false" +
                                "&otherUserId=${user.userId}" +
                                "&myTrack=${myUser?.trackName ?: ""}" +
                                "&myArtist=${myUser?.artistName ?: ""}" +
                                "&theirTrack=${user.trackName ?: ""}" +
                                "&theirArtist=${user.artistName ?: ""}" +
                                "&isPendingPing=true"
                            )
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
                    MyProfileScreen(
                        viewModel = profileVm,
                        onEditProfile = onNavToProfile,
                        onSignOut = {
                            scope.launch { authRepo.signOut(); onSignOut() }
                        },
                        onViewFriendProfile = { userId, username ->
                            innerNav.navigate("user_profile/$userId?username=$username")
                        },
                        onGoToFriends = { innerNav.navigate(SCREEN_FRIENDS) }
                    )
                }

                composable(SCREEN_FRIENDS) {
                    FriendsScreen(
                        viewModel = profileVm,
                        onBack = { innerNav.popBackStack() },
                        onViewProfile = { userId, username ->
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
                    val currentUserId = authRepo.getCurrentUser()?.id ?: ""
                    val isFriend = profileVm.friends.any { it.first == userId }
                    UserProfileScreen(
                        viewModel = vm,
                        onBack = { innerNav.popBackStack() },
                        isFriend = isFriend,
                        onStartChat = if (userId != currentUserId) {
                            {
                                if (isFriend) {
                                    val existing = conversationsVm.conversations
                                        .find { it.otherUserId == userId && it.isPersistent }
                                    if (existing != null) {
                                        innerNav.navigate(
                                            "chat/${existing.id}" +
                                            "?otherUsername=${existing.otherUsername}" +
                                            "&isPersistent=true" +
                                            "&otherUserId=${existing.otherUserId}" +
                                            "&myTrack=${existing.myInitialTrackTitle ?: ""}" +
                                            "&myArtist=${existing.myInitialTrackArtist ?: ""}" +
                                            "&theirTrack=${existing.theirInitialTrackTitle ?: ""}" +
                                            "&theirArtist=${existing.theirInitialTrackArtist ?: ""}"
                                        )
                                    } else {
                                        val conversationId = java.util.UUID.randomUUID().toString()
                                        innerNav.navigate(
                                            "chat/$conversationId?otherUsername=$username&isPersistent=true&otherUserId=$userId&myTrack=&myArtist=&theirTrack=&theirArtist=&isPendingPing=false"
                                        )
                                    }
                                } else {
                                    val conversationId = java.util.UUID.randomUUID().toString()
                                    innerNav.navigate(
                                        "chat/$conversationId?otherUsername=$username&isPersistent=false&otherUserId=$userId&myTrack=&myArtist=&theirTrack=&theirArtist=&isPendingPing=true"
                                    )
                                }
                            }
                        } else null
                    )
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
                        navArgument("theirArtist") { type = NavType.StringType; defaultValue = "" },
                        navArgument("isPendingPing") { type = NavType.BoolType; defaultValue = false }
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
                    val isPendingPing = args.getBoolean("isPendingPing")
                    val currentUserId = authRepo.getCurrentUser()?.id ?: ""

                    val vm: ChatViewModel = viewModel(
                        key = conversationId,
                        factory = object : ViewModelProvider.Factory {
                            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                return ChatViewModel(
                                    socialRepository, conversationId, currentUserId, otherUserId,
                                    isPersistent, myTrack, myArtist, theirTrack, theirArtist,
                                    isPendingPing
                                ) as T
                            }
                        }
                    )
                    ChatScreen(
                        viewModel = vm,
                        otherUsername = otherUsername,
                        onBack = { innerNav.popBackStack() },
                        onGoToProfile = if (otherUserId.isNotBlank()) {
                            { innerNav.navigate("user_profile/$otherUserId?username=$otherUsername") }
                        } else null
                    )
                }
            }
        }
    }
}
