package com.ustadmobile.meshrabiya.testapp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ustadmobile.meshrabiya.testapp.appstate.AppUiState
import com.ustadmobile.meshrabiya.testapp.server.TestAppServer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.instance
import java.net.InetAddress
import java.net.UnknownHostException

data class ChatUiState(
    val messages: List<TestAppServer.IncomingMessage> = emptyList(),
    val appUiState: AppUiState = AppUiState(),
    val targetAddress: String? = null,
)

class ChatViewModel(
    di: DI,
    private val targetAddress: String? // Null for broadcast or general view
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())

    val uiState: Flow<ChatUiState> = _uiState.asStateFlow()

    private val testAppServer: TestAppServer by di.instance()
    private val contactsManager: com.ustadmobile.meshrabiya.testapp.ContactsManager by di.instance()

    init {
        _uiState.update { prev ->
            prev.copy(
                appUiState = AppUiState(
                    title = if(targetAddress != null) "Chat con $targetAddress" else "Chat Global"
                ),
                targetAddress = targetAddress
            )
        }

        viewModelScope.launch {
            if (targetAddress != null) {
                contactsManager.nicknames.collect { nicks ->
                    val nick = nicks[targetAddress]
                    _uiState.update { prev ->
                        prev.copy(
                            appUiState = prev.appUiState.copy(
                                title = if (nick != null) "Chat con $nick" else "Chat con $targetAddress"
                            )
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            testAppServer.incomingMessages.collect { msgs ->
                // Filter messages if we are in a specific chat
                val filteredMsgs = if(targetAddress != null) {
                    msgs.filter { it.from == targetAddress || (it.from == "Me" && it.target == targetAddress) }
                } else {
                    msgs
                }

                _uiState.update { prev ->
                    prev.copy(messages = filteredMsgs)
                }
            }
        }
    }

    fun setNickname(nickname: String) {
        targetAddress?.let { ip ->
            contactsManager.setNickname(ip, nickname)
        }
    }

    fun sendMessage(text: String) {
        val target = _uiState.value.targetAddress ?: return
        viewModelScope.launch {
            try {
                val addr = InetAddress.getByName(target)
                testAppServer.sendTextMessage(addr, text)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendImage(imageUri: Uri) {
        val target = _uiState.value.targetAddress ?: return
        viewModelScope.launch {
            try {
                val addr = InetAddress.getByName(target)
                testAppServer.sendImageMessage(addr, imageUri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
