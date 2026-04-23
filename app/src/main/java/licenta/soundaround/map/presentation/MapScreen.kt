package licenta.soundaround.map.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import licenta.soundaround.BuildConfig
import licenta.soundaround.core.avatarColor
import licenta.soundaround.core.formatRelativeTime
import licenta.soundaround.core.minutesSinceLastSeen
import licenta.soundaround.map.domain.model.UserLocation
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

private const val STYLE_URL =
    "https://api.maptiler.com/maps/streets-v2/style.json?key=${BuildConfig.MAPTILER_API_KEY}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onPing: (UserLocation) -> Unit,
    onGoToConversation: (conversationId: String, otherUsername: String, isPersistent: Boolean, otherUserId: String) -> Unit,
    onOpenUserProfile: (userId: String, username: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val filteredUsers = viewModel.filteredUsers

    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> locationPermissionGranted = granted }

    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    MapLibre.getInstance(context)
    val mapView = remember { MapView(context) }
    val mapRef = remember { mutableStateOf<MapLibreMap?>(null) }
    val userCardSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val nearbySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showNearbySheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(mapRef.value, viewModel.ownLocation) {
        val map = mapRef.value ?: return@LaunchedEffect
        val loc = viewModel.ownLocation ?: return@LaunchedEffect
        map.cameraPosition = CameraPosition.Builder()
            .target(LatLng(loc.first, loc.second))
            .zoom(14.0)
            .build()
    }

    LaunchedEffect(mapRef.value, viewModel.users, viewModel.ownLocation) {
        val map = mapRef.value ?: return@LaunchedEffect
        val markerMap = updateMarkers(
            map = map,
            users = viewModel.users,
            currentUserId = viewModel.currentUserId,
            ownLocation = viewModel.ownLocation,
            context = context
        )
        map.setOnMarkerClickListener { marker ->
            markerMap[marker.id]?.let { viewModel.selectUser(it) }
            true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        map.setStyle(STYLE_URL) { mapRef.value = map }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Nearby pill — opens bottom sheet
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.clickable(enabled = viewModel.users.isNotEmpty()) {
                    showNearbySheet = true
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (viewModel.users.isNotEmpty()) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            viewModel.users.isEmpty() -> "No listeners nearby"
                            viewModel.matchFilter != MatchFilter.ALL ->
                                "${filteredUsers.size}/${viewModel.users.size} listeners"
                            else -> "${viewModel.users.size} listener${if (viewModel.users.size != 1) "s" else ""} nearby"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (viewModel.isComputingScores) {
                        Spacer(Modifier.width(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                    }
                    if (viewModel.users.isNotEmpty()) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "›",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                        )
                    }
                }
            }
        }

        if (viewModel.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        FloatingActionButton(
            onClick = { viewModel.refresh() },
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
        }
    }

    // Nearby users bottom sheet
    if (showNearbySheet) {
        ModalBottomSheet(
            onDismissRequest = { showNearbySheet = false },
            sheetState = nearbySheetState
        ) {
            NearbyUsersSheet(
                filteredUsers = filteredUsers,
                allUsersCount = viewModel.users.size,
                matchFilter = viewModel.matchFilter,
                compatibilityScores = viewModel.compatibilityScores,
                isComputingScores = viewModel.isComputingScores,
                onFilterChange = { viewModel.setFilter(it) },
                onLocateUser = { user ->
                    showNearbySheet = false
                    mapRef.value?.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(user.lat, user.lng))
                        .zoom(15.0)
                        .build()
                    viewModel.selectUser(user)
                }
            )
        }
    }

    // Selected user card bottom sheet
    viewModel.selectedUser?.let { user ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissUser() },
            sheetState = userCardSheetState
        ) {
            UserCard(
                user = user,
                compatibilityScore = viewModel.compatibilityScores[user.userId],
                bio = viewModel.selectedUserBio,
                lastFmUsername = viewModel.selectedUserLastFm,
                recentTracks = viewModel.selectedUserRecentTracks,
                existingConversationId = viewModel.existingConversation?.id,
                previewUrl = viewModel.previewUrl,
                isPreviewLoading = viewModel.isPreviewLoading,
                isPreviewPlaying = viewModel.isPreviewPlaying,
                onTogglePreview = { viewModel.togglePreview() },
                onPing = {
                    viewModel.dismissUser()
                    onPing(user)
                },
                onGoToConversation = {
                    val conv = viewModel.existingConversation ?: return@UserCard
                    viewModel.dismissUser()
                    onGoToConversation(conv.id, conv.otherUsername, conv.isPersistent, conv.otherUserId)
                },
                onOpenProfile = {
                    val username = user.username ?: user.userId
                    viewModel.dismissUser()
                    onOpenUserProfile(user.userId, username)
                }
            )
        }
    }
}

