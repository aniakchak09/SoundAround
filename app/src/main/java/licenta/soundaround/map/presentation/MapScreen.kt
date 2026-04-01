package licenta.soundaround.map.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import licenta.soundaround.BuildConfig
import licenta.soundaround.core.formatRelativeTime
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showNearbyDropdown by remember { mutableStateOf(false) }

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

    // Center camera on own GPS location as soon as map + location are both ready
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
                        map.setStyle(STYLE_URL) {
                            mapRef.value = map
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp)
        ) {
            // Nearby pill
            Surface(
                shape = RoundedCornerShape(50),
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.clickable(enabled = viewModel.users.isNotEmpty()) {
                    showNearbyDropdown = true
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Map,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
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
                }
            }

            // Dropdown — filter chips at the top, then the user list
            DropdownMenu(
                expanded = showNearbyDropdown,
                onDismissRequest = { showNearbyDropdown = false }
            ) {
                // Filter chips row
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MatchFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = viewModel.matchFilter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = {
                                Text(filter.label, style = MaterialTheme.typography.labelSmall)
                            }
                        )
                    }
                }
                if (filteredUsers.isEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "No users match this filter",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { showNearbyDropdown = false }
                    )
                } else {
                    filteredUsers.forEach { user ->
                        val score = viewModel.compatibilityScores[user.userId]
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "@${user.username ?: user.userId}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (user.trackName != null) {
                                            Text(
                                                text = "${if (user.isPlaying) "▶ " else ""}${user.trackName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (score != null) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "${(score * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                score >= 0.55f -> MaterialTheme.colorScheme.primary
                                                score >= 0.30f -> MaterialTheme.colorScheme.secondary
                                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            },
                            onClick = {
                                showNearbyDropdown = false
                                mapRef.value?.cameraPosition = CameraPosition.Builder()
                                    .target(LatLng(user.lat, user.lng))
                                    .zoom(15.0)
                                    .build()
                            }
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

    viewModel.selectedUser?.let { user ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissUser() },
            sheetState = sheetState
        ) {
            UserCard(
                user = user,
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
private fun UserCard(
    user: UserLocation,
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
            .padding(horizontal = 24.dp)
            .padding(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "@${user.username ?: user.userId}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onOpenProfile() }
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Column {
                val timeLabel = if (user.isPlaying) {
                    "Now playing"
                } else {
                    val rel = user.lastSeenAt?.let { formatRelativeTime(it) }
                    if (rel.isNullOrEmpty()) "Last played" else "Last played · $rel"
                }
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (user.isPlaying) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = user.trackName ?: "Unknown track",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = user.artistName ?: "Unknown artist",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "Tap username to view full profile",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        if (isPreviewLoading || previewUrl != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
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
                            text = if (isPreviewPlaying) "Playing preview" else "30s preview",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "iTunes · 30 seconds",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        if (existingConversationId != null) {
            Button(
                onClick = onGoToConversation,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Go to Conversation")
            }
        } else {
            Button(
                onClick = onPing,
                modifier = Modifier.fillMaxWidth()
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

    // Own location marker — always at GPS position, distinct blue circle
    if (ownLocation != null) {
        map.addMarker(
            MarkerOptions()
                .position(LatLng(ownLocation.first, ownLocation.second))
                .title("You")
                .icon(IconFactory.getInstance(context).fromBitmap(createOwnMarkerBitmap()))
        )
    }

    // Other users — skip current user if they happen to appear in the list
    return users
        .filter { it.userId != currentUserId }
        .associate { user ->
            val marker: Marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(user.lat, user.lng))
                    .title(user.username ?: user.userId)
                    .snippet(if (user.isPlaying) "▶ ${user.trackName}" else "⏸ ${user.trackName}")
            )
            marker.id to user
        }
}

private fun createOwnMarkerBitmap(): Bitmap {
    val size = 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val cy = size / 2f
    val radius = size / 2f - 5f

    canvas.drawCircle(cx, cy, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(25, 118, 210) // blue
        style = Paint.Style.FILL
    })
    canvas.drawCircle(cx, cy, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    })
    return bitmap
}
