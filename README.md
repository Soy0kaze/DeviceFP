# DeviceFP

Android 设备指纹信息收集应用。一键采集设备硬件与系统信息，支持导出为 JSON，便于开发调试与设备识别。

## 功能特性

- **多维度指纹采集**：电池、相机、CPU、内存、屏幕、存储、WiFi、SIM 等
- **CPU 频率**：实时读取并展示 CPU 频率表与图表（含 Native 实现）
- **一键收集**：主界面点击即可异步收集并展示分类结果
- **导出 JSON**：支持将指纹数据导出为 JSON 文件
- **Material Design**：支持日/夜主题

## 技术栈

- **语言**：Java、C++（CMake）
- **最低 SDK**：29 | **目标 SDK**：36
- **构建**：Gradle 8.x、Android Gradle Plugin 8.x
- **UI**：ViewBinding、Material Components、RecyclerView

## 环境要求

- Android Studio 或兼容 IDE
- JDK 11+
- Android SDK 29+

## 构建与运行

```bash
# 克隆仓库
git clone https://github.com/Soy0kaze/DeviceFP.git
cd DeviceFP

# 使用 Android Studio 打开项目，或命令行构建
./gradlew assembleDebug   # Linux/macOS
gradlew.bat assembleDebug # Windows
```

安装生成的 APK：`app/build/outputs/apk/debug/DeviceFP_1.0.apk`

## 项目结构

```
app/src/main/
├── java/com/kaze/devicefp/
│   ├── adapter/     # RecyclerView 适配器
│   ├── model/       # 数据模型与信息读取（电池、CPU、屏幕等）
│   ├── service/     # 指纹收集服务
│   ├── util/        # 工具类
│   └── view/        # 自定义视图（CPU 频率图表/表格）
└── cpp/             # Native 层（CPU 等）
```

## 权限说明

应用会申请网络、存储、位置、蓝牙、电话状态等权限，仅用于读取设备信息以生成指纹，不会上传或外发数据。

## 许可证

[MIT](LICENSE)
