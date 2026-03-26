package com.tv.terminal.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tv.terminal.ui.splash.SplashActivity

/**
 * 开机启动广播接收器
 * 设备启动后自动打开应用
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                // 启动应用
                val launchIntent = Intent(context, SplashActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(launchIntent)
                Log.d(TAG, "App started on boot")
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}