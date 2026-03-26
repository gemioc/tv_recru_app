package com.tv.terminal.data.remote.model

import com.google.gson.annotations.SerializedName
import com.google.gson.annotations.Expose

/**
 * 消息基类
 * 注意：type字段由子类提供，父类使用@Transient避免GSON序列化冲突
 */
abstract class ClientMessage {
    @Transient
    open val type: String = ""
}

abstract class ServerMessage {
    @Transient
    open val type: String = ""
}

// ===================== 心跳消息 =====================

/**
 * 心跳请求
 */
data class HeartbeatMessage(
    @SerializedName("type")
    override val type: String = "HEARTBEAT",
    @SerializedName("deviceCode")
    val deviceCode: String
) : ClientMessage()

/**
 * 心跳响应
 */
data class HeartbeatAckMessage(
    @SerializedName("type")
    override val type: String = "HEARTBEAT_ACK",
    @SerializedName("timestamp")
    val timestamp: Long
) : ServerMessage()

// ===================== 状态上报 =====================

/**
 * 状态上报消息
 */
data class StatusReportMessage(
    @SerializedName("type")
    override val type: String = "STATUS_REPORT",
    @SerializedName("deviceCode")
    val deviceCode: String,
    @SerializedName("data")
    val data: DeviceStatus
) : ClientMessage()

/**
 * 设备状态
 */
data class DeviceStatus(
    @SerializedName("isPlaying")
    val isPlaying: Boolean,
    @SerializedName("currentContentId")
    val currentContentId: Long?,
    @SerializedName("currentContentType")
    val currentContentType: String?,
    @SerializedName("playProgress")
    val playProgress: Int = 0,
    @SerializedName("errorMessage")
    val errorMessage: String? = null
)

// ===================== 推送确认 =====================

/**
 * 推送确认消息
 */
data class PushAckMessage(
    @SerializedName("type")
    override val type: String = "PUSH_ACK",
    @SerializedName("messageId")
    val messageId: String,
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("errorMessage")
    val errorMessage: String? = null
) : ClientMessage()

// ===================== 内容推送 =====================

/**
 * 内容推送消息
 */
data class PushContentMessage(
    @SerializedName("type")
    override val type: String = "PUSH_CONTENT",
    @SerializedName("messageId")
    val messageId: String,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("data")
    val data: ContentData
) : ServerMessage()

/**
 * 内容数据
 */
data class ContentData(
    @SerializedName("contentType")
    val contentType: String,  // "poster" or "video"
    @SerializedName("contentUrl")
    val contentUrl: String? = null,  // 兼容旧格式
    @SerializedName("contents")
    val contents: List<ContentItem>? = null,  // 新格式
    @SerializedName("playRule")
    val playRule: PlayRule? = null
)

/**
 * 内容项
 */
data class ContentItem(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("duration")
    val duration: Int? = null
)

/**
 * 播放规则
 */
data class PlayRule(
    @SerializedName("duration")
    val duration: Int = 10,
    @SerializedName("loop")
    val loop: Boolean = true,
    @SerializedName("interval")
    val interval: Int = 0,
    @SerializedName("volume")
    val volume: Int = 80
)

// ===================== 控制指令 =====================

/**
 * 控制消息
 */
data class ControlMessage(
    @SerializedName("type")
    override val type: String = "CONTROL",
    @SerializedName("data")
    val data: ControlData
) : ServerMessage()

/**
 * 控制数据
 */
data class ControlData(
    @SerializedName("action")
    val action: String
)