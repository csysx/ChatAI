package com.example.chatai.model.data

import com.google.gson.annotations.SerializedName

// 视频生成 API 请求体（）
data class VideoGenerationRequest(
    @SerializedName("model") val model: String,
    @SerializedName("prompt") val prompt: String,
    @SerializedName("negative_prompt") val negativePrompt: String = "色调艳丽,过曝,静态,细节模糊不清,字幕,风格,作品,画作,画面,静止,整体发灰,最差质量,低质量,JPEG压缩残留,丑陋的,残缺的,多余的手指,画得不好的手部,画得不好的脸部,畸形的,毁容的,形态畸形的肢体,手指融合,静止不动的画面,杂乱的背景,三条腿,背景人很多,倒着走",
    @SerializedName("image_size") val imageSize: String = "1280x720",
    @SerializedName("image") val image: String = "https://inews.gtimg.com/om_bt/Os3eJ8u3SgB3Kd-zrRRhgfR5hUvdwcVPKUTNO6O7sZfUwAA/641",
    @SerializedName("seed") val seed: Int = 123,
)


data class SubmitResponse(
    @SerializedName("requestId") val requestId: String
)



data class VideoStatusRequest(
    @SerializedName("requestId") val requestId: String
)


data class VideoStatusResponse(
    @SerializedName("status") val status: String,
    @SerializedName("reason") val reason: String?,
    @SerializedName("results") val results: VideoStatusResults?
)

data class VideoStatusResults(
    @SerializedName("videos") val videos: List<VideoStatusVideo>?,
    @SerializedName("timings") val timings: VideoTimings?,
    @SerializedName("seed") val seed: Int?
)

data class VideoStatusVideo(
    @SerializedName("url") val url: String
)

data class VideoTimings(
    @SerializedName("inference") val inference: Double?
)

