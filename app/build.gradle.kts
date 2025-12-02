plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.chatai"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.chatai"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        // 添加这一行，强制 Kapt 纠正存根生成
        freeCompilerArgs += listOf("-Xjvm-default=all")
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)


    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.material3)
    implementation(libs.androidx.benchmark.traceprocessor)
    implementation(libs.androidx.camera.camera2.pipe)
    implementation(libs.androidx.compose.foundation)




    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ViewModel + StateFlow（MVI 状态管理核心）
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx")  // ViewModel 核心
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")  // ViewModel 与 Compose 绑定
    implementation("androidx.lifecycle:lifecycle-livedata-ktx")  // 可选（若需 LiveData 兼容）

    // 协程（异步处理，如模拟网络延迟）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android")  // Android 专用协程库

    // ----------- Retrofit / OkHttp -------------
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation("javax.inject:javax.inject:1")
    implementation("io.coil-kt:coil-compose:2.4.0")
    // 添加 Hilt 依赖
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-android-compiler:2.51.1")

     // room数据库本地存储
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    implementation(libs.exoplayer.core)
    implementation(libs.exoplayer.ui)

    implementation(libs.androidx.appcompat)

}

// 如果你的文件顶部 plugins 里用的是 id("kotlin-kapt")
kapt {
    correctErrorTypes = true
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
        // 关键：告诉 Room 将 Kotlin 的 Unit 视为 Java 的 void
        arg("room.incremental", "true")
    }
}
