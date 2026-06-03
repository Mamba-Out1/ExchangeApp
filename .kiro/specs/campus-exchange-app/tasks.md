# 实现计划: 校园二手物品交换应用

## Overview

本实现计划将校园二手物品交换应用的设计转换为可执行的开发任务。应用采用Kotlin语言开发,使用Jetpack Compose构建UI,基于MVVM+Repository架构模式。核心功能包括AI图像识别、智能推荐引擎、以物易物匹配系统和即时通讯。

实现顺序遵循自底向上的原则:先搭建数据层和领域层基础设施,再实现核心算法逻辑,最后构建UI层。每个阶段都包含相应的测试任务,确保增量验证。

## Tasks

- [x] 1. 配置项目依赖和构建环境
  - 配置Gradle依赖项,包括Room、Retrofit、Hilt、Coil、Kotest等
  - 配置OpenAI API密钥管理(使用BuildConfig和local.properties)
  - 设置测试框架(JUnit、Kotest Property Testing、MockK、Turbine)
  - 配置Hilt依赖注入
  - _Requirements: 1.1, 13.1_

- [ ] 2. 实现数据层基础设施
  - [x] 2.1 创建Domain Models
    - 实现Item、User、Order、ChatMessage、Location、UserInteraction等数据模型
    - 定义ItemStatus、OrderStatus、MessageType等枚举类型
    - _Requirements: 5.2, 7.2, 8.2, 9.3, 10.1_
   
  - [ ] 2.2 创建Room Database Entities和Converters
    - 实现ItemEntity、UserEntity、OrderEntity、ChatMessageEntity、UserInteractionEntity
    - 实现Converters类处理类型转换(JSON序列化、时间戳转换)
    - _Requirements: 2.1, 2.2, 2.3_
  
  - [ ] 2.3 实现Room DAOs
    - 实现ItemDao(查询、插入、更新、删除物品)
    - 实现UserDao(用户CRUD操作)
    - 实现OrderDao(订单管理)
    - 实现ChatDao(聊天记录、消息流观察)
    - 实现UserInteractionDao(用户交互记录)
    - _Requirements: 2.4, 2.5, 2.6, 9.3_
  
  - [ ] 2.4 创建AppDatabase类
    - 配置Room数据库(版本、实体、类型转换器)
    - 定义数据库访问接口
    - _Requirements: 2.1, 2.7_
  
  - [ ]* 2.5 编写数据序列化Property Test
    - **Property 8: Item序列化Round-Trip属性**
    - **验证需求: Requirements 12.5**
    - 测试Item对象序列化和反序列化的等价性
    - _Requirements: 12.5_

- [ ] 3. 实现OpenAI API集成
  - [ ] 3.1 创建OpenAI API接口和DTO
    - 定义OpenAIApiService接口
    - 实现ImageAnalysisRequest、Message、Content、ImageUrl等请求DTO
    - 实现ImageAnalysisResponse、Choice、MessageResponse等响应DTO
    - _Requirements: 1.1, 1.2_
  
  - [ ] 3.2 实现Retrofit配置和拦截器
    - 配置Retrofit客户端(超时、日志、JSON解析)
    - 实现OpenAIRetryInterceptor(重试机制,指数退避)
    - 实现API密钥注入
    - _Requirements: 13.1, 13.2, 13.5, 13.6_
  
  - [ ] 3.3 实现AIRepository
    - 实现recognizeItem方法(调用GPT-4V API识别物品)
    - 实现parseRecognitionResult方法(解析JSON响应)
    - 实现错误处理和降级策略
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_
  
  - [ ]* 3.4 编写JSON解析Property Test
    - **Property 9: JSON解析错误鲁棒性**
    - **验证需求: Requirements 12.3**
    - 测试畸形JSON的错误处理
    - _Requirements: 12.3_
  
  - [ ]* 3.5 编写API集成测试
    - 使用MockWebServer模拟OpenAI API响应
    - 测试成功场景和错误场景
    - _Requirements: 1.7, 13.3, 13.7_

- [ ] 4. Checkpoint - 验证数据层和API集成
  - 确保所有测试通过,如有疑问请询问用户

