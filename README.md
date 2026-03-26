# TvTerminal - 电视招聘展示终端

人力公司电视招聘展示系统的Android终端应用，用于在公司50台电视上展示招聘内容和职位信息。

## 功能特性

- **海报展示** - 支持招聘海报轮播展示，1920x1080分辨率适配
- **视频播放** - 宣传片视频自动循环播放
- **实时通信** - WebSocket连接后台，实时接收推送内容
- **心跳保活** - 定时心跳检测，保持在线状态
- **开机自启** - 电视开机自动启动应用
- **远程配置** - 支持后台远程配置终端参数

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| 最低版本 | Android 5.0 (API 21) |
| 目标版本 | Android 14 (API 34) |
| 架构 | MVVM + ViewBinding |
| 网络 | OkHttp + Retrofit |
| 视频播放 | ExoPlayer |
| 图片加载 | Glide |
| 异步处理 | Kotlin Coroutines |

## 项目结构

```
app/src/main/java/com/tv/terminal/
├── TvApplication.kt          # Application入口
├── data/
│   ├── local/
│   │   └── SharedPreferencesManager.kt  # 本地存储
│   └── remote/
│       ├── WebSocketManager.kt          # WebSocket管理
│       ├── HeartbeatManager.kt          # 心跳管理
│       └── model/
│           └── Message.kt               # 消息模型
├── receiver/
│   └── BootReceiver.kt      # 开机启动接收器
├── ui/
│   ├── main/
│   │   ├── MainActivity.kt   # 主界面
│   │   └── MainViewModel.kt # 主界面ViewModel
│   ├── poster/
│   │   └── PosterFragment.kt # 海报展示Fragment
│   ├── video/
│   │   └── VideoFragment.kt  # 视频播放Fragment
│   ├── splash/
│   │   └── SplashActivity.kt # 启动页
│   └── setting/
│       └── SettingActivity.kt # 设置页面
└── util/
    ├── DeviceUtils.kt       # 设备工具类
    ├── FileUtils.kt         # 文件工具类
    └── NetworkUtils.kt      # 网络工具类
```

## 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Gradle 8.x
- Android SDK 34

## 构建运行

### Debug版本
```bash
./gradlew assembleDebug
```

### Release版本
```bash
./gradlew assembleRelease
```

### 安装到设备
```bash
./gradlew installDebug
```

## 配置说明

### 服务器地址配置
在 `SettingActivity` 中可配置后台服务器地址，或修改 `SharedPreferencesManager` 中的默认值。

### 网络安全配置
应用允许明文流量（HTTP），配置文件位于 `res/xml/network_security_config.xml`。

## 权限说明

| 权限 | 用途 |
|------|------|
| INTERNET | 网络通信 |
| ACCESS_NETWORK_STATE | 网络状态检测 |
| ACCESS_WIFI_STATE | WiFi状态检测 |
| WRITE_EXTERNAL_STORAGE | 文件存储（Android 9及以下） |
| READ_EXTERNAL_STORAGE | 文件读取（Android 12及以下） |
| RECEIVE_BOOT_COMPLETED | 开机自启动 |
| WAKE_LOCK | 保持唤醒 |

## 相关项目

- **后台管理系统** - Spring Boot后端服务
- **前端管理界面** - Vue3管理后台

## 版本历史

- **v1.0.0** - 初始版本
  - 海报轮播展示
  - 视频播放
  - WebSocket实时通信
  - 开机自启

## License

Private - Internal Use Only