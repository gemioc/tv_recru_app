package com.tv.terminal.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.tv.terminal.data.remote.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * WebSocket 管理器
 * 负责与服务端建立连接、发送/接收消息、断线重连
 *
 * @property deviceCode 设备编码
 */
class WebSocketManager(
    private val deviceCode: String
) {
    private var webSocket: WebSocket? = null
    private var serverUrl: String = ""

    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val gson = Gson()

    // 连接状态
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    // 接收消息
    private val _messageFlow = MutableSharedFlow<ServerMessage>(extraBufferCapacity = 10)
    val messageFlow: SharedFlow<ServerMessage> = _messageFlow

    /**
     * 连接服务器
     * @param url 服务器地址，格式：http://ip:port
     */
    fun connect(url: String) {
        this.serverUrl = url

        // 转换 HTTP URL 为 WebSocket URL
        val wsUrl = url.replace("http://", "ws://").replace("https://", "wss://")
        val fullUrl = "$wsUrl/ws/tv?deviceCode=$deviceCode"

        Log.d(TAG, "Connecting to: $fullUrl")

        _connectionState.value = ConnectionState.Connecting

        val request = Request.Builder()
            .url(fullUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                _connectionState.value = ConnectionState.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received message: $text")
                val message = parseMessage(text)
                message?.let {
                    CoroutineScope(Dispatchers.Main).launch {
                        _messageFlow.emit(it)
                    }
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: code=$code, reason=$reason")
                webSocket.close(1000, null)
                _connectionState.value = ConnectionState.Disconnected
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: code=$code, reason=$reason")
                _connectionState.value = ConnectionState.Disconnected
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                _connectionState.value = ConnectionState.Disconnected
                // 自动重连
                reconnect()
            }
        })
    }

    /**
     * 重连
     */
    private fun reconnect() {
        CoroutineScope(Dispatchers.Main).launch {
            delay(RECONNECT_INTERVAL)
            if (serverUrl.isNotEmpty()) {
                connect(serverUrl)
            }
        }
    }

    /**
     * 发送消息
     */
    fun send(message: ClientMessage) {
        val json = gson.toJson(message)
        Log.d(TAG, "Sending message: $json")
        webSocket?.send(json)
    }

    /**
     * 断开连接
     */
    fun disconnect() {
        webSocket?.close(1000, "App closed")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * 解析服务端消息
     */
    private fun parseMessage(text: String): ServerMessage? {
        return try {
            val jsonObject = gson.fromJson(text, JsonObject::class.java)
            val type = jsonObject.get("type")?.asString ?: return null

            when (type) {
                "HEARTBEAT_ACK" -> gson.fromJson(text, HeartbeatAckMessage::class.java)
                "PUSH_CONTENT" -> gson.fromJson(text, PushContentMessage::class.java)
                "CONTROL" -> gson.fromJson(text, ControlMessage::class.java)
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse message: $text", e)
            null
        }
    }

    /**
     * 是否已连接
     */
    fun isConnected(): Boolean {
        return _connectionState.value == ConnectionState.Connected
    }

    companion object {
        private const val TAG = "WebSocketManager"
        private const val RECONNECT_INTERVAL = 5000L // 5秒重连
    }
}

/**
 * 连接状态
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
}