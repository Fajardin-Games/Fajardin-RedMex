package com.ustadmobile.meshrabiya.testapp

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ContactsManager(
    private val nicknamesFile: File,
    private val json: Json,
) {
    private val logPrefix = "[ContactsManager]"
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    // Map of IP Address (String) to Nickname (String)
    private val _nicknames = MutableStateFlow<Map<String, String>>(emptyMap())
    val nicknames: Flow<Map<String, String>> = _nicknames.asStateFlow()

    init {
        loadNicknames()
    }

    private fun loadNicknames() {
        scope.launch {
            if (nicknamesFile.exists()) {
                try {
                    val text = nicknamesFile.readText()
                    if (text.isNotBlank()) {
                        val loadedMap = json.decodeFromString<Map<String, String>>(text)
                        _nicknames.update { loadedMap }
                    }
                } catch (e: Exception) {
                    Log.e(logPrefix, "Failed to load nicknames", e)
                }
            }
        }
    }

    fun setNickname(ipAddress: String, nickname: String) {
        _nicknames.update { prev ->
            val newMap = prev.toMutableMap()
            newMap[ipAddress] = nickname
            saveNicknames(newMap)
            newMap
        }
    }
    
    fun getNickname(ipAddress: String): String? {
        return _nicknames.value[ipAddress]
    }

    private fun saveNicknames(map: Map<String, String>) {
        scope.launch {
            try {
                val jsonStr = json.encodeToString(map)
                nicknamesFile.writeText(jsonStr)
            } catch (e: Exception) {
                Log.e(logPrefix, "Failed to save nicknames", e)
            }
        }
    }
}
