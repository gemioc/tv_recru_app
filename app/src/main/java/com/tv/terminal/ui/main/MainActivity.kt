package com.tv.terminal.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.tv.terminal.R
import com.tv.terminal.data.local.SharedPreferencesManager
import com.tv.terminal.data.remote.HeartbeatManager
import com.tv.terminal.data.remote.WebSocketManager
import com.tv.terminal.data.remote.model.*
import com.tv.terminal.databinding.ActivityMainBinding
import com.tv.terminal.ui.poster.PosterFragment
import com.tv.terminal.ui.setting.SettingActivity
import com.tv.terminal.ui.video.VideoFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 主界面
 * 内容展示容器，处理 WebSocket 消息和内容切换
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private lateinit var webSocketManager: WebSocketManager
    private lateinit var heartbeatManager: HeartbeatManager

    private var posterFragment: PosterFragment? = null
    private var videoFragment: VideoFragment? = null

    // 长按检测
    private var longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 全屏显示
        setupFullScreen()

        // 初始化 WebSocket
        initWebSocket()

        // 显示设备编码
        binding.deviceCodeText.text = "设备: ${SharedPreferencesManager.getDeviceCode()}"

        // 观察数据
        observeData()

        // 更新时间
        startTimeUpdater()

        // 设置长按监听
        setupLongPressListener()
    }

    /**
     * 设置全屏
     */
    private fun setupFullScreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    /**
     * 初始化 WebSocket
     */
    private fun initWebSocket() {
        val deviceCode = SharedPreferencesManager.getDeviceCode()
        val serverUrl = SharedPreferencesManager.getServerUrl()

        webSocketManager = WebSocketManager(deviceCode)
        heartbeatManager = HeartbeatManager(webSocketManager, deviceCode)

        // 连接服务器
        webSocketManager.connect(serverUrl)

        // 监听消息
        lifecycleScope.launch {
            webSocketManager.messageFlow.collect { message ->
                handleMessage(message)
            }
        }

        // 监听连接状态
        lifecycleScope.launch {
            webSocketManager.connectionState.collect { state ->
                updateConnectionState(state)
            }
        }

        // 启动心跳
        heartbeatManager.start()
    }

    /**
     * 处理服务端消息
     */
    private fun handleMessage(message: ServerMessage) {
        Log.d(TAG, "Handling message: ${message.type}")

        when (message) {
            is PushContentMessage -> handlePushContent(message)
            is ControlMessage -> handleControl(message)
            is HeartbeatAckMessage -> {
                // 心跳响应
                heartbeatManager.resetCount()
                viewModel.updateConnectionState(true)
            }
        }
    }

    /**
     * 处理内容推送
     */
    private fun handlePushContent(message: PushContentMessage) {
        val contentData = message.data

        Log.d(TAG, "Push content: type=${contentData.contentType}")

        // 兼容两种格式：单内容URL 或 多内容列表
        val contents = contentData.contents ?: listOf(
            ContentItem(
                id = 0,
                name = "content",
                url = contentData.contentUrl ?: ""
            )
        )
        val rule = contentData.playRule ?: PlayRule()

        when (contentData.contentType) {
            "poster" -> showPoster(contents, rule)
            "video" -> showVideo(contents, rule)
        }

        // 发送推送确认
        webSocketManager.send(PushAckMessage(
            messageId = message.messageId,
            success = true
        ))
    }

    /**
     * 处理控制指令
     */
    private fun handleControl(message: ControlMessage) {
        Log.d(TAG, "Control action: ${message.data.action}")

        when (message.data.action) {
            "restart" -> restartApp()
            "pause" -> {
                videoFragment?.pause()
                posterFragment?.pause()
            }
            "resume" -> {
                videoFragment?.resume()
                posterFragment?.resume()
            }
        }
    }

    /**
     * 显示海报
     */
    private fun showPoster(contents: List<ContentItem>, rule: PlayRule) {
        // 停止视频
        videoFragment?.stop()

        // 获取服务器地址用于拼接相对路径
        val serverUrl = SharedPreferencesManager.getServerUrl()

        // 处理URL：如果是相对路径则拼接服务器地址
        val processedContents = contents.map { item ->
            if (item.url.startsWith("http")) {
                item
            } else {
                item.copy(url = "$serverUrl${item.url}")
            }
        }

        // 创建或显示海报 Fragment
        if (posterFragment == null) {
            posterFragment = PosterFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(R.id.contentContainer, posterFragment!!)
                .commitNow()
        } else {
            supportFragmentManager.beginTransaction()
                .show(posterFragment!!)
                .commitNow()
        }

        // 隐藏视频
        videoFragment?.let {
            supportFragmentManager.beginTransaction()
                .hide(it)
                .commitNow()
        }

        // 设置内容（此时 Fragment 已附加）
        posterFragment?.setContents(processedContents, rule)
    }

    /**
     * 显示视频
     */
    private fun showVideo(contents: List<ContentItem>, rule: PlayRule) {
        // 停止海报
        posterFragment?.stop()

        // 获取服务器地址用于拼接相对路径
        val serverUrl = SharedPreferencesManager.getServerUrl()

        // 处理URL：如果是相对路径则拼接服务器地址
        val processedContents = contents.map { item ->
            if (item.url.startsWith("http")) {
                item
            } else {
                item.copy(url = "$serverUrl${item.url}")
            }
        }

        // 创建或显示视频 Fragment
        if (videoFragment == null) {
            videoFragment = VideoFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(R.id.contentContainer, videoFragment!!)
                .commitNow()
        } else {
            supportFragmentManager.beginTransaction()
                .show(videoFragment!!)
                .commitNow()
        }

        // 隐藏海报
        posterFragment?.let {
            supportFragmentManager.beginTransaction()
                .hide(it)
                .commitNow()
        }

        // 设置内容（此时 Fragment 已附加）
        videoFragment?.setContents(processedContents, rule)
    }

    /**
     * 更新连接状态
     */
    private fun updateConnectionState(state: com.tv.terminal.data.remote.ConnectionState) {
        val isConnected = state == com.tv.terminal.data.remote.ConnectionState.Connected
        viewModel.updateConnectionState(isConnected)
    }

    /**
     * 上报播放状态
     */
    fun reportPlayingStatus(contentId: Long, contentType: String) {
        val deviceCode = SharedPreferencesManager.getDeviceCode()
        val status = DeviceStatus(
            isPlaying = true,
            currentContentId = contentId,
            currentContentType = contentType
        )
        webSocketManager.send(StatusReportMessage(
            deviceCode = deviceCode,
            data = status
        ))
    }

    /**
     * 观察数据
     */
    private fun observeData() {
        lifecycleScope.launch {
            viewModel.isConnected.collect { isConnected ->
                updateConnectionUI(isConnected)
            }
        }
    }

    /**
     * 更新连接状态 UI
     */
    private fun updateConnectionUI(isConnected: Boolean) {
        if (isConnected) {
            binding.statusIndicator.setBackgroundResource(R.drawable.status_indicator_online)
            binding.statusText.text = getString(R.string.status_online)
        } else {
            binding.statusIndicator.setBackgroundResource(R.drawable.status_indicator_offline)
            binding.statusText.text = getString(R.string.status_offline)
        }
    }

    /**
     * 启动时间更新器
     */
    private fun startTimeUpdater() {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        val runnable = object : Runnable {
            override fun run() {
                binding.timeText.text = timeFormat.format(Date())
                longPressHandler.postDelayed(this, TIME_UPDATE_INTERVAL)
            }
        }

        longPressHandler.post(runnable)
    }

    /**
     * 设置长按监听
     * 长按屏幕 5 秒进入设置页
     */
    private fun setupLongPressListener() {
        binding.rootContainer.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    longPressRunnable = Runnable {
                        openSetting()
                    }
                    longPressHandler.postDelayed(longPressRunnable!!, LONG_PRESS_DURATION)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let {
                        longPressHandler.removeCallbacks(it)
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 打开设置页
     */
    private fun openSetting() {
        val intent = Intent(this, SettingActivity::class.java)
        startActivity(intent)
    }

    /**
     * 重启应用
     */
    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        heartbeatManager.stop()
        webSocketManager.disconnect()
        longPressHandler.removeCallbacksAndMessages(null)
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val TIME_UPDATE_INTERVAL = 60_000L // 1分钟更新一次时间
        private const val LONG_PRESS_DURATION = 5000L // 5秒长按
    }
}
