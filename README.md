# ChatAI
一款基于 Android 平台的简易智能 AI 助手 App，支持多会话管理、上下文对话、文本/图片/视频生成，具备消息持久化存储等功能。


## 一、项目介绍
### 1. 核心定位
轻量级 AI 助手客户端，聚焦「多会话隔离」「上下文记忆」「多模态生成」三大核心能力，适配主流 AI 接口（OpenAI / 自定义后端），支持本地数据库持久化存储，无服务端强依赖（可对接自有 AI 接口）。


### 2. 核心功能
| 功能模块         | 详细说明                                                                 |
|------------------|--------------------------------------------------------------------------|
| 多会话管理       | 支持创建/切换/删除会话，会话列表展示最后一条消息与时间，会话数据隔离存储   |
| 上下文对话       | AI 可记忆当前会话历史消息，基于完整上下文生成回复，支持上下文条数限制     |
| 多模态生成       | 文本对话、图像生成、视频生成（支持图片素材上传）                         |
| 消息持久化       | 所有消息/会话存储到本地 Room 数据库，重启 App 不丢失                     |
| 消息状态管理     | 加载中/成功/失败状态展示，失败消息支持重试，错误提示与兜底               |
| 交互体验优化     | 消息气泡、加载动画、返回键支持、输入框防抖、消息自动滚动到底部           |


### 3. 技术栈
| 技术分类   | 核心技术                                                                 |
|------------|--------------------------------------------------------------------------|
| 开发语言   | Kotlin（100% 纯 Kotlin 开发）                                            |
| 架构模式   | MVVM + 单向数据流（ViewModel + Repository + UI 分层）                    |
| 界面框架   | Jetpack Compose（声明式 UI，替代传统 XML）                               |
| 异步处理   | Kotlin Coroutines + Flow（协程 + 数据流，处理异步任务）                   |
| 依赖注入   | Hilt（可选，支持纯手动依赖创建）                                         |
| 本地存储   | Room 数据库（会话/消息持久化，支持表关联、数据迁移）                     |
| 网络请求   | Retrofit + OkHttp（对接 AI 接口，支持文本/图片/视频生成）                 |
| 其他       | Jetpack 组件（Lifecycle 生命周期管理、Navigation 页面导航、Material3 UI 组件） |


## 二、快速开始
### 1. 环境要求
- 开发工具：Android Studio Hedgehog | 2023.1.1 及以上
- Android 系统：Min SDK 24（Android 7.0），Target SDK 34
- JDK：17 及以上
- 网络：需对接可访问的 AI 接口（OpenAI 或自定义后端）


### 2. 源码拉取
```bash
# 克隆仓库
git clone https://github.com/csysx/chatai.git
cd ChatAI
# （可选）切换到代码分支
git checkout master
```


### 3. 配置 AI 接口
修改 `app/src/main/java/com/example/chatai/repository/RetrofitClient.kt` 中的接口配置：

```kotlin
object RetrofitClient {
    // 替换为你的 AI 接口地址（如 OpenAI 或自建后端）
    private const val BASE_URL = "https://api.openai.com/v1/"
    
    // 替换为你的 API Key（OpenAI 需配置，自建后端可删除）
    private const val API_KEY = "sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val request = chain.request()
                            .newBuilder()
                            .addHeader("Authorization", "Bearer $API_KEY") // OpenAI 鉴权
                            .build()
                        chain.proceed(request)
                    }
                    .build()
            )
            .build()
            .create(ApiService::class.java)
    }
}
```

### 4. 本地运行
1. 打开 Android Studio，导入项目（选择 ChatAI 根目录）；
2. 等待 Gradle 同步完成（首次同步需下载依赖）；
3. 连接 Android 真机 / 启动模拟器（API 24+）；
4. 点击「Run 'app'」按钮（绿色三角），选择设备后运行。

## 三、项目架构
### 1. 目录结构
```bash
app/src/main/java/com/example/chatai/
├── model/                  # 数据模型层
│   ├── data/               # 实体类（Session/ChatMessage/枚举）
│   ├── intent/             # 意图类（ChatIntent/SessionIntent，UI 行为封装）
│   └── state/              # 状态类（ChatUiState/SessionUiState，UI 状态封装）
├── repository/             # 仓库层（数据统一入口）
│   ├── local/              # 本地数据（Room DAO/数据库）
│   ├── RemoteChatRepository.kt  # 消息相关数据处理
│   ├── RemoteSessionRepository.kt # 会话相关数据处理
│   └── RetrofitClient.kt   # 网络请求配置
├── ui/                     # UI 层
│   ├── ChatScreen.kt       # 聊天页
│   ├── SessionListScreen.kt # 会话列表页
│   └── MainNavigation.kt   # 导航配置
├── viewmodel/              # 视图模型层
│   ├── ChatViewModel.kt    # 聊天页逻辑（消息/生成/上下文）
│   └── SessionViewModel.kt # 会话列表逻辑（会话管理/标题）
└── MainActivity.kt         # 应用入口
```


### 2. 核心流程
#### 会话创建与消息发送流程
用户点击「新建会话」→ SessionViewModel 创建会话（生成 UUID）→ 跳转到聊天页 → 用户输入文本 → ChatViewModel 存储用户消息 → 拼接上下文 → 调用 AI 接口 → 接收 AI 回复 → 存储 AI 消息 → 更新 UI 状态 → 渲染消息气泡

#### 数据流转原则
- UI → ViewModel：通过 Intent 传递用户行为（单向）；
- ViewModel → Repository：通过协程调用数据操作；
- Repository → 数据源：本地 Room / 远程 API；
- 数据源 → UI：通过 Flow 异步回调，StateFlow 驱动 UI 刷新。

## 四、功能使用说明
| 操作               | 操作方式                                                                 |
|--------------------|--------------------------------------------------------------------------|
| 创建会话           | 会话列表页点击「+」按钮                                                 |
| 切换会话           | 点击会话列表中的会话项                                                   |
| 删除会话           | 长按会话项 → 选择删除 / 会话列表页滑动删除                               |
| 发送文本消息       | 聊天页输入框输入文本 → 点击发送按钮                                     |
| 生成图像           | 切换到图像模式 → 输入提示词 → 点击生成按钮                               |
| 生成视频           | 切换到视频模式 → 选择图片素材 → 输入提示词 → 点击生成按钮                 |
| 重试失败消息       | 点击失败消息气泡 → 选择重试                                             |
| 清空会话消息       | 聊天页右上角菜单 → 选择「清空消息」                                     |
| 返回会话列表       | 聊天页左上角返回箭头 / 系统返回键                                       |

---
更新时间：2025-12-04  
如果本项目对你有帮助，欢迎 Star ⭐ 支持！