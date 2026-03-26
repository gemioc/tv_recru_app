package com.tv.terminal.ui.main

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 主界面 ViewModel
 */
class MainViewModel : ViewModel() {

    // 连接状态
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    // 当前播放内容ID
    private val _currentContentId = MutableStateFlow<Long?>(null)
    val currentContentId: StateFlow<Long?> = _currentContentId.asStateFlow()

    // 当前播放内容类型
    private val _currentContentType = MutableStateFlow<String?>(null)
    val currentContentType: StateFlow<String?> = _currentContentType.asStateFlow()

    /**
     * 更新连接状态
     */
    fun updateConnectionState(connected: Boolean) {
        _isConnected.value = connected
    }

    /**
     * 更新播放状态
     */
    fun updatePlayingStatus(contentId: Long?, contentType: String?) {
        _currentContentId.value = contentId
        _currentContentType.value = contentType
    }
}