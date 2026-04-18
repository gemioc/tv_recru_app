package com.tv.terminal.ui.splash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tv.terminal.R
import com.tv.terminal.data.local.SharedPreferencesManager
import com.tv.terminal.databinding.ActivitySplashBinding
import com.tv.terminal.service.KeepAliveService
import com.tv.terminal.ui.main.MainActivity
import com.tv.terminal.ui.setting.SettingActivity
import com.tv.terminal.util.DeviceUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 启动页
 * 初始化设备信息，检查配置，跳转主界面或设置页
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 保持屏幕常亮，防止启动过程中息屏
        acquireWakeLock()

        // 启动前台服务保活（从启动页就开始保护）
        KeepAliveService.start(this)

        // 播放启动动画
        playEntranceAnimation()

        // 延迟跳转（在延迟中完成初始化）
        Handler(Looper.getMainLooper()).postDelayed({
            initAndNavigate()
        }, SPLASH_DELAY)
    }

    /**
     * 获取 WakeLock 防止启动过程中息屏
     */
    private fun acquireWakeLock() {
        // 1. FLAG_KEEP_SCREEN_ON
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Log.d(TAG, "SplashActivity: FLAG_KEEP_SCREEN_ON enabled")

        // 2. WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "TvTerminal:SplashWakeLock"
        )
        wakeLock?.acquire()
        Log.d(TAG, "SplashActivity: WakeLock acquired")
    }

    /**
     * 播放入场动画
     */
    private fun playEntranceAnimation() {
        // Logo 缩放 + 渐入动画
        val logoAnimation = AnimationSet(true).apply {
            val scaleAnim = ScaleAnimation(
                0.8f, 1f, 0.8f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            )
            val alphaAnim = AlphaAnimation(0f, 1f)

            addAnimation(scaleAnim)
            addAnimation(alphaAnim)
            duration = 600
            interpolator = android.view.animation.DecelerateInterpolator()
        }

        // 文字 渐入动画
        val textAnimation = AlphaAnimation(0f, 1f).apply {
            duration = 500
            startOffset = 300
            interpolator = android.view.animation.DecelerateInterpolator()
        }

        // ProgressBar 渐入动画
        val progressAnimation = AlphaAnimation(0f, 1f).apply {
            duration = 400
            startOffset = 500
            interpolator = android.view.animation.DecelerateInterpolator()
        }

        binding.logoCard.startAnimation(logoAnimation)
        binding.appNameText.startAnimation(textAnimation)
        binding.taglineText.startAnimation(textAnimation)
        binding.progressBar.startAnimation(progressAnimation)
    }

    /**
     * 初始化设备编码并跳转
     */
    private fun initAndNavigate() {
        lifecycleScope.launch {
            // 确保设备编码已初始化
            val savedCode = SharedPreferencesManager.getDeviceCode()
            if (savedCode.isEmpty()) {
                // 首次启动，生成设备编码
                val deviceCode = DeviceUtils.generateDeviceCode()
                SharedPreferencesManager.saveDeviceCode(deviceCode)
            }

            // 检查是否已完整配置
            val intent = if (SharedPreferencesManager.getServerUrl().isNotEmpty()) {
                // 已配置服务器地址，进入主界面
                Intent(this@SplashActivity, MainActivity::class.java)
            } else {
                // 未配置服务器地址，进入设置页
                Intent(this@SplashActivity, SettingActivity::class.java)
            }

            // 添加淡出效果
            val fadeOut = AlphaAnimation(1f, 0f).apply {
                duration = 300
                interpolator = android.view.animation.AccelerateInterpolator()
            }

            binding.root.startAnimation(fadeOut)

            delay(300) // 等待动画完成

            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 释放 WakeLock
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "SplashActivity: WakeLock released")
            }
        }
    }

    companion object {
        private const val TAG = "SplashActivity"
        private const val SPLASH_DELAY = 2500L // 2.5秒
    }
}