- [ ] 5. 实现位置服务
  - [ ] 5.1 实现LocationService接口和实现类
    - 实现getCurrentLocation方法(使用FusedLocationProviderClient)
    - 实现calculateDistance方法(使用Location.distanceBetween)
    - 实现formatDistance方法(格式化为"XX米"或"XX公里")
    - 处理权限检查和错误场景
    - _Requirements: 15.1, 15.2, 15.3, 15.4, 15.7_
  
  - [ ]* 5.2 编写距离计算Property Tests
    - **Property 10: 距离计算对称性**
    - **验证需求: Requirements 15.3**
    - 测试calculateDistance(A, B) == calculateDistance(B, A)
    - **Property 11: 距离格式化单调性**
    - **验证需求: Requirements 15.7**
    - 测试距离格式化的一致性
    - _Requirements: 15.3, 15.7_
  
  - [ ]* 5.3 编写位置服务单元测试
    - 测试权限未授予场景
    - 测试定位服务未开启场景
    - 测试定位超时场景
    - _Requirements: 15.6_

- [ ] 6. 实现推荐引擎
  - [ ] 6.1 创建RecommendationEngine接口和实现类
    - 实现getRecommendedItems方法
    - 实现calculateRecommendationScore方法(距离、点击、收藏权重)
    - 实现calculateDistanceScore辅助方法
    - 实现updateClickWeight和updateFavoriteWeight方法
    - _Requirements: 3.1, 3.2, 3.3, 3.5, 3.6_
  
  - [ ]* 6.2 编写推荐引擎Property Tests
    - **Property 1: 推荐分数计算公式正确性**
    - **验证需求: Requirements 3.2, 3.3**
    - 测试推荐分数公式计算的正确性
    - **Property 2: 推荐结果排序不变性**
    - **验证需求: Requirements 3.4**
    - 测试排序的幂等性
    - _Requirements: 3.2, 3.3, 3.4_
  
  - [ ]* 6.3 编写推荐引擎单元测试
    - 测试空物品列表场景
    - 测试无用户交互历史场景
    - 测试无位置信息场景
    - 测试最大距离场景
    - _Requirements: 3.8_

- [ ] 7. 实现匹配系统
  - [ ] 7.1 创建MatchingSystem接口和实现类
    - 实现getMatchedItems方法
    - 实现calculateMatchingScore方法(标签和关键词权重)
    - 实现calculateTagSimilarity方法(Jaccard相似度)
    - 实现calculateKeywordSimilarity方法(分词+Jaccard)
    - 实现tokenize辅助方法
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  
  - [ ]* 7.2 编写匹配系统Property Tests
    - **Property 3: 匹配分数计算组合性**
    - **验证需求: Requirements 4.2, 4.3, 4.4**
    - 测试匹配分数公式的正确性
    - **Property 4: 匹配结果排序正确性**
    - **验证需求: Requirements 4.8**
    - 测试匹配结果按分数降序排列
    - **Property 5: 标签相似度对称性**
    - **验证需求: Requirements 4.3**
    - 测试标签相似度的对称性
    - **Property 6: 关键词相似度范围约束**
    - **验证需求: Requirements 4.3**
    - 测试关键词相似度在[0, 1]范围内
    - _Requirements: 4.2, 4.3, 4.4, 4.8_
  
  - [ ]* 7.3 编写匹配系统单元测试
    - 测试空标签场景
    - 测试空描述场景
    - 测试最低匹配阈值过滤
    - _Requirements: 4.6, 4.7_

- [ ] 8. Checkpoint - 验证核心算法逻辑
  - 确保所有测试通过,如有疑问请询问用户

- [ ] 9. 实现Repository层
  - [ ] 9.1 定义Repository接口
    - 定义ItemRepository接口(物品CRUD、搜索、过滤)
    - 定义UserRepository接口(用户管理)
    - 定义ChatRepository接口(聊天消息、会话管理)
    - 定义OrderRepository接口(订单管理)
    - 定义UserInteractionRepository接口(交互记录)
    - _Requirements: 5.1, 7.3, 8.2, 9.2, 10.1_
  
  - [ ] 9.2 实现ItemRepositoryImpl
    - 实现getAllItems、getItemById、getItemsByUserId等方法
    - 实现insertItem、updateItem、deleteItem方法
    - 实现getItemsByTag方法
    - 实现Entity与Model的转换逻辑
    - _Requirements: 5.1, 5.2, 5.3, 14.4, 14.5_
  
  - [ ] 9.3 实现UserRepositoryImpl
    - 实现getUserById、getUserByPhone方法
    - 实现insertUser、updateUser方法
    - _Requirements: 7.2, 11.3, 11.4_
  
  - [ ] 9.4 实现ChatRepositoryImpl
    - 实现getConversation、sendMessage方法
    - 实现markAsRead方法
    - 实现observeConversation方法(返回Flow)
    - 实现generateConversationId辅助方法
    - _Requirements: 9.3, 9.4, 9.5, 9.7_
  
  - [ ] 9.5 实现OrderRepositoryImpl
    - 实现getOrdersByUserId、getOrderById方法
    - 实现insertOrder、updateOrder方法
    - _Requirements: 8.2, 8.4_
  
  - [ ] 9.6 实现UserInteractionRepositoryImpl
    - 实现getUserInteractions、getInteraction方法
    - 实现insertOrUpdateInteraction、incrementClickCount方法
    - _Requirements: 3.5, 3.6, 10.4_
  
  - [ ]* 9.7 编写Repository层单元测试
    - 使用fake DAO实现测试Repository
    - 测试数据转换逻辑
    - 测试错误处理路径
    - _Requirements: 2.8_