@Composable
private fun NearbyUsersSheet(
    filteredUsers: List<UserLocation>,
    allUsersCount: Int,
    matchFilter: MatchFilter,
    compatibilityScores: Map<String, Float>,
    isComputingScores: Boolean,
    onFilterChange: (MatchFilter) -> Unit,
    onLocateUser: (UserLocation) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Listeners Nearby",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (isComputingScores) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                "$allUsersCount total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            items(MatchFilter.entries) { filter ->
                FilterChip(
                    selected = matchFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No users match this filter",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(filteredUsers, key = { it.userId }) { user ->
                    NearbyUserSheetRow(
                        user = user,
                        score = compatibilityScores[user.userId],
                        onClick = { onLocateUser(user) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
}

@Composable
private fun NearbyUserSheetRow(
    user: UserLocation,
    score: Float?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val avatarBg = avatarColor(user.username ?: user.userId)
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(avatarBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                (user.username ?: user.userId).firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "@${user.username ?: user.userId}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (user.trackName != null) {
                Text(
                    "${if (user.isPlaying) "▶ " else ""}${user.trackName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!user.isPlaying) {
                val rel = user.lastSeenAt?.let { formatRelativeTime(it) }
                if (!rel.isNullOrEmpty()) {
                    Text(
                        "last seen $rel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
        if (score != null) {
            Spacer(Modifier.width(10.dp))
            val percent = (score * 100).toInt()
            val scoreColor = when {
                percent >= 55 -> MaterialTheme.colorScheme.primary
                percent >= 30 -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = scoreColor.copy(alpha = 0.15f)
            ) {
                Text(
                    "$percent%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun UserCard(
    user: UserLocation,
    compatibilityScore: Float?,
    bio: String?,
    lastFmUsername: String?,
    recentTracks: List<licenta.soundaround.music.domain.model.Track>,
    existingConversationId: String?,
    previewUrl: String?,
    isPreviewLoading: Boolean,
    isPreviewPlaying: Boolean,
    onTogglePreview: () -> Unit,
    onPing: () -> Unit,
    onGoToConversation: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header: avatar + name + bio + profile button
        Row(verticalAlignment = Alignment.CenterVertically) {
            val avatarBg = avatarColor(user.username ?: user.userId)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(avatarBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (user.username ?: user.userId).firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "@${user.username ?: user.userId}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!bio.isNullOrBlank()) {
                    Text(
                        bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onOpenProfile) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = "View full profile",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Compatibility score bar
        if (compatibilityScore != null) {
            val percent = (compatibilityScore * 100).toInt()
            val scoreColor = when {
                percent >= 55 -> MaterialTheme.colorScheme.primary
                percent >= 30 -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Music compatibility",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "$percent% match",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                }
                LinearProgressIndicator(
                    progress = { compatibilityScore.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = scoreColor,
                    trackColor = scoreColor.copy(alpha = 0.15f)
                )
            }
        }

        // Track card
        if (user.trackName != null || user.albumArt != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (user.albumArt != null) {
                        AsyncImage(
                            model = user.albumArt,
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val timeLabel = if (user.isPlaying) "Now playing"
                        else {
                            val rel = user.lastSeenAt?.let { formatRelativeTime(it) }
                            if (rel.isNullOrEmpty()) "Last played" else "Last played · $rel"
                        }
                        Text(
                            timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (user.isPlaying) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (user.isPlaying) FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(
                            user.trackName ?: "Unknown track",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (user.artistName != null) {
                            Text(
                                user.artistName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Audio preview
        if (isPreviewLoading || previewUrl != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPreviewLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Loading preview…",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        IconButton(onClick = onTogglePreview, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (isPreviewPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPreviewPlaying) "Pause" else "Play",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isPreviewPlaying) "Playing preview" else "30s preview",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "iTunes · 30 seconds",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }

        // Action button
        if (existingConversationId != null) {
            Button(
                onClick = onGoToConversation,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Go to Conversation")
            }
        } else {
            Button(
                onClick = onPing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Send Ping")
            }
        }
    }
}

private fun updateMarkers(
    map: MapLibreMap,
    users: List<UserLocation>,
    currentUserId: String?,
    ownLocation: Pair<Double, Double>?,
    context: Context
): Map<Long, UserLocation> {
    map.markers.toList().forEach { map.removeMarker(it) }

    if (ownLocation != null) {
        map.addMarker(
            MarkerOptions()
                .position(LatLng(ownLocation.first, ownLocation.second))
                .title("You")
                .icon(IconFactory.getInstance(context).fromBitmap(createOwnMarkerBitmap()))
        )
    }

    return users
        .filter { it.userId != currentUserId }
        .associate { user ->
            val initial = (user.username ?: user.userId)
                .firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            val bgColor = avatarColor(user.username ?: user.userId).toArgb()
            val isRecentlyActive = minutesSinceLastSeen(user.lastSeenAt)?.let { it < 15 } ?: false
            val marker: Marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(user.lat, user.lng))
                    .title(user.username ?: user.userId)
                    .snippet(if (user.isPlaying) "▶ ${user.trackName}" else "${user.trackName ?: ""}")
                    .icon(
                        IconFactory.getInstance(context).fromBitmap(
                            createUserMarkerBitmap(initial, bgColor, user.isPlaying, isRecentlyActive)
                        )
                    )
            )
            marker.id to user
        }
}

private fun createUserMarkerBitmap(initial: String, bgColor: Int, isPlaying: Boolean, isRecentlyActive: Boolean = false): Bitmap {
    val size = 88
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f
    val radius = size / 2f - 5f

    canvas.drawCircle(cx, cy, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bgColor
        style = Paint.Style.FILL
    })
    canvas.drawCircle(cx, cy, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    })

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = size * 0.36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    val bounds = Rect()
    textPaint.getTextBounds(initial, 0, initial.length, bounds)
    canvas.drawText(initial, cx, cy - (bounds.top + bounds.bottom) / 2f, textPaint)

    val dotColor = when {
        isPlaying -> android.graphics.Color.rgb(76, 175, 80)   // green
        isRecentlyActive -> android.graphics.Color.rgb(255, 152, 0) // orange
        else -> 0
    }
    if (dotColor != 0) {
        val dotR = 11f
        val dotX = size - dotR - 2f
        val dotY = dotR + 2f
        canvas.drawCircle(dotX, dotY, dotR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dotColor
            style = Paint.Style.FILL
        })
        canvas.drawCircle(dotX, dotY, dotR, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        })
    }

    return bitmap
}

private fun createOwnMarkerBitmap(): Bitmap {
    val size = 56
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f
    val radius = size / 2f - 4f

    canvas.drawCircle(cx, cy, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(25, 118, 210)
        style = Paint.Style.FILL
    })
    canvas.drawCircle(cx, cy, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    })
    return bitmap
}
