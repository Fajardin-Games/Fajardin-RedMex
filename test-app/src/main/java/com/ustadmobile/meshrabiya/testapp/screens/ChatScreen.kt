package com.ustadmobile.meshrabiya.testapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ustadmobile.meshrabiya.testapp.ViewModelFactory
import com.ustadmobile.meshrabiya.testapp.appstate.AppUiState
import com.ustadmobile.meshrabiya.testapp.viewmodel.ChatUiState
import com.ustadmobile.meshrabiya.testapp.viewmodel.ChatViewModel
import org.kodein.di.compose.localDI

@Composable
fun ChatScreen(
        targetAddress: String?,
        onSetAppUiState: (AppUiState) -> Unit,
        viewModel: ChatViewModel =
                viewModel(
                        factory =
                                ViewModelFactory(
                                        di = localDI(),
                                        owner = LocalSavedStateRegistryOwner.current,
                                        vmFactory = { ChatViewModel(it, targetAddress) },
                                        defaultArgs = null,
                                )
                ),
) {
        val uiState by viewModel.uiState.collectAsState(ChatUiState())
        var showEditNameDialog by remember { mutableStateOf(false) }

        LaunchedEffect(uiState.appUiState, showEditNameDialog) {
                val updatedUiState =
                        uiState.appUiState.copy(
                                fabState =
                                        com.ustadmobile.meshrabiya.testapp.appstate.FabState(
                                                visible = false
                                        ),
                                topBarActionIcon =
                                        if (targetAddress != null) Icons.Default.Edit else null,
                                topBarActionContentDescription = "Editar Alias",
                                onTopBarActionClick =
                                        if (targetAddress != null) {
                                                { showEditNameDialog = true }
                                        } else null
                        )
                onSetAppUiState(updatedUiState)
        }

        if (showEditNameDialog && targetAddress != null) {
                var nicknameInput by remember { mutableStateOf("") }

                AlertDialog(
                        onDismissRequest = { showEditNameDialog = false },
                        title = { Text("Editar Alias") },
                        text = {
                                Column {
                                        Text("Ingresa un alias para $targetAddress")
                                        OutlinedTextField(
                                                value = nicknameInput,
                                                onValueChange = { nicknameInput = it },
                                                modifier = Modifier.padding(top = 8.dp)
                                        )
                                }
                        },
                        confirmButton = {
                                TextButton(
                                        onClick = {
                                                viewModel.setNickname(nicknameInput)
                                                showEditNameDialog = false
                                        }
                                ) { Text("Guardar") }
                        },
                        dismissButton = {
                                TextButton(onClick = { showEditNameDialog = false }) {
                                        Text("Cancelar")
                                }
                        }
                )
        }

        Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), reverseLayout = true) {
                        items(uiState.messages) { msg ->
                                val isMe = msg.from == "Me"
                                val alignment = if (isMe) Alignment.End else Alignment.Start
                                val color = if (isMe) Color.Blue else Color.Gray

                                Column(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .padding(
                                                                horizontal = 8.dp,
                                                                vertical = 4.dp
                                                        ),
                                        horizontalAlignment = alignment
                                ) {
                                        Text(
                                                text = msg.from,
                                                color = color,
                                                style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                                text = msg.text,
                                                modifier = Modifier.padding(top = 2.dp)
                                        )
                                }
                        }
                }

                Row(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        var text by remember { mutableStateOf("") }

                        OutlinedTextField(
                                value = text,
                                onValueChange = { text = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Mensaje") }
                        )

                        IconButton(
                                onClick = {
                                        if (targetAddress != null && text.isNotBlank()) {
                                                viewModel.sendMessage(text)
                                                text = ""
                                        }
                                }
                        ) { Icon(Icons.Default.Send, contentDescription = "Enviar") }
                }
        }
}