- [ ] 10. 实现Use Cases
  - [ ] 10.1 创建AI识别Use Case
    - 实现RecognizeItemImageUseCase
    - 处理图片Base64编码
    - 调用AIRepository并返回Result
    - _Requirements: 1.1, 1.6_
  
  - [ ] 10.2 创建推荐Use Case
    - 实现GetRecommendedItemsUseCase
    - 调用RecommendationEngine并使用ItemRepository
    - _Requirements: 3.1, 3.4_
  
  - [ ] 10.3 创建匹配Use Case
    - 实现GetMatchedItemsUseCase
    - 调用MatchingSystem
    - _Requirements: 4.5, 4.8_
  
  - [ ] 10.4 创建距离计算Use Case
    - 实现CalculateDistanceUseCase
    - 调用LocationService
    - _Requirements: 15.3, 15.4_
  
  - [ ] 10.5 创建物品管理Use Cases
    - 实现SaveItemUseCase(保存或更新物品)
    - 实现DeleteItemUseCase(删除物品)
    - 实现GetItemDetailsUseCase(获取物品详情)
    - _Requirements: 5.3, 6.1, 7.5_
  
  - [ ] 10.6 创建消息Use Cases
    - 实现SendMessageUseCase
    - 实现MarkMessagesAsReadUseCase
    - _Requirements: 9.4, 9.6_
  
  - [ ] 10.7 创建收藏Use Cases
    - 实现ToggleFavoriteUseCase
    - _Requirements: 10.1, 10.2, 10.4_
  
  - [ ]* 10.8 编写Use Case单元测试
    - 使用mock Repository测试业务逻辑
    - 测试边界条件
    - _Requirements: 所有相关需求_

- [ ] 11. 实现表单验证
  - [ ] 11.1 创建ItemFormValidator
    - 实现validate方法验证物品表单数据
    - 验证必填字段(name、description、price、images)
    - 返回ValidationException包含所有缺失字段
    - _Requirements: 6.6, 6.8_
  
  - [ ]* 11.2 编写表单验证Property Test
    - **Property 7: 表单验证完整性**
    - **验证需求: Requirements 6.6**
    - 测试表单验证能正确识别所有缺失字段
    - _Requirements: 6.6_
  
  - [ ]* 11.3 编写表单验证单元测试
    - 测试各种无效输入场景
    - 测试字段边界值
    - _Requirements: 6.8, 14.7_

- [ ] 12. Checkpoint - 验证领域层完整性
  - 确保所有测试通过,如有疑问请询问用户

- [ ] 13. 配置依赖注入(Hilt)
  - [ ] 13.1 创建Application类和Hilt配置
    - 使用@HiltAndroidApp注解Application
    - 创建DatabaseModule提供AppDatabase和DAOs
    - 创建NetworkModule提供Retrofit、OkHttpClient、OpenAIApiService
    - 创建RepositoryModule提供Repository实现
    - _Requirements: 所有模块_
  
  - [ ] 13.2 创建Use Case Module
    - 提供所有Use Case的依赖注入
    - _Requirements: 所有Use Cases_

