package com.tv.terminal.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.tv.terminal.ui.splash.SplashActivity

/**
 * 开机启动广播接收器
 * 设备启动后自动打开应用
 * 支持海信等电视的开机自启机制
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "android.intent.action.ACTION_BOOT_COMPLETED" -> {
                // 延迟启动，确保系统完全就绪
                // 部分电视需要延迟才能正确启动Activity
                Log.d(TAG, "Boot completed, launching app...")
                launchApplication(context)
            }
        }
    }

    private fun launchApplication(context: Context) {
        try {
            val launchIntent = Intent(context, SplashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                // 海信电视特殊处理
                if (Build.MANUFACTURER.equals("Hisense", ignoreCase = true) ||
                    Build.MANUFACTURER.equals("Hisense", ignoreCase = true)) {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            }

            // 使用 Intent.createChooser 确保在某些设备上能正常工作
            val chooserIntent = Intent.createChooser(launchIntent, "启动招聘展示系统")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(launchIntent)
            Log.d(TAG, "App started successfully on boot")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app on boot: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}