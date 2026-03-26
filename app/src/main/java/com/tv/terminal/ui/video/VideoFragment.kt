package com.tv.terminal.ui.video

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.tv.terminal.data.remote.model.ContentItem
import com.tv.terminal.data.remote.model.PlayRule
import com.tv.terminal.databinding.FragmentVideoBinding
import com.tv.terminal.ui.main.MainActivity

/**
 * 视频播放页面
 * 使用 ExoPlayer 播放视频，支持循环播放和音量控制
 */
class VideoFragment : Fragment() {

    private var _binding: FragmentVideoBinding? = null
    private val binding get() = _binding!!

    private var player: ExoPlayer? = null
    private var videoList: List<ContentItem> = emptyList()
    private var currentIndex = 0
    private var currentRule: PlayRule? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializePlayer()
    }

    /**
     * 初始化播放器
     */
    private fun initializePlayer() {
        player = ExoPlayer.Builder(requireContext()).build().apply {
            binding.playerView.player = this
            addListener(playerListener)
        }
    }

    /**
     * 设置视频内容
     * @param contents 视频列表
     * @param rule 播放规则
     */
    fun setContents(contents: List<ContentItem>, rule: PlayRule) {
        Log.d(TAG, "setContents: count=${contents.size}, loop=${rule.loop}, volume=${rule.volume}")

        this.videoList = contents
        this.currentIndex = 0
        this.currentRule = rule

        if (contents.isNotEmpty()) {
            playVideo(0, rule)
        }
    }

    /**
     * 播放指定索引的视频
     */
    private fun playVideo(index: Int, rule: PlayRule) {
        val video = videoList.getOrNull(index) ?: return

        Log.d(TAG, "playVideo: index=$index, url=${video.url}")

        val mediaItem = MediaItem.fromUri(Uri.parse(video.url))

        player?.apply {
            setMediaItem(mediaItem)
            repeatMode = if (rule.loop && videoList.size == 1) {
                // 单视频循环
                Player.REPEAT_MODE_ONE
            } else if (rule.loop) {
                // 多视频列表循环
                Player.REPEAT_MODE_ALL
            } else {
                Player.REPEAT_MODE_OFF
            }
            // 音量：0-100 映射到 0f-1f
            volume = rule.volume / 100f
            prepare()
            playWhenReady = true
        }
    }

    /**
     * 播放器监听器
     */
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> {
                    // 播放准备就绪
                    Log.d(TAG, "Player ready")
                    videoList.getOrNull(currentIndex)?.let { video ->
                        reportStatus(video.id, "video")
                    }
                }
                Player.STATE_ENDED -> {
                    // 播放结束
                    Log.d(TAG, "Playback ended")
                    // 如果是多视频且非循环模式，播放下一个
                    if (videoList.size > 1 && currentRule?.loop == false) {
                        if (currentIndex < videoList.size - 1) {
                            currentIndex++
                            playVideo(currentIndex, currentRule!!)
                        }
                    }
                }
                Player.STATE_BUFFERING -> {
                    Log.d(TAG, "Player buffering")
                }
                Player.STATE_IDLE -> {
                    Log.d(TAG, "Player idle")
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Player error: ${error.message}", error)
            // 可以在此处添加重试逻辑
        }
    }

    /**
     * 暂停播放
     */
    fun pause() {
        Log.d(TAG, "pause")
        player?.pause()
    }

    /**
     * 恢复播放
     */
    fun resume() {
        Log.d(TAG, "resume")
        player?.play()
    }

    /**
     * 停止播放
     */
    fun stop() {
        Log.d(TAG, "stop")
        player?.stop()
    }

    /**
     * 设置音量
     * @param volume 音量值 0-100
     */
    fun setVolume(volume: Int) {
        player?.volume = volume / 100f
    }

    /**
     * 上报播放状态
     */
    private fun reportStatus(contentId: Long, contentType: String) {
        (activity as? MainActivity)?.reportPlayingStatus(contentId, contentType)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: releasing player")
        player?.release()
        player = null
        _binding = null
    }

    companion object {
        private const val TAG = "VideoFragment"

        fun newInstance() = VideoFragment()
    }
}