- [ ] 14. 实现ViewModel层
  - [ ] 14.1 实现LoginViewModel
    - 处理用户登录逻辑
    - 管理登录状态(StateFlow)
    - 调用UserRepository验证凭证
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7_
  
  - [ ] 14.2 实现HomeViewModel
    - 管理物品列表状态
    - 实现分页加载(loadMore方法)
    - 实现下拉刷新(refresh方法)
    - 调用GetRecommendedItemsUseCase
    - 处理收藏操作(调用ToggleFavoriteUseCase)
    - _Requirements: 5.1, 5.2, 5.4, 5.5, 5.6, 10.1, 10.2_
  
  - [ ] 14.3 实现ItemDetailViewModel
    - 获取物品详情(调用GetItemDetailsUseCase)
    - 获取匹配物品(调用GetMatchedItemsUseCase)
    - 处理收藏状态切换
    - 处理联系卖家操作
    - _Requirements: 4.5, 5.3, 10.3_
  
  - [ ] 14.4 实现PostItemViewModel
    - 管理物品发布表单状态
    - 处理图片上传(Base64编码)
    - 调用RecognizeItemImageUseCase识别物品
    - 调用ItemFormValidator验证表单
    - 调用SaveItemUseCase保存物品
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8_
  
  - [ ] 14.5 实现ProfileViewModel
    - 获取用户信息
    - 获取用户发布的物品列表
    - 获取收藏列表
    - 处理编辑和删除物品操作
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7_
  
  - [ ] 14.6 实现OrderListViewModel
    - 获取用户订单列表
    - 处理订单状态更新(确认、取消)
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7_
  
  - [ ] 14.7 实现ChatViewModel
    - 观察聊天消息(使用Flow)
    - 发送文字消息(调用SendMessageUseCase)
    - 发送图片消息
    - 标记消息已读(调用MarkMessagesAsReadUseCase)
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8_
  
  - [ ]* 14.8 编写ViewModel层单元测试
    - 使用mock Use Cases测试ViewModel
    - 使用Turbine测试Flow
    - 测试状态管理逻辑
    - _Requirements: 所有UI相关需求_

- [ ] 15. 实现UI层 - 通用组件
  - [ ] 15.1 创建通用Composable组件
    - ItemCard组件(显示物品卡片)
    - ItemImageCarousel组件(图片轮播)
    - EmptyStateView组件(空状态视图)
    - LoadingView组件(加载动画)
    - ErrorView组件(错误提示)
    - _Requirements: 5.2, 5.7_
  
  - [ ] 15.2 创建Navigation组件
    - 定义导航图(NavHost)
    - 定义路由常量
    - 实现底部导航栏(BottomNavigationBar)
    - _Requirements: 所有界面_

- [ ] 16. 实现UI层 - 主要屏幕
  - [ ] 16.1 实现LoginScreen
    - 实现登录表单UI(手机号、密码输入框)
    - 连接LoginViewModel
    - 处理登录成功和失败状态
    - _Requirements: 11.1, 11.2, 11.5, 11.6_
  
  - [ ] 16.2 实现HomeScreen
    - 实现物品列表UI(LazyColumn)
    - 实现下拉刷新(SwipeRefresh)
    - 实现上滑加载更多(分页)
    - 连接HomeViewModel
    - 显示推荐物品
    - _Requirements: 5.1, 5.2, 5.4, 5.5, 5.6, 5.7_
  
  - [ ] 16.3 实现ItemDetailScreen
    - 实现物品详情UI(图片、名称、描述、价格、距离)
    - 实现收藏按钮
    - 实现联系卖家按钮
    - 显示匹配物品列表
    - 连接ItemDetailViewModel
    - _Requirements: 4.5, 5.3, 10.3, 15.4, 15.5_
  
  - [ ] 16.4 实现PostItemScreen
    - 实现图片上传UI(最多9张)
    - 实现表单输入UI(名称、描述、价格)
    - 实现标签选择UI
    - 显示AI识别结果
    - 连接PostItemViewModel
    - 处理图片上传失败和表单验证错误
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8_
  
  - [ ] 16.5 实现ProfileScreen
    - 实现个人信息显示UI(头像、昵称、联系方式)
    - 实现已发布物品列表
    - 实现收藏列表
    - 连接ProfileViewModel
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6, 7.7, 10.5_
  
  - [ ] 16.6 实现OrderListScreen
    - 实现订单列表UI
    - 显示订单状态(待确认、进行中、已完成、已取消)
    - 实现订单操作按钮(确认、取消)
    - 连接OrderListViewModel
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.7_
  
  - [ ] 16.7 实现ChatScreen
    - 实现聊天消息列表UI(LazyColumn)
    - 实现消息输入框
    - 实现发送按钮
    - 支持文字和图片消息显示
    - 连接ChatViewModel
    - 显示未读消息标记
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6, 9.7, 9.8_
  
  - [ ] 16.8 实现FavoritesScreen
    - 实现收藏列表UI
    - 支持点击跳转到物品详情
    - 连接ProfileViewModel
    - _Requirements: 7.4, 7.6, 10.5_
  
  - [ ]* 16.9 编写UI集成测试
    - 使用Compose测试框架编写UI测试
    - 测试用户交互流程
    - 测试导航跳转
    - _Requirements: 所有UI需求_

