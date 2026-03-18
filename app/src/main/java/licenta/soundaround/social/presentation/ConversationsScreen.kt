package licenta.soundaround.social.presentation

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import licenta.soundaround.social.domain.model.Conversation
import licenta.soundaround.social.domain.model.FriendRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    viewModel: ConversationsViewModel,
    onOpenChat: (conversation: Conversation) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.load()
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats") },
                actions = {
                    IconButton(onClick = { viewModel.load() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                viewModel.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                viewModel.pendingRequests.isEmpty() && viewModel.conversations.isEmpty() -> Text(
                    text = "No conversations yet.\nPing someone on the map!",
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                )
                else -> LazyColumn {
                    if (viewModel.pendingRequests.isNotEmpty()) {
                        item {
                            Text(
                                text = "Friend Requests",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(viewModel.pendingRequests, key = { it.fromUserId }) { request ->
                            FriendRequestItem(
                                request = request,
                                onAccept = { viewModel.acceptRequest(request.fromUserId) },
                                onDecline = { viewModel.declineRequest(request.fromUserId) }
                            )
                            HorizontalDivider()
                        }
                        item {
                            Text(
                                text = "Messages",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    items(viewModel.conversations, key = { it.id }) { conversation ->
                        ConversationItem(
                            conversation = conversation,
                            onClick = { onOpenChat(conversation) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun FriendRequestItem(
    request: FriendRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    ListItem(
        headlineContent = { Text("@${request.fromUsername}") },
        supportingContent = { Text("wants to be friends") },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDecline,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Decline") }
                Button(onClick = onAccept) { Text("Accept") }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ConversationItem(conversation: Conversation, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text("@${conversation.otherUsername}") },
        supportingContent = {
            if (conversation.initialTrackTitle != null) {
                Text(
                    "${conversation.initialTrackTitle} · ${conversation.initialTrackArtist}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            if (!conversation.isPersistent) {
                Text(
                    "Temp",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
