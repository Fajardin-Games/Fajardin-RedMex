package com.ustadmobile.meshrabiya.testapp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ustadmobile.meshrabiya.testapp.ViewModelFactory
import com.ustadmobile.meshrabiya.testapp.appstate.AppUiState
import com.ustadmobile.meshrabiya.testapp.viewmodel.ChatSummary
import com.ustadmobile.meshrabiya.testapp.viewmodel.RecentChatsUiState
import com.ustadmobile.meshrabiya.testapp.viewmodel.RecentChatsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.kodein.di.compose.localDI

@Composable
fun RecentChatsScreen(
        viewModel: RecentChatsViewModel =
                viewModel(
                        factory =
                                ViewModelFactory(
                                        di = localDI(),
                                        owner = LocalSavedStateRegistryOwner.current,
                                        vmFactory = { RecentChatsViewModel(it) },
                                        defaultArgs = null,
                                )
                ),
        onSetAppUiState: (AppUiState) -> Unit,
        onChatClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState(RecentChatsUiState())

    LaunchedEffect(uiState.appUiState) { onSetAppUiState(uiState.appUiState) }

    RecentChatsList(chats = uiState.chats, onChatClick = onChatClick)
}

@Composable
private fun RecentChatsList(chats: List<ChatSummary>, onChatClick: (String) -> Unit) {
    if (chats.isEmpty()) {
        Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) { Text("No hay chats recientes.", style = MaterialTheme.typography.bodyLarge) }
    } else {
        LazyColumn {
            items(chats, key = { it.ipAddress }) { chat ->
                ChatItem(chat = chat, onClick = { onChatClick(chat.ipAddress) })
                Divider()
            }
        }
    }
}

@Composable
private fun ChatItem(chat: ChatSummary, onClick: () -> Unit) {
    val dateFormatter = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val timeString = dateFormatter.format(Date(chat.timestamp))

    Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.padding(end = 16.dp)
        ) {
            Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp).size(28.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                        text = chat.nickname ?: chat.ipAddress,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                )
                Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
