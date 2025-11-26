package com.example.chatai.repository

import com.example.chatai.model.data.ChatCompletionRequest
import com.example.chatai.model.data.ChatCompletionResponse
import com.example.chatai.model.data.ImageGenerationRequest
import com.example.chatai.model.data.ImageGenerationResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {

    // API 端点是 /v1/chat/completions
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(@Body request: ChatCompletionRequest): Response<ChatCompletionResponse>

    // 新增：图片生成接口
    @Headers("Content-Type: application/json")
    @POST("v1/images/generations")
    suspend fun generateImage(@Body request: ImageGenerationRequest): Response<ImageGenerationResponse>

}