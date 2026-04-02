package licenta.soundaround.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import licenta.soundaround.music.data.TopArtistDto
import licenta.soundaround.music.data.TopTrackDto
import licenta.soundaround.music.domain.model.Track
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MyProfileScreen(
    viewModel: MyProfileViewModel,
    onEditProfile: () -> Unit,
    onSignOut: () -> Unit,
    onViewFriendProfile: (userId: String, username: String) -> Unit,
    onGoToFriends: () -> Unit
) {
    val profile = viewModel.profile
    var showAvatarDialog by remember { mutableStateOf(false) }

    if (showAvatarDialog && !profile?.avatarUrl.isNullOrBlank()) {
        Dialog(onDismissRequest = { showAvatarDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAvatarDialog = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = profile!!.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Header — centered, large avatar
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clickable(enabled = !profile?.avatarUrl.isNullOrBlank()) { showAvatarDialog = true }
                ) {
                    if (!profile?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile!!.avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        val letter = profile?.username?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                letter,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    "@${profile?.username ?: ""}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                if (!profile?.bio.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        profile!!.bio!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                viewModel.joinDate?.let { dateStr ->
                    val formatted = formatJoinDate(dateStr)
                    if (formatted != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Joined $formatted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onEditProfile,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit Profile", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Friends card — right after header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onGoToFriends() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Friends",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        val pendingCount = viewModel.pendingRequests.size
                        val subtitle = buildString {
                            append("${viewModel.friends.size} friend${if (viewModel.friends.size != 1) "s" else ""}")
                            if (pendingCount > 0) append(" · $pendingCount pending")
                        }
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Icon(
                        Icons.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // Last.fm stats card
        viewModel.lastFmInfo?.let { info ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(label = "Scrobbles", value = formatCount(info.playcount))
                        if (!profile?.lastFmUsername.isNullOrBlank()) {
                            StatItem(label = "last.fm", value = "@${profile!!.lastFmUsername}")
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        // Top Artists
        if (viewModel.topArtists.isNotEmpty()) {
            item {
                SectionLabel("Top Artists")
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.topArtists) { artist ->
                        ArtistItem(artist, viewModel.artistImages[artist.name])
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

        // Top Tracks
        if (viewModel.topTracks.isNotEmpty()) {
            item { SectionLabel("Top Tracks") }
            items(viewModel.topTracks) { track ->
                TopTrackItem(
                    track,
                    viewModel.trackImages["${track.name}_${track.artist.name}"],
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        // Recent Tracks
        if (viewModel.recentTracks.isNotEmpty()) {
            item { SectionLabel("Recent Tracks") }
            items(viewModel.recentTracks) { track ->
                RecentTrackItem(track, modifier = Modifier.padding(horizontal = 16.dp))
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        // Sign out
        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onSignOut,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text("Sign Out")
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ArtistItem(artist: TopArtistDto, imageUrl: String?) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                )
            }
            Column {
                Text(
                    artist.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${formatCount(artist.playcount)} plays",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TopTrackItem(track: TopTrackDto, imageUrl: String?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(track.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist.name, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text("${formatCount(track.playcount)} plays", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}

@Composable
private fun RecentTrackItem(track: Track, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (track.imageUrl.isNotBlank()) {
            AsyncImage(model = track.imageUrl, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (track.isNowPlaying) {
            Text("▶ now", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatJoinDate(dateStr: String): String? {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(dateStr.take(19)) ?: return null
        SimpleDateFormat("MMM yyyy", Locale.US).format(date)
    } catch (_: Exception) { null }
}

private fun formatCount(count: String): String {
    val n = count.toLongOrNull() ?: return count
    return when {
        n >= 1_000_000 -> "${n / 1_000_000}M"
        n >= 1_000 -> "${n / 1_000}k"
        else -> n.toString()
    }
}
