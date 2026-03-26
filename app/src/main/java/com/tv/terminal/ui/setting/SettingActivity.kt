package com.tv.terminal.ui.setting

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tv.terminal.R
import com.tv.terminal.data.local.SharedPreferencesManager
import com.tv.terminal.databinding.ActivitySettingBinding
import com.tv.terminal.util.DeviceUtils

/**
 * 设置页面
 * 配置服务器地址和显示设备编码
 */
class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 加载已有配置
        loadConfig()

        // 设置按钮监听
        setupListeners()
    }

    /**
     * 加载已有配置
     */
    private fun loadConfig() {
        // 显示设备编码
        val deviceCode = SharedPreferencesManager.getDeviceCode()
        if (deviceCode.isNotEmpty()) {
            binding.deviceCodeText.text = deviceCode
        } else {
            binding.deviceCodeText.text = DeviceUtils.generateDeviceCode()
        }

        // 显示服务器地址
        val serverUrl = SharedPreferencesManager.getServerUrl()
        if (serverUrl.isNotEmpty()) {
            binding.serverInput.setText(serverUrl)
        } else {
            // 默认地址（模拟器访问宿主机）
            binding.serverInput.setText(DEFAULT_SERVER_URL)
        }
    }

    /**
     * 设置按钮监听
     */
    private fun setupListeners() {
        // 保存按钮
        binding.saveButton.setOnClickListener {
            saveConfig()
        }

        // 返回按钮
        binding.backButton.setOnClickListener {
            finish()
        }

        // 清除配置按钮
        binding.clearButton.setOnClickListener {
            clearConfig()
        }
    }

    /**
     * 保存配置
     */
    private fun saveConfig() {
        val serverUrl = binding.serverInput.text.toString().trim()

        // 验证服务器地址
        if (serverUrl.isEmpty()) {
            Toast.makeText(this, R.string.hint_server, Toast.LENGTH_SHORT).show()
            return
        }

        // 简单验证 URL 格式
        if (!serverUrl.startsWith("http://") && !serverUrl.startsWith("https://")) {
            Toast.makeText(this, "服务器地址格式错误，应以 http:// 或 https:// 开头", Toast.LENGTH_SHORT).show()
            return
        }

        // 保存设备编码
        val deviceCode = binding.deviceCodeText.text.toString()
        SharedPreferencesManager.saveDeviceCode(deviceCode)

        // 保存服务器地址
        SharedPreferencesManager.saveServerUrl(serverUrl)

        // 保存设备名称
        val deviceName = "${DeviceUtils.getDeviceBrand()} ${DeviceUtils.getDeviceModel()}"
        SharedPreferencesManager.saveDeviceName(deviceName)

        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()

        // 返回
        finish()
    }

    /**
     * 清除配置
     */
    private fun clearConfig() {
        SharedPreferencesManager.clearAll()
        Toast.makeText(this, "配置已清除，请重新打开应用", Toast.LENGTH_LONG).show()
        finishAffinity() // 关闭所有Activity
    }

    companion object {
        // 模拟器默认使用 10.0.2.2 访问宿主机
        private const val DEFAULT_SERVER_URL = "http://10.0.2.2:8080"
    }
}