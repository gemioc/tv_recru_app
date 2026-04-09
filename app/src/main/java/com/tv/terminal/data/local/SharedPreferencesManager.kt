package com.tv.terminal.data.local

import android.content.Context
import android.content.SharedPreferences
import com.tv.terminal.TvApplication

/**
 * SharedPreferences 管理器
 */
object SharedPreferencesManager {

    private const val PREF_NAME = "tv_terminal_prefs"

    // Keys
    private const val KEY_DEVICE_CODE = "device_code"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_DEVICE_NAME = "device_name"

    private fun getPrefs(): SharedPreferences {
        return TvApplication.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 获取设备编码
     */
    fun getDeviceCode(): String {
        return getPrefs().getString(KEY_DEVICE_CODE, "") ?: ""
    }

    /**
     * 保存设备编码
     */
    fun saveDeviceCode(deviceCode: String) {
        getPrefs().edit().putString(KEY_DEVICE_CODE, deviceCode).apply()
    }

    /**
     * 获取服务器地址
     */
    fun getServerUrl(): String {
        return getPrefs().getString(KEY_SERVER_URL, "") ?: ""
    }

    /**
     * 保存服务器地址
     */
    fun saveServerUrl(serverUrl: String) {
        getPrefs().edit().putString(KEY_SERVER_URL, serverUrl).apply()
    }

    /**
     * 获取设备名称
     */
    fun getDeviceName(): String {
        return getPrefs().getString(KEY_DEVICE_NAME, "") ?: ""
    }

    /**
     * 保存设备名称
     */
    fun saveDeviceName(deviceName: String) {
        getPrefs().edit().putString(KEY_DEVICE_NAME, deviceName).apply()
    }

    /**
     * 清除所有配置
     */
    fun clearAll() {
        getPrefs().edit().clear().apply()
    }
}