# LUT 应用设置持久化功能

## 概述

本次更新为 LUT 应用添加了设置持久化功能，解决了每次重启应用都需要重新设置文件夹路径的问题。

## 新增功能

### SettingsManager 类

新增了 `SettingsManager` 类，位于 `cn.jetNest.lut.data` 包中，负责管理应用的所有设置项：

- **监控目录** (`watchDirectory`)
- **输出目录** (`outputDirectory`) 
- **选中的LUT文件** (`selectedLutFile`)
- **强度设置** (`strength`)
- **质量设置** (`quality`)
- **抖动类型** (`ditherType`)

### 持久化存储

使用 Android 的 `SharedPreferences` 进行数据持久化存储：

- 设置文件名：`lut_settings`
- 存储模式：`Context.MODE_PRIVATE`
- 支持实时数据更新，使用 `StateFlow` 提供响应式数据流

### 默认值

- 强度：60
- 质量：90
- 其他设置项：null（未设置状态）

## 技术实现

### 数据流架构

```
UI Layer (Compose) 
    ↕ 
MainViewModel 
    ↕ 
SettingsManager 
    ↕ 
SharedPreferences
```

### 主要方法

#### 设置方法
- `setWatchDirectory(path: String)`
- `setOutputDirectory(path: String)`
- `setSelectedLutFile(filename: String)`
- `setStrength(value: Int)`
- `setQuality(value: Int)`
- `setDitherType(type: String?)`

#### 获取方法
- 所有设置项都通过 `StateFlow` 提供响应式数据流
- 支持实时监听设置变化

#### 管理方法
- `clearAllSettings()` - 清除所有设置

## 使用方式

设置持久化功能已自动集成到现有的 UI 中，用户无需额外操作：

1. 在设置页面配置文件夹路径
2. 设置会自动保存到本地存储
3. 重启应用后，之前的设置会自动恢复

## 测试

添加了基础的单元测试 `SettingsManagerTest`，验证：

- 类的存在性和可实例化性
- 默认值的正确性
- 设置键名的正确定义
- SharedPreferences 文件名的正确性

## 兼容性

- 最低 Android API 24
- 向后兼容，不影响现有功能
- 首次使用时会使用默认值，之后会记住用户设置

## 文件变更

### 新增文件
- `app/src/main/java/cn/jetNest/lut/data/SettingsManager.kt`
- `app/src/test/java/cn/jetNest/lut/data/SettingsManagerTest.kt`

### 修改文件
- `app/src/main/java/cn/jetNest/lut/viewmodel/MainViewModel.kt`
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`

## 注意事项

- 设置数据存储在应用私有目录，卸载应用时会一并删除
- 强度和质量设置有范围限制（强度：0-100，质量：1-100）
- 所有设置操作都是线程安全的