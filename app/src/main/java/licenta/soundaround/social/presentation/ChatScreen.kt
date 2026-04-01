package licenta.soundaround.social.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import licenta.soundaround.social.domain.model.Message
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    otherUsername: String,
    onBack: () -> Unit,
    onGoToProfile: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var showConnectionSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState()
    val hasConnectionInfo = viewModel.myInitialTrackTitle != null || viewModel.theirInitialTrackTitle != null

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) {
            listState.animateScrollToItem(viewModel.messages.size - 1)
        }
    }

    if (showConnectionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showConnectionSheet = false },
            sheetState = sheetState
        ) {
            ConnectionInfoSheet(
                otherUsername = otherUsername,
                myTrackTitle = viewModel.myInitialTrackTitle,
                myTrackArtist = viewModel.myInitialTrackArtist,
                theirTrackTitle = viewModel.theirInitialTrackTitle,
                theirTrackArtist = viewModel.theirInitialTrackArtist,
                isPersistent = viewModel.isPersistent
            )
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false; reportReason = "" },
            title = { Text("Report @$otherUsername") },
            text = {
                OutlinedTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    label = { Text("Reason") },
                    placeholder = { Text("Describe the issue...") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (reportReason.isNotBlank()) {
                        viewModel.reportUser(reportReason.trim())
                        showReportDialog = false
                        reportReason = ""
                    }
                }) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false; reportReason = "" }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = if (onGoToProfile != null) Modifier.clickable { onGoToProfile() } else Modifier
                    ) {
                        Text("@$otherUsername", fontWeight = FontWeight.SemiBold)
                        when {
                            viewModel.otherIsTyping -> Text(
                                "typing...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            viewModel.expiresLabel != null -> Text(
                                text = viewModel.expiresLabel!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (viewModel.expiresLabel == "Chat expired")
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (hasConnectionInfo) {
                        IconButton(onClick = { showConnectionSheet = true }) {
                            Icon(
                                Icons.Filled.MusicNote,
                                contentDescription = "Connection details",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    when {
                        viewModel.isPersistent -> {}
                        viewModel.friendRequestSent -> {
                            TextButton(onClick = {}, enabled = false) {
                                Text("Request Sent")
                            }
                        }
                        else -> {
                            TextButton(onClick = { viewModel.sendFriendRequest() }) {
                                Text("Add Friend")
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Block user", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; viewModel.blockUser() }
                            )
                            DropdownMenuItem(
                                text = { Text("Report user") },
                                onClick = { showMenu = false; showReportDialog = true }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it; if (it.isNotEmpty()) viewModel.onTextChanged() },
                    placeholder = { Text("Message...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText.trim())
                            inputText = ""
                        }
                    },
                    enabled = !viewModel.isSending
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "Send")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val messages = viewModel.messages
            val lastOwnIndex = messages.indexOfLast { viewModel.isMyMessage(it) }
            // Only show Seen if the other user hasn't replied since our last message
            val lastMessageIsOwn = messages.lastOrNull()?.let { viewModel.isMyMessage(it) } == true
            val otherReadMs = viewModel.otherLastReadAt?.let { parseTimestamp(it) }
            messages.forEachIndexed { index, message ->
                val prevMessage = messages.getOrNull(index - 1)
                val nextMessage = messages.getOrNull(index + 1)
                val currentDay = messageDay(message.sentAt)
                val prevDay = prevMessage?.let { messageDay(it.sentAt) }
                if (prevDay == null || currentDay != prevDay) {
                    item(key = "date_$currentDay") {
                        DateSeparator(label = currentDay)
                    }
                }
                val isOwn = viewModel.isMyMessage(message)
                val showTime = nextMessage == null ||
                    minutesDiff(message.sentAt, nextMessage.sentAt) >= 1
                val showSeen = isOwn && index == lastOwnIndex &&
                    lastMessageIsOwn &&
                    otherReadMs != null &&
                    (parseTimestamp(message.sentAt) ?: Long.MAX_VALUE) <= otherReadMs
                item(key = message.id) {
                    MessageBubble(
                        message = message,
                        isOwn = isOwn,
                        showTime = showTime,
                        showSeen = showSeen
                    )
                }
            }
        }
    }
}

@Composable
private fun DateSeparator(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@Composable
private fun MessageBubble(message: Message, isOwn: Boolean, showTime: Boolean, showSeen: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isOwn) 16.dp else 4.dp,
                bottomEnd = if (isOwn) 4.dp else 16.dp
            ),
            color = if (isOwn) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = if (isOwn) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showTime || showSeen) {
            Text(
                text = buildString {
                    if (showTime) append(messageTime(message.sentAt))
                    if (showSeen) {
                        if (showTime) append(" · ")
                        append("Seen")
                    }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (showSeen) 1f else 0.6f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

private fun parseTimestamp(sentAt: String): Long? {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val clean = sentAt.substringBefore("+").trimEnd('Z').substringBefore(".")
        sdf.parse(clean)?.time
    } catch (e: Exception) { null }
}

private fun minutesDiff(fromSentAt: String, toSentAt: String): Long {
    val from = parseTimestamp(fromSentAt) ?: return 0
    val to = parseTimestamp(toSentAt) ?: return 0
    return (to - from) / 60_000
}

private fun messageTime(sentAt: String): String {
    val ms = parseTimestamp(sentAt) ?: return ""
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(ms)
}

private fun messageDay(sentAt: String): String {
    val ms = parseTimestamp(sentAt) ?: return sentAt
    return SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(ms)
}

@Composable
private fun ConnectionInfoSheet(
    otherUsername: String,
    myTrackTitle: String?,
    myTrackArtist: String?,
    theirTrackTitle: String?,
    theirTrackArtist: String?,
    isPersistent: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "How you connected",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
        HorizontalDivider()
        if (myTrackTitle != null) {
            TrackRow(label = "You were listening to", title = myTrackTitle, artist = myTrackArtist)
        }
        if (theirTrackTitle != null) {
            TrackRow(label = "@$otherUsername was listening to", title = theirTrackTitle, artist = theirTrackArtist)
        }
        if (myTrackTitle == null && theirTrackTitle == null) {
            Text(
                text = "No track info available for this conversation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isPersistent) {
            HorizontalDivider()
            Text(
                text = "This is a permanent friendship chat.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun TrackRow(label: String, title: String, artist: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            if (artist != null) {
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
