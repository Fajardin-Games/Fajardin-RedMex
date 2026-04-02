package com.ustadmobile.meshrabiya.testapp.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.ustadmobile.meshrabiya.testapp.ViewModelFactory
import com.ustadmobile.meshrabiya.testapp.appstate.AppUiState
import com.ustadmobile.meshrabiya.testapp.theme.MeshAmber
import com.ustadmobile.meshrabiya.testapp.theme.MeshBackground
import com.ustadmobile.meshrabiya.testapp.theme.MeshOnAmber
import com.ustadmobile.meshrabiya.testapp.theme.MeshOutline
import com.ustadmobile.meshrabiya.testapp.theme.MeshSurfaceVariant
import com.ustadmobile.meshrabiya.testapp.theme.MeshText
import com.ustadmobile.meshrabiya.testapp.theme.MeshTextSecondary
import com.ustadmobile.meshrabiya.testapp.theme.MeshWood
import com.ustadmobile.meshrabiya.testapp.viewmodel.ChatUiState
import com.ustadmobile.meshrabiya.testapp.viewmodel.ChatViewModel
import org.kodein.di.compose.localDI
import java.io.File

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

        val imagePicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
                if (uri != null && targetAddress != null) {
                        viewModel.sendImage(uri)
                }
        }

        Column(
                modifier = Modifier
                        .fillMaxSize()
                        .background(MeshBackground)
        ) {
                LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        reverseLayout = true
                ) {
                        items(uiState.messages) { msg ->
                                val isMe = msg.from == "Me"
                                val bubbleShape = if (isMe) {
                                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
                                } else {
                                        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                                }
                                val bubbleColor = if (isMe) MeshWood else MeshSurfaceVariant
                                val senderColor = if (isMe) MeshAmber else MaterialTheme.colorScheme.secondary

                                Column(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 4.dp),
                                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                                ) {
                                        Text(
                                                text = if (isMe) "Tú" else msg.from,
                                                color = senderColor,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp, end = 4.dp)
                                        )

                                        Box(
                                                modifier = Modifier
                                                        .widthIn(max = 300.dp)
                                                        .clip(bubbleShape)
                                                        .background(bubbleColor)
                                                        .padding(
                                                                horizontal = if (msg.imageUri != null) 0.dp else 12.dp,
                                                                vertical = if (msg.imageUri != null) 0.dp else 8.dp
                                                        )
                                        ) {
                                                if (msg.imageUri != null) {
                                                        val imageModel: Any = if (msg.imageUri.startsWith("content://")) {
                                                                Uri.parse(msg.imageUri)
                                                        } else {
                                                                File(msg.imageUri)
                                                        }
                                                        AsyncImage(
                                                                model = imageModel,
                                                                contentDescription = "Imagen",
                                                                contentScale = ContentScale.FillWidth,
                                                                modifier = Modifier
                                                                        .widthIn(max = 300.dp)
                                                                        .heightIn(max = 300.dp)
                                                                        .clip(bubbleShape)
                                                        )
                                                } else {
                                                        Text(
                                                                text = msg.text,
                                                                color = MeshText,
                                                                style = MaterialTheme.typography.bodyMedium
                                                        )
                                                }
                                        }
                                }
                        }
                }

                // Input bar
                Row(
                        modifier = Modifier
                                .background(MeshBackground)
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        var text by remember { mutableStateOf("") }

                        IconButton(
                                onClick = {
                                        if (targetAddress != null) imagePicker.launch("image/*")
                                }
                        ) {
                                Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = "Adjuntar imagen",
                                        tint = MeshTextSecondary
                                )
                        }

                        OutlinedTextField(
                                value = text,
                                onValueChange = { text = it },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                        Text("Mensaje", color = MeshTextSecondary)
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MeshAmber,
                                        unfocusedBorderColor = MeshOutline,
                                        focusedTextColor = MeshText,
                                        unfocusedTextColor = MeshText,
                                        cursorColor = MeshAmber,
                                ),
                                shape = RoundedCornerShape(24.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                        onSend = {
                                                if (targetAddress != null && text.isNotBlank()) {
                                                        viewModel.sendMessage(text)
                                                        text = ""
                                                }
                                        }
                                ),
                                singleLine = true,
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                                onClick = {
                                        if (targetAddress != null && text.isNotBlank()) {
                                                viewModel.sendMessage(text)
                                                text = ""
                                        }
                                },
                                colors = ButtonDefaults.buttonColors(
                                        containerColor = MeshAmber,
                                        contentColor = MeshOnAmber,
                                        disabledContainerColor = Color(0xFF5A4800),
                                        disabledContentColor = Color(0xFF9A8040),
                                ),
                                shape = RoundedCornerShape(24.dp),
                        ) {
                                Text(
                                        text = "SEND",
                                        style = MaterialTheme.typography.labelLarge
                                )
                        }
                }
        }
}
