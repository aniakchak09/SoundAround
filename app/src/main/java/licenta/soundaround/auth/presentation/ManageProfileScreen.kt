package licenta.soundaround.auth.presentation

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import licenta.soundaround.auth.data.AuthRepository

// shall change this to profile editing screen in the future, for now it's just a simple last.fm linking step after registration
// maybe something to select genres you like, or artists you like (but I think last.fm already knows that)
@Composable
fun ManageProfileScreen(
    authRepo: AuthRepository,
    onSuccess: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var bio by rememberSaveable { mutableStateOf("") }
    var lastFmUsername by rememberSaveable { mutableStateOf("") }
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
        }
    }

    Column(
        modifier = Modifier
            .padding(24.dp)
            .imePadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Brief description") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = lastFmUsername,
            onValueChange = { lastFmUsername = it },
            label = { Text("Last.fm username") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                scope.launch {
                    var allSuccessful = true

                    // LOGIC: Only update Name if it's NOT empty
                    if (username.isNotBlank()) {
                        val res = authRepo.updateUsername(username)
                        if (!res) allSuccessful = false
                    }

                    // LOGIC: Bio saves no matter what (can be cleared)
                    val bioRes = authRepo.updateBio(bio)
                    if (!bioRes) allSuccessful = false

                    // LOGIC: Only update Last.fm if not blank
                    if (lastFmUsername.isNotBlank()) {
                        val lastFmRes = authRepo.updateLastFm(lastFmUsername)
                        if (!lastFmRes) allSuccessful = false
                    }

                    if (allSuccessful) {
                        Toast.makeText(context, "Profile Updated!", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    }
                }
            }
        ) {
            Text("Save Changes")
        }
    }
}