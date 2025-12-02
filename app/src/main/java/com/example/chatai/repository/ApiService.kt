package com.example.chatai.repository

import com.example.chatai.model.data.ChatCompletionRequest
import com.example.chatai.model.data.ChatCompletionResponse
import com.example.chatai.model.data.ImageGenerationRequest
import com.example.chatai.model.data.ImageGenerationResponse
import com.example.chatai.model.data.SubmitResponse
import com.example.chatai.model.data.VideoGenerationRequest
import com.example.chatai.model.data.VideoStatusRequest
import com.example.chatai.model.data.VideoStatusResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    // API 端点是 /v1/chat/completions
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(@Body request: ChatCompletionRequest): Response<ChatCompletionResponse>

    // 新增：图片生成接口
    @Headers("Content-Type: application/json")
    @POST("v1/images/generations")
    suspend fun generateImage(@Body request: ImageGenerationRequest): Response<ImageGenerationResponse>


    @Headers("Content-Type: application/json")
    @POST("v1/video/submit")
    suspend fun submitVideoTask(@Body req: VideoGenerationRequest): Response<SubmitResponse>


    @Headers("Content-Type: application/json")
    @POST("v1/video/status")
    suspend fun checkVideoStatus(@Body req: VideoStatusRequest): Response<VideoStatusResponse>


    @Multipart
    @POST("/v1/video/submit")
    suspend fun submitVideoWithImage(
        @Part("model") model: RequestBody,
        @Part("prompt") prompt: RequestBody,
        @Part("negative_prompt") negativePrompt: RequestBody,
        @Part("image_size") imageSize: RequestBody,
        @Part image: MultipartBody.Part,
        @Part("seed") seed: RequestBody
    ): Response<SubmitResponse>
}