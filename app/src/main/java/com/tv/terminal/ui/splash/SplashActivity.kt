package com.tv.terminal.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tv.terminal.R
import com.tv.terminal.data.local.SharedPreferencesManager
import com.tv.terminal.databinding.ActivitySplashBinding
import com.tv.terminal.ui.main.MainActivity
import com.tv.terminal.ui.setting.SettingActivity
import com.tv.terminal.util.DeviceUtils
import kotlinx.coroutines.launch

/**
 * 启动页
 * 初始化设备信息，检查配置，跳转主界面或设置页
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 延迟跳转（在延迟中完成初始化）
        Handler(Looper.getMainLooper()).postDelayed({
            initAndNavigate()
        }, SPLASH_DELAY)
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
            startActivity(intent)
            finish()
        }
    }

    companion object {
        private const val SPLASH_DELAY = 2000L // 2秒
    }
}