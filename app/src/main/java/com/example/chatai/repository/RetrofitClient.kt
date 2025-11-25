package com.example.chatai.repository

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // 替换为你的 API Base URL
    private const val BASE_URL = "https://api.siliconflow.cn"

    // 替换为你的 API Key
    private const val API_KEY = "sk-wuymoymntcsphsttqaqckbhdshjzoevjmmfoodgtkxogpzas"

    val apiService: ApiService by lazy {
        // 创建一个日志拦截器，用于在控制台查看网络请求和响应
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY // 打印请求和响应的全部内容
        }

        // 创建 OkHttpClient，并添加拦截器
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                // 如果 API 需要认证，在这里添加 Header
                val requestWithAuth = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $API_KEY") // 例如 OpenAI 的认证方式
                    .build()
                chain.proceed(requestWithAuth)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        // 创建 Retrofit 实例
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}