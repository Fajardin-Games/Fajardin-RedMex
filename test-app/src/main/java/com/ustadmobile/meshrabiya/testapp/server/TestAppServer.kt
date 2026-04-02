package com.ustadmobile.meshrabiya.testapp.server

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ustadmobile.meshrabiya.ext.copyToWithProgressCallback
import com.ustadmobile.meshrabiya.log.MNetLogger
import com.ustadmobile.meshrabiya.testapp.ext.getUriNameAndSize
import com.ustadmobile.meshrabiya.testapp.ext.updateItem
import com.ustadmobile.meshrabiya.util.FileSerializer
import com.ustadmobile.meshrabiya.util.InetAddressSerializer
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.headersContentLength
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicInteger



/**
 * The TestAppServer is used to send/receive files between nodes. Flow as follows:
 * 1. The sender
 */
class TestAppServer(
    private val appContext: Context,
    private val httpClient: OkHttpClient,
    private val mLogger: MNetLogger,
    name: String,
    port: Int = 0,
    private val localVirtualAddr: InetAddress,
    private val receiveDir: File,
    private val json: Json,
    private val chatHistoryFile: File,
) : NanoHTTPD(port), Closeable {

    private val logPrefix: String = "[TestAppServer - $name] "

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    enum class Status {
        PENDING, IN_PROGRESS, COMPLETED, FAILED, DECLINED
    }
    data class OutgoingTransfer(
        val id: Int,
        val name: String,
        val uri: Uri,
        val toHost: InetAddress,
        val status: Status = Status.PENDING,
        val size: Int,
        val transferred: Int = 0,
    )

    @Serializable
    data class IncomingTransfer(
        val id: Int,
        val requestReceivedTime: Long = System.currentTimeMillis(),
        @Serializable(with = InetAddressSerializer::class)
        val fromHost: InetAddress,
        val name: String,
        val status: Status = Status.PENDING,
        val size: Int,
        val transferred: Int = 0,
        val transferTime: Int = 1,
        @Serializable(with = FileSerializer::class)
        val file: File? = null,
    )

    private val transferIdAtomic = AtomicInteger()

    private val _outgoingTransfers = MutableStateFlow(emptyList<OutgoingTransfer>())

    val outgoingTransfers: Flow<List<OutgoingTransfer>> = _outgoingTransfers.asStateFlow()

    val _incomingTransfers = MutableStateFlow(emptyList<IncomingTransfer>())

    val incomingTransfers: Flow<List<IncomingTransfer>> = _incomingTransfers.asStateFlow()

    val localPort: Int
        get() = super.getListeningPort()

    @Serializable
    data class IncomingMessage(
        val from: String, // IP address string
        val text: String,
        val target: String? = null, // IP address of recipient (for outgoing messages)
        val time: Long = System.currentTimeMillis(),
        val imageUri: String? = null, // local file path or content URI for images
    )

    private val _incomingMessages = MutableStateFlow(emptyList<IncomingMessage>())
    val incomingMessages: Flow<List<IncomingMessage>> = _incomingMessages.asStateFlow()

    init {
        scope.launch {
            val incomingFiles = receiveDir.listFiles { file, fileName: String? ->
                fileName?.endsWith(".rx.json") == true
            }?.map {
                json.decodeFromString(IncomingTransfer.serializer(), it.readText())
            } ?: emptyList()
            _incomingTransfers.update { prev ->
                buildList {
                    addAll(prev)
                    addAll(incomingFiles.sortedByDescending { it.requestReceivedTime })
                }
            }

            // Load chat history
            if (chatHistoryFile.exists()) {
                try {
                    val savedMessages = json.decodeFromString<List<IncomingMessage>>(chatHistoryFile.readText())
                    _incomingMessages.update { savedMessages }
                } catch (e: Exception) {
                    mLogger(Log.ERROR, "$logPrefix Failed to load chat history", e)
                }
            }
        }
    }

    private fun saveChatHistory() {
        scope.launch {
            try {
                val currentMessages = _incomingMessages.value
                val jsonStr = json.encodeToString(currentMessages)
                chatHistoryFile.writeText(jsonStr)
            } catch (e: Exception) {
                mLogger(Log.ERROR, "$logPrefix Failed to save chat history", e)
            }
        }
    }


    override fun serve(session: IHTTPSession): Response {
        val path = session.uri
        mLogger(Log.INFO, "$logPrefix : ${session.method} ${session.uri}")

        if(path.startsWith("/download/")) {
            val xferId = path.substringAfterLast("/").toInt()
            val outgoingXfer = _outgoingTransfers.value.first {
                it.id == xferId
            }

            val contentIn = appContext.contentResolver.openInputStream(outgoingXfer.uri)?.let {
                InputStreamCounter(it.buffered())
            }

            if(contentIn == null) {
                mLogger(Log.ERROR, "$logPrefix Failed to open input stream to serve $path - ${outgoingXfer.uri}")
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain",
                    "Failed to open InputStream")
            }

            mLogger(Log.INFO, "$logPrefix Sending file for xfer #$xferId")
            val response = if (outgoingXfer.size > 0) {
                newFixedLengthResponse(
                    Response.Status.OK, "application/octet-stream",
                    contentIn,
                    outgoingXfer.size.toLong()
                )
            } else {
                newChunkedResponse(Response.Status.OK, "application/octet-stream", contentIn)
            }

            //Provide status updates by checking how many bytes have been read periodically
            scope.launch {
                while(!contentIn.closed) {
                    _outgoingTransfers.update { prev ->
                        prev.updateItem(
                            updatePredicate = { it.id == xferId },
                            function = { item ->
                                item.copy(
                                    transferred = contentIn.bytesRead,
                                )
                            }
                        )
                    }
                    delay(500)
                }

                val status = if(contentIn.bytesRead == outgoingXfer.size) {
                    Status.COMPLETED
                }else {
                    Status.FAILED
                }
                mLogger(Log.INFO, "$logPrefix Sending file for xfer #$xferId - finished - status=$status")

                _outgoingTransfers.update { prev ->
                    prev.updateItem(
                        updatePredicate = { it.id == xferId },
                        function = { item ->
                            item.copy(
                                transferred = contentIn.bytesRead,
                                status = status
                            )
                        }
                    )
                }
            }

            return response
        }else if(path.startsWith("/send")) {
            mLogger(Log.INFO, "$logPrefix Received incoming transfer request")
            val searchParams = session.queryParameterString.split("&")
                .map {
                    it.substringBefore("=") to it.substringAfter("=")
                }.toMap()

            val id = searchParams["id"]
            val filename = searchParams["filename"]
            val size = searchParams["size"]?.toInt() ?: -1
            val fromAddr = searchParams["from"]

            if(id != null && filename != null && fromAddr != null) {
                val incomingTransfer = IncomingTransfer(
                    id = id.toInt(),
                    fromHost = InetAddress.getByName(fromAddr),
                    name = filename,
                    size = size
                )

                _incomingTransfers.update { prev ->
                    buildList {
                        add(incomingTransfer)
                        addAll(prev)
                    }
                }

                mLogger(Log.INFO, "$logPrefix Added request id $id for $filename from ${incomingTransfer.fromHost}")
                return newFixedLengthResponse("OK")
            }else {
                mLogger(Log.INFO, "$logPrefix incomin transfer request - bad request - missing params")
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Bad request")
            }
        }else if(path.startsWith("/decline")){
            val xferId = path.substringAfterLast("/").toInt()
            _outgoingTransfers.update { prev ->
                prev.updateItem(
                    updatePredicate = { it.id == xferId },
                    function = {
                        it.copy(
                            status = Status.DECLINED
                        )
                    }
                )
            }

            return newFixedLengthResponse("OK")
        }else if(path.startsWith("/message")) {
            val searchParams = session.queryParameterString.split("&")
                .map {
                    it.substringBefore("=") to it.substringAfter("=")
                }.toMap()

            val text = searchParams["text"]?.let { java.net.URLDecoder.decode(it, "UTF-8") }
            val fromVal = searchParams["from"]

            if(text != null && fromVal != null) {
                // Mensaje especial de imagen: el sender indica qué transferencia es una imagen de chat
                if (text.startsWith(CHAT_IMG_PREFIX)) {
                    val transferId = text.removePrefix(CHAT_IMG_PREFIX).toIntOrNull()
                    if (transferId != null) {
                        scope.launch {
                            // Wait up to 3s for the /send request to be processed first
                            var transfer = _incomingTransfers.value.firstOrNull { it.id == transferId }
                            var retries = 0
                            while (transfer == null && retries < 6) {
                                delay(500)
                                transfer = _incomingTransfers.value.firstOrNull { it.id == transferId }
                                retries++
                            }
                            if (transfer != null) {
                                receiveDir.takeIf { !it.exists() }?.mkdirs()
                                val destFile = File(receiveDir, transfer.name)
                                acceptIncomingTransfer(transfer, destFile)
                                if (destFile.exists() && destFile.length() > 0) {
                                    val msg = IncomingMessage(
                                        from = fromVal,
                                        text = "",
                                        imageUri = destFile.absolutePath
                                    )
                                    _incomingMessages.update { prev ->
                                        buildList { add(msg); addAll(prev) }
                                    }
                                    saveChatHistory()
                                    mLogger(Log.INFO, "$logPrefix Chat image saved from $fromVal: ${destFile.absolutePath}")
                                } else {
                                    mLogger(Log.WARN, "$logPrefix Chat image download failed from $fromVal (transferId=$transferId)")
                                }
                            } else {
                                mLogger(Log.WARN, "$logPrefix Chat image transfer $transferId not found after retries")
                            }
                        }
                    }
                    return newFixedLengthResponse("OK")
                }

                val msg = IncomingMessage(
                    from = fromVal,
                    text = text,
                )

                _incomingMessages.update { prev ->
                    buildList {
                        add(msg)
                        addAll(prev)
                    }
                }
                saveChatHistory()
                mLogger(Log.INFO, "$logPrefix Received message from $fromVal : $text")
                return newFixedLengthResponse("OK")
            }else {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Bad request")
            }
        }else {
            mLogger(Log.INFO, "$logPrefix : $path - NOT FOUND")
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "not found: $path")
        }
    }


    init {

    }

    /**
     * Add an outgoing transfer. This is done using a Uri so that we don't have to make our own
     * copy of the file the user wants to transfer.
     */
    fun addOutgoingTransfer(
        uri: Uri,
        toNode: InetAddress,
        toPort: Int = DEFAULT_PORT,
    ): OutgoingTransfer {
        val transferId = transferIdAtomic.incrementAndGet()

        val nameAndSize = appContext.contentResolver.getUriNameAndSize(uri)
        val effectiveName = nameAndSize.name ?: "unknown"
        mLogger(Log.INFO, "$logPrefix adding outgoing transfer of $uri " +
                "(name=${nameAndSize.name} size=${nameAndSize.size} to $toNode:$toPort")

        val outgoingTransfer = OutgoingTransfer(
            id = transferId,
            name = effectiveName,
            uri = uri ,
            toHost = toNode,
            size = nameAndSize.size.toInt(),
        )


        //tell the other side about the transfer
        val request = Request.Builder().url("http://${toNode.hostAddress}:$toPort/" +
                "send?id=$transferId&filename=${URLEncoder.encode(effectiveName, "UTF-8")}" +
                "&size=${nameAndSize.size}&from=${localVirtualAddr.hostAddress}")
            //.addHeader("connection", "close")
            .build()
        mLogger(Log.INFO, "$logPrefix notifying $toNode of incoming transfer")

        val response = httpClient.newCall(request).execute()
        val serverResponse = response.body?.string()
        mLogger(Log.INFO, "$logPrefix - received response: $serverResponse")

        _outgoingTransfers.update { prev ->
            buildList {
                add(outgoingTransfer)
                addAll(prev)
            }
        }

        return outgoingTransfer
    }

    fun acceptIncomingTransfer(
        transfer: IncomingTransfer,
        destFile: File,
        fromPort: Int = DEFAULT_PORT,
    ) {
        val startTime = System.currentTimeMillis()
        _incomingTransfers.update { prev ->
            prev.updateItem(
                updatePredicate = { it.id == transfer.id },
                function = { item ->
                    item.copy(
                        status = Status.IN_PROGRESS,
                    )
                }
            )
        }

        try {
            val request = Request.Builder()
                .url("http://${transfer.fromHost.hostAddress}:$fromPort/download/${transfer.id}")
                .build()

            val response = httpClient.newCall(request).execute()
            val fileSize = response.headersContentLength()
            var lastUpdateTime = 0L
            val totalTransfered = response.body?.byteStream()?.use { responseIn ->
                FileOutputStream(destFile).use { fileOut ->
                    responseIn.copyToWithProgressCallback(
                        out = fileOut,
                        onProgress = { bytesTransferred ->
                            val timeNow = System.currentTimeMillis()
                            if(timeNow - lastUpdateTime > 500) {
                                _incomingTransfers.update { prev ->
                                    prev.updateItem(
                                        updatePredicate = { it.id == transfer.id },
                                        function = { item ->
                                            item.copy(
                                                transferred = bytesTransferred.toInt()
                                            )
                                        }
                                    )
                                }
                                lastUpdateTime = System.currentTimeMillis()
                            }
                        }
                    )
                }
            }
            response.close()

            val transferDurationMs = (System.currentTimeMillis() - startTime).toInt()
            val incomingTransfersVal = _incomingTransfers.updateAndGet { prev ->
                prev.updateItem(
                    updatePredicate = { it.id == transfer.id },
                    function = { item ->
                        item.copy(
                            transferTime = transferDurationMs,
                            status = if(fileSize <= 0 || totalTransfered == fileSize) {
                                 Status.COMPLETED
                            }else {
                                  Status.FAILED
                            },
                            file = destFile,
                            transferred = totalTransfered?.toInt() ?: item.transferred
                        )
                    }
                )
            }

            //Write JSON to file so received files can be listed after app restarts etc.
            val incomingTransfer = incomingTransfersVal.firstOrNull {
                it.id == transfer.id
            }

            if(incomingTransfer != null) {
                val jsonFile = File(receiveDir, "${incomingTransfer.name}.rx.json")
                jsonFile.writeText(json.encodeToString(IncomingTransfer.serializer(), incomingTransfer))
            }

            val speedKBS = transfer.size / transferDurationMs
            mLogger(Log.INFO, "$logPrefix acceptIncomingTransfer successful: Downloaded " +
                    "${transfer.size}bytes in ${transfer.transferTime}ms ($speedKBS) KB/s")
        }catch(e: Exception) {
            mLogger(Log.ERROR, "$logPrefix acceptIncomingTransfer ($transfer) FAILED", e)
            _incomingTransfers.update { prev ->
                prev.updateItem(
                    updatePredicate = { it.id == transfer.id },
                    function = { item ->
                        item.copy(
                            transferred = destFile.length().toInt(),
                            status = Status.FAILED,
                        )
                    }
                )
            }
        }
    }

    suspend fun onDeclineIncomingTransfer(
        transfer: IncomingTransfer,
        fromPort: Int = DEFAULT_PORT,
    ) {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("http://${transfer.fromHost.hostAddress}:$fromPort/decline/${transfer.id}")
                .build()

            try {
                val response = httpClient.newCall(request).execute()
                val strResponse = response.body?.string()
                mLogger(Log.DEBUG, "$logPrefix - onDeclineIncomingTransfer - request to: ${request.url} : response = $strResponse")
            }catch(e: Exception) {
                mLogger(Log.WARN, "$logPrefix - onDeclineIncomingTransfer : exception- request to: ${request.url} : FAIL", e)
            }
        }

        _incomingTransfers.update { prev ->
            prev.updateItem(
                updatePredicate = { it.id == transfer.id },
                function = {
                    it.copy(
                        status = Status.DECLINED,
                    )
                }
            )
        }
    }

    suspend fun onDeleteIncomingTransfer(
        incomingTransfer: IncomingTransfer
    ) {
        withContext(Dispatchers.IO) {
            val jsonFile = incomingTransfer.file?.let {
                File(it.parentFile, it.name + ".rx.json")
            }
            incomingTransfer.file?.delete()
            jsonFile?.delete()
            _incomingTransfers.update { prev ->
                prev.filter { it.id != incomingTransfer.id }
            }
        }
    }

    suspend fun sendTextMessage(
        toNode: InetAddress,
        text: String,
        port: Int = DEFAULT_PORT,
    ) {
        withContext(Dispatchers.IO) {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val request = Request.Builder()
                .url("http://${toNode.hostAddress}:$port/message?text=$encodedText&from=${localVirtualAddr.hostAddress}")
                .build()

            try {
                httpClient.newCall(request).execute()
                mLogger(Log.INFO, "$logPrefix sent message to $toNode: $text")

                // Also add to our own list so we see what we sent
                val myMsg = IncomingMessage(
                    from = "Me",
                    text = text,
                    target = toNode.hostAddress
                )
                _incomingMessages.update { prev ->
                    buildList {
                        add(myMsg)
                        addAll(prev)
                    }
                }
                saveChatHistory()

            } catch(e: Exception) {
                mLogger(Log.ERROR, "$logPrefix failed to send message to $toNode", e)
                throw e
            }
        }
    }


    suspend fun sendImageMessage(
        toNode: InetAddress,
        imageUri: Uri,
        port: Int = DEFAULT_PORT,
    ) {
        withContext(Dispatchers.IO) {
            // 1. Registra la transferencia saliente y notifica al receptor vía /send (mecanismo existente)
            val transfer = addOutgoingTransfer(imageUri, toNode, port)

            // 2. Envía mensaje especial para que el receptor auto-acepte y lo muestre en el chat
            val encodedMsg = URLEncoder.encode("$CHAT_IMG_PREFIX${transfer.id}", "UTF-8")
            val msgRequest = Request.Builder()
                .url("http://${toNode.hostAddress}:$port/message?text=$encodedMsg&from=${localVirtualAddr.hostAddress}")
                .build()

            try {
                httpClient.newCall(msgRequest).execute()

                // 3. Agrega mensaje local para que el sender vea la imagen inmediatamente
                val myMsg = IncomingMessage(
                    from = "Me",
                    text = "",
                    target = toNode.hostAddress,
                    imageUri = imageUri.toString()
                )
                _incomingMessages.update { prev ->
                    buildList { add(myMsg); addAll(prev) }
                }
                saveChatHistory()
                mLogger(Log.INFO, "$logPrefix sent chat image to $toNode (transferId=${transfer.id})")
            } catch (e: Exception) {
                mLogger(Log.ERROR, "$logPrefix failed to send chat image to $toNode", e)
                throw e
            }
        }
    }

    override fun close() {
        stop()
        scope.cancel()
    }

    companion object {

        const val DEFAULT_PORT = 4242

        // Prefijo para mensajes internos de imagen en chat
        const val CHAT_IMG_PREFIX = "__IMG__:"

    }

}