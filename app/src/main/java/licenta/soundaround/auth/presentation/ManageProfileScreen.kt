package licenta.soundaround.auth.presentation

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import licenta.soundaround.auth.domain.model.VisibilityMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import licenta.soundaround.auth.data.AuthRepository

@Composable
fun ManageProfileScreen(
    authRepo: AuthRepository,
    onSuccess: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var lastFmUsername by rememberSaveable { mutableStateOf("") }
    var visibilityMode by rememberSaveable { mutableStateOf(VisibilityMode.PUBLIC) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val session = authRepo.getCurrentSession()
        if (session != null) {
            val profile = authRepo.getProfile()
            Log.d("ManageProfileScreen", "Profile: $profile")
            username = profile?.username ?: ""
            bio = profile?.bio ?: ""
            lastFmUsername = profile?.lastFmUsername ?: ""
            visibilityMode = authRepo.getVisibilityMode()
        }
        Log.d("ManageProfileScreen", "Session: $session")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Edit Profile",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Update your information",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(36.dp))

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
                        val lastFmRes = authRepo.updateLastFm(lastFmUsername)
                        if (!lastFmRes) allSuccessful = false
                    }

                    if (allSuccessful) {
                        authRepo.updateVisibilityMode(visibilityMode)
                        Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    }
                }
            },
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Save Changes", style = MaterialTheme.typography.labelLarge)
        }
    }
}
