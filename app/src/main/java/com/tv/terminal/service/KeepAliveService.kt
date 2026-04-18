package com.tv.terminal.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tv.terminal.R
import com.tv.terminal.ui.main.MainActivity

/**
 * 前台服务 - 确保进程不会被系统杀死，持续阻止息屏
 * 相比 WakeLock，前台服务更可靠，是 Android 设备保活的标准方式
 */
class KeepAliveService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "KeepAliveService created")

        // 创建通知渠道（Android 8.0+ 必须）
        createNotificationChannel()

        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification())

        // 获取 WakeLock
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "KeepAliveService started")
        return START_STICKY // 被杀死后会自动重启
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released in KeepAliveService")
            }
        }
        Log.d(TAG, "KeepAliveService destroyed")
    }

    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "招聘展示服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持应用运行，防止息屏"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * 创建前台通知
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("招聘展示系统")
            .setContentText("正在运行中...")
            .setSmallIcon(R.drawable.ic_notification) // 需要创建图标
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // 不可清除
            .build()
    }

    /**
     * 获取 WakeLock
     */
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "TvTerminal:KeepAliveWakeLock"
        )
        wakeLock?.acquire()
        Log.d(TAG, "WakeLock acquired in KeepAliveService")
    }

    companion object {
        private const val TAG = "KeepAliveService"
        private const val CHANNEL_ID = "tv_recru_keepalive"
        private const val NOTIFICATION_ID = 10001

        /**
         * 启动前台服务
         */
        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "KeepAliveService start requested")
        }

        /**
         * 停止前台服务
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
            Log.d(TAG, "KeepAliveService stop requested")
        }
    }
}
