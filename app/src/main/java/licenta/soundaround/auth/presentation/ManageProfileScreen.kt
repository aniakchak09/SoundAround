package licenta.soundaround.auth.presentation

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import licenta.soundaround.auth.data.AuthRepository
import licenta.soundaround.auth.domain.model.VisibilityMode

@Composable
fun ManageProfileScreen(
    authRepo: AuthRepository,
    onSuccess: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var lastFmUsername by rememberSaveable { mutableStateOf("") }
    var visibilityMode by rememberSaveable { mutableStateOf(VisibilityMode.PUBLIC) }
    var avatarUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var isUploadingAvatar by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            isUploadingAvatar = true
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                if (bytes != null) {
                    val url = authRepo.uploadAvatar(bytes)
                    if (url != null) {
                        authRepo.updateAvatarUrl(url)
                        avatarUrl = url
                        Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Upload failed. Check your connection.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not read image.", Toast.LENGTH_SHORT).show()
            }
            isUploadingAvatar = false
        }
    }

    LaunchedEffect(Unit) {
        val session = authRepo.getCurrentSession()
        if (session != null) {
            val profile = authRepo.getProfile()
            Log.d("ManageProfileScreen", "Profile: $profile")
            username = profile?.username ?: ""
            bio = profile?.bio ?: ""
            lastFmUsername = profile?.lastFmUsername ?: ""
            avatarUrl = profile?.avatarUrl
            visibilityMode = authRepo.getVisibilityMode()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // Avatar
        Box(contentAlignment = Alignment.BottomEnd) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Profile picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(96.dp).clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }
            if (isUploadingAvatar) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp
                )
            } else {
                SmallFloatingActionButton(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.size(32.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = "Change photo", modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Edit Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio") },
            minLines = 2,
            maxLines = 3,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = lastFmUsername,
            onValueChange = { lastFmUsername = it },
            label = { Text("Last.fm username") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            supportingText = { Text("Used to show your currently playing track") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Visibility on map",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                VisibilityMode.PUBLIC to "Public",
                VisibilityMode.FRIENDS_ONLY to "Friends",
                VisibilityMode.INVISIBLE to "Invisible"
            ).forEach { (mode, label) ->
                FilterChip(
                    selected = visibilityMode == mode,
                    onClick = { visibilityMode = mode },
                    label = { Text(label, maxLines = 1) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = when (visibilityMode) {
                VisibilityMode.PUBLIC -> "Everyone nearby can see you on the map"
                VisibilityMode.FRIENDS_ONLY -> "Only friends can see you on the map"
                VisibilityMode.INVISIBLE -> "You won't appear on anyone's map"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                scope.launch {
                    var allSuccessful = true
                    if (username.isNotBlank()) {
                        val error = authRepo.updateUsername(username)
                        if (error != null) {
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            allSuccessful = false
                        }
                    }
                    val bioRes = authRepo.updateBio(bio)
                    if (!bioRes) allSuccessful = false
                    if (lastFmUsername.isNotBlank()) {
                        val lastFmError = authRepo.updateLastFm(lastFmUsername)
                        if (lastFmError != null) {
                            Toast.makeText(context, lastFmError, Toast.LENGTH_LONG).show()
                            allSuccessful = false
                        }
                    }
                    if (allSuccessful) {
                        authRepo.updateVisibilityMode(visibilityMode)
                        Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    }
                }
            },
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Save Changes", style = MaterialTheme.typography.labelLarge)
        }

        }
    }
}
