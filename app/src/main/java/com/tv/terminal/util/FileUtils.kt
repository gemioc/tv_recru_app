package com.tv.terminal.util

import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 文件工具类
 */
object FileUtils {

    /**
     * 获取缓存目录
     */
    fun getCacheDir(): File {
        return com.tv.terminal.TvApplication.context.cacheDir
    }

    /**
     * 获取缓存大小
     */
    fun getCacheSize(): String {
        val cacheDir = getCacheDir()
        val size = getFolderSize(cacheDir)
        return formatFileSize(size)
    }

    /**
     * 清除缓存
     */
    fun clearCache(): Boolean {
        return deleteDir(getCacheDir())
    }

    /**
     * 获取文件夹大小
     */
    private fun getFolderSize(folder: File): Long {
        var size = 0L
        try {
            val files = folder.listFiles()
            files?.forEach { file ->
                size += if (file.isDirectory) {
                    getFolderSize(file)
                } else {
                    file.length()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }

    /**
     * 格式化文件大小
     */
    private fun formatFileSize(size: Long): String {
        if (size < 1024) {
            return "$size B"
        }
        val kb = size / 1024.0
        if (kb < 1024) {
            return BigDecimal(kb).setScale(2, RoundingMode.HALF_UP).toString() + " KB"
        }
        val mb = kb / 1024.0
        if (mb < 1024) {
            return BigDecimal(mb).setScale(2, RoundingMode.HALF_UP).toString() + " MB"
        }
        val gb = mb / 1024.0
        return BigDecimal(gb).setScale(2, RoundingMode.HALF_UP).toString() + " GB"
    }

    /**
     * 删除目录
     */
    private fun deleteDir(dir: File?): Boolean {
        if (dir == null || !dir.exists()) return true
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { child ->
                deleteDir(child)
            }
        }
        return dir.delete()
    }
}