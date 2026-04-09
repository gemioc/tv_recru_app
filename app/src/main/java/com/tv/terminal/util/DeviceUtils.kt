package com.tv.terminal.util

import android.os.Build
import java.net.NetworkInterface

/**
 * 设备工具类
 */
object DeviceUtils {

    /**
     * 获取设备 MAC 地址
     */
    fun getMacAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.name.equals("wlan0", ignoreCase = true)) {
                    val macBytes = networkInterface.hardwareAddress ?: continue
                    return macBytes.joinToString(":") { "%02X".format(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "000000"
    }

    /**
     * 获取 MAC 后 6 位
     */
    fun getMacSuffix(): String {
        val mac = getMacAddress()
        return mac.replace(":", "").takeLast(6).uppercase()
    }

    /**
     * 生成设备编码
     * 格式：TV_{MAC后6位}_{时间戳后4位}
     */
    fun generateDeviceCode(): String {
        val macSuffix = getMacSuffix()
        val timestampSuffix = (System.currentTimeMillis() % 10000).toString().padStart(4, '0')
        return "TV_${macSuffix}_$timestampSuffix"
    }

    /**
     * 获取设备型号
     */
    fun getDeviceModel(): String {
        return Build.MODEL ?: "Unknown"
    }

    /**
     * 获取设备品牌
     */
    fun getDeviceBrand(): String {
        return Build.BRAND ?: "Unknown"
    }
}