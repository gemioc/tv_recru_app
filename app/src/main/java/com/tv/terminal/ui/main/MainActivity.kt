package com.tv.terminal.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.tv.terminal.R
import com.tv.terminal.data.local.SharedPreferencesManager
import com.tv.terminal.data.remote.HeartbeatManager
import com.tv.terminal.service.KeepAliveService
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
    private var wakeLock: PowerManager.WakeLock? = null

    private var posterFragment: PosterFragment? = null
    private var videoFragment: VideoFragment? = null

    // 服务器时间偏移量（用于同步真实时间）
    private var serverTimeOffset = 0L

    // 长按检测
    private var longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null

    // WakeLock 续期 Handler
    private val wakeLockHandler = Handler(Looper.getMainLooper())
    private var wakeLockRenewer: Runnable? = null

    // 用户 activity 模拟 Handler（防止某些定制固件息屏）
    private val userActivityHandler = Handler(Looper.getMainLooper())
    private var userActivityRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 保持屏幕常亮（多重保障）
        acquireAndKeepWakeLock()

        // 启动前台服务保活
        KeepAliveService.start(this)

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
     * 保持屏幕常亮 - 多重保障机制
     * 1. FLAG_KEEP_SCREEN_ON - 基本保障
     * 2. WakeLock - 强制唤醒
     * 3. WakeLock 续期 - 防止超时释放
     * 4. 用户 activity 模拟 - 防止国产定制固件息屏
     */
    private fun acquireAndKeepWakeLock() {
        // 1. FLAG_KEEP_SCREEN_ON（基本保障，最简单有效）
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Log.d(TAG, "FLAG_KEEP_SCREEN_ON enabled")

        // 2. WakeLock（强制唤醒，防止系统休眠）
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "TvTerminal:WakeLock"
        )
        wakeLock?.acquire() // 不带超时，持续有效直到主动 release
        Log.d(TAG, "WakeLock acquired")

        // 3. 启动 WakeLock 续期（防止系统强制释放）
        startWakeLockRenewer()

        // 4. 启动用户 activity 模拟（针对国产定制固件）
        startUserActivitySimulation()
    }

    /**
     * 启动 WakeLock 续期机制
     * 每 8 分钟检查并续期一次（在超时前 2 分钟续期）
     */
    private fun startWakeLockRenewer() {
        wakeLockRenewer = object : Runnable {
            override fun run() {
                wakeLock?.let {
                    if (!it.isHeld) {
                        it.acquire()
                        Log.d(TAG, "WakeLock renewed")
                    }
                }
                wakeLockHandler.postDelayed(this, WAKELOCK_RENEW_INTERVAL)
            }
        }
        // 延迟 8 分钟后开始续期（WakeLock 默认 10 分钟超时）
        wakeLockHandler.postDelayed(wakeLockRenewer!!, WAKELOCK_RENEW_INTERVAL)
        Log.d(TAG, "WakeLock renewer started, interval: ${WAKELOCK_RENEW_INTERVAL}ms")
    }

    /**
     * 启动用户 activity 模拟
     * 某些国产定制固件的 TV 设备会在无操作后强制息屏，
     * 通过模拟用户按键事件防止息屏
     */
    private fun startUserActivitySimulation() {
        userActivityRunnable = object : Runnable {
            override fun run() {
                try {
                    // 模拟耳机按钮按下事件（不会产生声音或实际效果）
                    val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK)
                    dispatchKeyEvent(eventDown)

                    // 模拟松开
                    val eventUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK)
                    dispatchKeyEvent(eventUp)

                    Log.d(TAG, "User activity simulated")
                } catch (e: Exception) {
                    Log.e(TAG, "User activity simulation failed: ${e.message}")
                }
                userActivityHandler.postDelayed(this, USER_ACTIVITY_INTERVAL)
            }
        }
        // 延迟 1 分钟后开始模拟（每 3 分钟模拟一次）
        userActivityHandler.postDelayed(userActivityRunnable!!, USER_ACTIVITY_INITIAL_DELAY)
        Log.d(TAG, "User activity simulation started, interval: ${USER_ACTIVITY_INTERVAL}ms")
    }

    /**
     * 唤醒屏幕（收到推送内容时调用）
     */
    private fun wakeUpScreen() {
        Log.d(TAG, "Waking up screen...")
        // 点亮屏幕
        wakeLock?.let {
            if (!it.isHeld) {
                it.acquire()
            }
        }
        // 唤醒屏幕
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
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

        // 监听消息 - 使用全局作用域，不受生命周期影响
        webSocketManager.messageFlow.let { flow ->
            lifecycleScope.launch {
                flow.collect { message ->
                    handleMessage(message)
                }
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
                // 同步服务器时间
                syncServerTime(message.timestamp)
            }
        }
    }

    /**
     * 处理内容推送
     */
    private fun handlePushContent(message: PushContentMessage) {
        val contentData = message.data

        Log.d(TAG, "Push content: type=${contentData.contentType}")

        // 唤醒屏幕
        wakeUpScreen()

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
            "image" -> showPoster(contents, rule) // 图片复用海报展示逻辑
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
            "wakeup" -> wakeUpScreen()
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
        timeFormat.timeZone = TimeZone.getDefault()

        // 立即更新时间，不等待定时器
        binding.timeText.text = timeFormat.format(getServerTime())

        val runnable = object : Runnable {
            override fun run() {
                binding.timeText.text = timeFormat.format(getServerTime())
                longPressHandler.postDelayed(this, TIME_UPDATE_INTERVAL)
            }
        }

        longPressHandler.postDelayed(runnable, TIME_UPDATE_INTERVAL)
    }

    /**
     * 获取服务器时间（本地时间 + 偏移量）
     */
    private fun getServerTime(): Date {
        return Date(System.currentTimeMillis() + serverTimeOffset)
    }

    /**
     * 同步服务器时间
     * @param serverTimestamp 服务器时间戳
     */
    private fun syncServerTime(serverTimestamp: Long) {
        // 计算偏移量
        serverTimeOffset = serverTimestamp - System.currentTimeMillis()
        Log.d(TAG, "服务器时间同步完成，偏移量: ${serverTimeOffset}ms")
        // 立即更新时间显示
        binding.timeText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(getServerTime())
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

        // 停止 WakeLock 续期
        wakeLockRenewer?.let { wakeLockHandler.removeCallbacks(it) }

        // 停止用户 activity 模拟
        userActivityRunnable?.let { userActivityHandler.removeCallbacks(it) }

        // 释放 WakeLock
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released")
            }
        }

        // 停止保活服务
        KeepAliveService.stop(this)
    }

    /**
     * 遥控器按键处理
     * 按菜单键或播放键打开设置页面
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_MENU -> {
                openSetting()
                return true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                openSetting()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val TIME_UPDATE_INTERVAL = 60_000L // 1分钟更新一次时间
        private const val USER_ACTIVITY_INITIAL_DELAY = 60_000L // 1分钟后开始模拟
        private const val LONG_PRESS_DURATION = 5000L // 5秒长按
        private const val WAKELOCK_RENEW_INTERVAL = 8 * 60 * 1000L // 8分钟续期一次
        private const val USER_ACTIVITY_INTERVAL = 3 * 60 * 1000L // 3分钟模拟一次用户 activity
    }
}
