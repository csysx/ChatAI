// app/src/main/java/com/example/chatai/model/data/ImageGenerationRequest.kt
package com.example.chatai.model.data

data class ImageGenerationRequest(
    val model: String = "stable-diffusion-v1-5", // SiliconFlow 支持的图像模型
    val prompt: String,                          // 图像描述文本
    val n: Int = 1,                              // 生成图片数量
    val size: String = "512x512"                 // 图片尺寸
)

// 图像生成响应模型
data class ImageGenerationResponse(
    val created: Long,
    val data: List<ImageData>
)

data class ImageData(
    val url: String // 生成的图片 URL
)