- [ ] 17. 实现权限管理和错误处理
  - [ ] 17.1 实现位置权限请求
    - 在HomeScreen和PostItemScreen请求位置权限
    - 处理权限拒绝场景(使用默认位置)
    - 显示权限说明对话框
    - _Requirements: 15.1, 15.6_
  
  - [ ] 17.2 实现相机和存储权限请求
    - 在PostItemScreen请求相机和存储权限
    - 处理权限拒绝场景
    - _Requirements: 6.2_
  
  - [ ] 17.3 实现全局错误处理
    - 实现ErrorLogger工具类(分级日志)
    - 在各ViewModel中处理错误并显示用户友好提示
    - 实现网络错误处理(超时、不可用、重试)
    - 实现API错误处理(降级策略)
    - _Requirements: 1.6, 13.2, 13.3, 13.5, 13.6, 13.7_

- [ ] 18. 实现标签体系
  - [ ] 18.1 创建标签管理模块
    - 定义预定义标签列表(电子产品、书籍、服装、运动器材、生活用品、其他)
    - 实现标签验证逻辑
    - 实现标签筛选UI
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7_
  
  - [ ]* 18.2 编写标签筛选单元测试
    - 测试标签验证逻辑
    - 测试标签筛选功能
    - _Requirements: 14.3, 14.5_

- [ ] 19. Checkpoint - 完整功能验证
  - 确保所有测试通过,如有疑问请询问用户

- [ ] 20. 集成测试和端到端测试
  - [ ]* 20.1 编写数据库集成测试
    - 使用in-memory数据库测试DAO操作
    - 测试数据持久化和读取
    - _Requirements: 2.1, 2.2, 2.3, 2.7_
  
  - [ ]* 20.2 编写API集成测试
    - 使用MockWebServer测试OpenAI API集成
    - 测试成功和失败场景
    - _Requirements: 1.1, 1.6, 1.7_
  
  - [ ]* 20.3 编写关键用户流程端到端测试
    - 测试用户登录流程
    - 测试物品发布流程(含AI识别)
    - 测试物品浏览和搜索流程
    - 测试聊天和交换流程
    - _Requirements: 所有核心流程_

- [ ] 21. 性能优化和最终调整
  - [ ] 21.1 优化图片加载和缓存
    - 配置Coil图片加载库
    - 实现图片压缩和缓存策略
    - _Requirements: 6.2, 6.3_
  
  - [ ] 21.2 优化数据库查询性能
    - 添加必要的数据库索引
    - 优化复杂查询
    - _Requirements: 2.7_
  
  - [ ] 21.3 优化推荐和匹配算法性能
    - 添加缓存机制
    - 异步计算优化
    - _Requirements: 3.7, 4.7_
  
  - [ ] 21.4 代码审查和重构
    - 检查代码风格一致性
    - 移除未使用的代码
    - 优化导入语句
    - _Requirements: 所有_

- [ ] 22. 最终Checkpoint - 确保所有测试通过
  - 运行所有单元测试和property tests
  - 运行所有集成测试
  - 运行UI测试
  - 如有失败测试或疑问,请询问用户

## Notes

- 任务标记为`*`的是可选测试任务,可以跳过以加快MVP开发
- 每个任务都引用了具体的需求编号,确保可追溯性
- Checkpoint任务用于增量验证,确保每个阶段的质量
- Property tests验证设计文档中定义的11个正确性属性
- 单元测试和集成测试验证具体示例和边界情况
- OpenAI API密钥需要用户在`local.properties`文件中配置:
  ```
  OPENAI_API_KEY=sk-your-api-key-here
  OPENAI_API_ENDPOINT=https://api.openai.com
  ```
- 所有与位置、相机、存储相关的功能都需要运行时权限,在相应屏幕实现权限请求
