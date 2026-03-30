package com.tv.terminal.ui.poster

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.tv.terminal.R
import com.tv.terminal.data.remote.model.ContentItem
import com.tv.terminal.data.remote.model.PlayRule
import com.tv.terminal.databinding.FragmentPosterBinding
import com.tv.terminal.ui.main.MainActivity

/**
 * 海报展示页面
 * 支持单张静态展示和多张自动轮播
 */
class PosterFragment : Fragment() {

    private var _binding: FragmentPosterBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private var currentIndex = 0
    private var posterList: List<ContentItem> = emptyList()
    private var playRule: PlayRule? = null

    // 待加载的内容（用于 View 创建后加载）
    private var pendingContents: List<ContentItem>? = null
    private var pendingRule: PlayRule? = null

    private val autoPlayRunnable = object : Runnable {
        override fun run() {
            if (posterList.size > 1 && playRule?.loop == true) {
                currentIndex = (currentIndex + 1) % posterList.size
                showPoster(currentIndex)
                playRule?.duration?.let { duration ->
                    handler.postDelayed(this, duration * 1000L)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPosterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 如果有待加载的内容，现在加载
        pendingContents?.let { contents ->
            pendingRule?.let { rule ->
                loadContents(contents, rule)
                pendingContents = null
                pendingRule = null
            }
        }
    }

    /**
     * 设置海报内容
     * @param contents 海报列表
     * @param rule 播放规则
     */
    fun setContents(contents: List<ContentItem>, rule: PlayRule) {
        Log.d(TAG, "setContents: count=${contents.size}, duration=${rule.duration}, loop=${rule.loop}")

        if (_binding == null) {
            // View 还没创建，保存待加载的内容
            pendingContents = contents
            pendingRule = rule
            return
        }

        loadContents(contents, rule)
    }

    /**
     * 加载内容（内部方法）
     */
    private fun loadContents(contents: List<ContentItem>, rule: PlayRule) {
        this.posterList = contents
        this.playRule = rule
        this.currentIndex = 0

        // 停止之前的轮播
        stop()

        if (contents.isNotEmpty()) {
            showPoster(0)

            // 多张海报自动轮播
            if (contents.size > 1 && rule.loop) {
                handler.postDelayed(autoPlayRunnable, rule.duration * 1000L)
            }
        }
    }

    /**
     * 显示指定索引的海报
     */
    private fun showPoster(index: Int) {
        val poster = posterList.getOrNull(index) ?: return

        Log.d(TAG, "showPoster: index=$index, url=${poster.url}")

        // 确保在主线程且 Fragment 已附加
        if (!isAdded || _binding == null) {
            Log.w(TAG, "Fragment not attached or view is null, skipping showPoster")
            return
        }

        Glide.with(this)
            .load(poster.url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .transition(DrawableTransitionOptions.withCrossFade(500))
            .listener(object : RequestListener<Drawable> {
                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: com.bumptech.glide.load.DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d(TAG, "Image loaded: ${poster.name}")
                    // 上报播放状态
                    reportStatus(poster.id, "poster")
                    return false
                }

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.e(TAG, "Failed to load image: ${poster.url}", e)
                    return false
                }
            })
            .into(binding.imageView)
    }

    /**
     * 停止轮播
     */
    fun stop() {
        handler.removeCallbacks(autoPlayRunnable)
    }

    /**
     * 暂停轮播
     */
    fun pause() {
        Log.d(TAG, "pause")
        handler.removeCallbacks(autoPlayRunnable)
    }

    /**
     * 恢复轮播
     */
    fun resume() {
        Log.d(TAG, "resume")
        // 如果有多张海报且开启了循环，恢复轮播
        if (posterList.size > 1 && playRule?.loop == true) {
            handler.postDelayed(autoPlayRunnable, playRule!!.duration * 1000L)
        }
    }

    /**
     * 上报播放状态
     */
    private fun reportStatus(contentId: Long, contentType: String) {
        (activity as? MainActivity)?.reportPlayingStatus(contentId, contentType)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(autoPlayRunnable)
        _binding = null
    }

    companion object {
        private const val TAG = "PosterFragment"

        fun newInstance() = PosterFragment()
    }
}