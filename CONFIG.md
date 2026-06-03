# 项目配置指南

## OpenAI API 配置

本应用使用 OpenAI GPT-4V API 进行物品图像识别。您需要配置 API 密钥才能使用 AI 识别功能。

### 配置步骤

1. **获取 OpenAI API 密钥**
   - 访问 [OpenAI Platform](https://platform.openai.com/)
   - 注册账户并创建 API 密钥

2. **配置本地密钥**
   - 复制 `local.properties.template` 文件并重命名为 `local.properties`
   - 在 `local.properties` 文件中填入您的 API 密钥：
     ```properties
     OPENAI_API_KEY=sk-your-actual-api-key-here
     OPENAI_API_ENDPOINT=https://api.openai.com
     ```

3. **验证配置**
   - 确保 `local.properties` 文件不会被提交到版本控制（已在 .gitignore 中配置）
   - 构建项目，API 密钥将自动注入到 BuildConfig 中

### 安全注意事项

- ⚠️ **永远不要将 API 密钥提交到版本控制系统**
- ⚠️ `local.properties` 文件仅用于本地开发
- ⚠️ 生产环境应使用安全的密钥管理服务

### 如果没有 API 密钥

如果您暂时没有 OpenAI API 密钥，应用仍可运行，但 AI 图像识别功能将不可用。用户需要手动输入物品信息。

## 依赖项说明

项目已配置以下主要依赖：

### 核心框架
- **Kotlin**: 开发语言
- **Jetpack Compose**: UI 框架
- **Hilt**: 依赖注入

### 数据层
- **Room**: 本地数据库
- **Retrofit**: 网络请求
- **OkHttp**: HTTP 客户端
- **Kotlinx Serialization**: JSON 序列化

### UI & 资源
- **Coil**: 图片加载
- **Material3**: Material Design 组件
- **Navigation Compose**: 导航

### 异步处理
- **Kotlin Coroutines**: 协程
- **Flow**: 响应式数据流

### 位置服务
- **Play Services Location**: Google 定位服务

### 测试框架
- **JUnit**: 单元测试框架
- **Kotest**: Property-based Testing
- **MockK**: Mock 框架
- **Turbine**: Flow 测试
- **Coroutines Test**: 协程测试

## 构建项目

```bash
# 同步 Gradle 依赖
./gradlew build

# 运行应用
./gradlew installDebug

# 运行测试
./gradlew test
```

## 故障排除

### Gradle 同步失败
- 确保网络连接正常
- 检查 Gradle 版本兼容性
- 清理并重新构建：`./gradlew clean build`

### BuildConfig 找不到
- 确保启用了 `buildFeatures { buildConfig = true }`
- 重新同步 Gradle

### API 密钥未生效
- 检查 `local.properties` 文件是否在项目根目录
- 检查文件中的密钥格式是否正确
- 重新同步 Gradle 并重新构建项目
