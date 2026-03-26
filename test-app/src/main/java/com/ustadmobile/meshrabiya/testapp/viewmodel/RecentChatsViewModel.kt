package com.ustadmobile.meshrabiya.testapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ustadmobile.meshrabiya.testapp.ContactsManager
import com.ustadmobile.meshrabiya.testapp.appstate.AppUiState
import com.ustadmobile.meshrabiya.testapp.appstate.FabState
import com.ustadmobile.meshrabiya.testapp.server.TestAppServer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance

data class ChatSummary(
    val ipAddress: String,
    val nickname: String?,
    val lastMessage: String,
    val timestamp: Long
)

data class RecentChatsUiState(
    val appUiState: AppUiState = AppUiState(title = "Contactos / Chats", fabState = FabState(visible = false)),
    val chats: List<ChatSummary> = emptyList()
)

class RecentChatsViewModel(di: DI) : ViewModel() {

    private val testAppServer: TestAppServer by di.instance()
    private val contactsManager: ContactsManager by di.instance()

    private val _uiState = MutableStateFlow(RecentChatsUiState())
    val uiState: Flow<RecentChatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(testAppServer.incomingMessages, contactsManager.nicknames) { messages, nicknames ->
                // Group messages to extract latest chat per IP
                val chatsByIp = mutableMapOf<String, TestAppServer.IncomingMessage>()
                
                messages.forEach { msg ->
                    // Determine the "other" participant in the conversation
                    val otherIp = if (msg.from == "Me") msg.target else msg.from
                    if (otherIp != null) {
                        val existingMsg = chatsByIp[otherIp]
                        if (existingMsg == null || msg.time > existingMsg.time) {
                            chatsByIp[otherIp] = msg
                        }
                    }
                }

                // Map to UI representation
                chatsByIp.map { (ip, lastMsg) ->
                    val displayName = nicknames[ip]
                    ChatSummary(
                        ipAddress = ip,
                        nickname = displayName,
                        lastMessage = lastMsg.text,
                        timestamp = lastMsg.time
                    )
                }.sortedByDescending { it.timestamp }
            }.collect { summaries ->
                _uiState.update { prev ->
                    prev.copy(chats = summaries)
                }
            }
        }
    }
}
