package com.ustadmobile.meshrabiya.testapp.screens

import android.content.Intent
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import com.ustadmobile.meshrabiya.testapp.theme.MeshAmber
import com.ustadmobile.meshrabiya.testapp.theme.MeshBackground
import com.ustadmobile.meshrabiya.testapp.theme.MeshOnAmber
import com.ustadmobile.meshrabiya.testapp.theme.MeshSurface
import com.ustadmobile.meshrabiya.testapp.theme.MeshTeal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ustadmobile.meshrabiya.testapp.ViewModelFactory
import com.ustadmobile.meshrabiya.testapp.appstate.AppUiState
import com.ustadmobile.meshrabiya.testapp.server.TestAppServer
import com.ustadmobile.meshrabiya.testapp.viewmodel.ReceiveUiState
import com.ustadmobile.meshrabiya.testapp.viewmodel.ReceiveViewModel
import org.kodein.di.compose.localDI

@Composable
fun ReceiveScreen(
    onSetAppUiState: (AppUiState) -> Unit,
    viewModel: ReceiveViewModel = viewModel(
        factory = ViewModelFactory(
            di = localDI(),
            owner = LocalSavedStateRegistryOwner.current,
            vmFactory = {
                ReceiveViewModel(it)
            },
            defaultArgs = null,
        )
    ),
) {
    val uiState by viewModel.uiState.collectAsState(ReceiveUiState())

    LaunchedEffect(uiState.appUiState) {
        onSetAppUiState(uiState.appUiState)
    }

    ReceiveScreen(
        uiState = uiState,
        onClickAccept = viewModel::onClickAcceptIncomingTransfer,
        onClickDecline = viewModel::onClickDeclineIncomingTransfer,
        onClickDelete = viewModel::onClickDeleteTransfer,
    )
}

@Composable
fun ReceiveScreen(
    uiState: ReceiveUiState,
    onClickAccept: (TestAppServer.IncomingTransfer) -> Unit =  { },
    onClickDecline: (TestAppServer.IncomingTransfer) -> Unit = { },
    onClickDelete: (TestAppServer.IncomingTransfer) -> Unit = { },
) {
    val context = LocalContext.current

    fun openTransfer(transfer: TestAppServer.IncomingTransfer) {
        val file = transfer.file
        if(file != null && transfer.status == TestAppServer.Status.COMPLETED) {
            val uri = FileProvider.getUriForFile(
                context, "com.ustadmobile.meshrabiya.testapp.fileprovider", file
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                file.extension
            ) ?: "*/*"
            intent.setDataAndType(uri, mimeType)
            if(intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            }else {
                Toast.makeText(context, "No se encontró aplicación para abrir el archivo", Toast.LENGTH_LONG).show()
            }

        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MeshBackground)
    ) {
        items(
            items = uiState.incomingTransfers,
            key = { Triple(it.fromHost, it.id, it.requestReceivedTime) }
        ) { transfer ->
            ListItem(
                modifier = Modifier
                    .clickable { openTransfer(transfer) }
                    .fillMaxWidth(),
                colors = ListItemDefaults.colors(
                    containerColor = MeshSurface,
                    headlineColor = MaterialTheme.colorScheme.onBackground,
                    supportingColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                headlineContent = {
                    Text(transfer.name, style = MaterialTheme.typography.titleSmall)
                },
                supportingContent = {
                    Column {
                        Text("De ${transfer.fromHost.hostAddress} (${transfer.status})")
                        Text(buildString {
                            append("${transfer.transferred} / ${transfer.size} bytes")
                            if(transfer.status == TestAppServer.Status.COMPLETED) {
                                append(" @ ${transfer.size / transfer.transferTime}KB/s (${transfer.transferTime}ms)")
                            }
                        })

                        if(transfer.status == TestAppServer.Status.PENDING) {
                            Row {
                                OutlinedButton(
                                    modifier = Modifier.padding(start = 0.dp, top = 8.dp, bottom = 8.dp),
                                    onClick = { onClickAccept(transfer) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MeshTeal),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MeshTeal)
                                ) {
                                    Text("Aceptar")
                                }
                                OutlinedButton(
                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                                    onClick = { onClickDecline(transfer) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Rechazar")
                                }
                            }
                        }

                        if(transfer.status == TestAppServer.Status.COMPLETED) {
                            OutlinedButton(
                                onClick = { openTransfer(transfer) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MeshAmber),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MeshAmber)
                            ) {
                                Text("Abrir")
                            }
                        }
                    }
                },
                trailingContent = {
                    if(transfer.status == TestAppServer.Status.COMPLETED) {
                        IconButton(onClick = { onClickDelete(transfer) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
            )
        }
    }
}
