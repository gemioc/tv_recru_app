package com.tv.terminal.data.remote

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.tv.terminal.data.remote.model.HeartbeatMessage

/**
 * 心跳管理器
 * 负责定时发送心跳包保持连接
 *
 * @property webSocketManager WebSocket管理器
 * @property deviceCode 设备编码
 */
class HeartbeatManager(
    private val webSocketManager: WebSocketManager,
    private val deviceCode: String
) {
    private val handler = Handler(Looper.getMainLooper())
    private var heartbeatCount = 0

    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            sendHeartbeat()
            handler.postDelayed(this, HEARTBEAT_INTERVAL)
        }
    }

    /**
     * 启动心跳
     */
    fun start() {
        Log.d(TAG, "Starting heartbeat")
        // 立即发送一次心跳
        sendHeartbeat()
        // 定时发送心跳
        handler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL)
    }

    /**
     * 停止心跳
     */
    fun stop() {
        Log.d(TAG, "Stopping heartbeat")
        handler.removeCallbacks(heartbeatRunnable)
        heartbeatCount = 0
    }

    /**
     * 发送心跳
     */
    private fun sendHeartbeat() {
        if (webSocketManager.isConnected()) {
            val message = HeartbeatMessage(deviceCode = deviceCode)
            webSocketManager.send(message)
            heartbeatCount++
            Log.d(TAG, "Heartbeat sent, count: $heartbeatCount")
        }
    }

    /**
     * 重置心跳计数
     */
    fun resetCount() {
        heartbeatCount = 0
    }

    companion object {
        private const val TAG = "HeartbeatManager"
        private const val HEARTBEAT_INTERVAL = 30_000L // 30秒
    }
}