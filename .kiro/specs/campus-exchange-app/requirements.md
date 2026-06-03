# Requirements Document

## Introduction

校园二手物品交换应用是一个专为校园用户设计的Android应用程序，提供便捷的二手物品交换平台。该应用通过AI图像识别、智能推荐和以物易物匹配系统，帮助用户快速发布、浏览和交换闲置物品。应用采用Kotlin语言开发，使用Jetpack Compose UI框架构建用户界面。

## Glossary

- **App**: 校园二手物品交换Android应用
- **User**: 使用该应用的校园用户
- **Item**: 用户发布的二手物品
- **Image_Recognition_API**: 外部AI图像识别服务接口
- **Item_Tag**: 用于分类和匹配的物品标签
- **Storage_Module**: Room数据库或SharedPreferences存储模块
- **Recommendation_Engine**: 基于规则的智能推荐引擎
- **Matching_System**: 以物易物匹配系统
- **Item_Post**: 用户发布的物品信息
- **Chat_Record**: 用户之间的聊天记录
- **Favorite_List**: 用户收藏的物品列表
- **Login_State**: 用户的登录状态信息
- **Recommendation_Score**: 推荐评分，计算公式为：距离权重 + 点击权重 + 收藏权重
- **Matching_Score**: 匹配评分，基于标签相似度和关键词匹配度计算
- **Item_Description**: 物品的文字描述信息
- **Item_Price_Estimate**: AI识别生成的物品估价
- **Similarity_Score**: 关键词相似度评分

## Requirements

### Requirement 1: AI图像识别与物品信息生成

**User Story:** 作为用户，我希望上传物品图片后自动获取物品信息，这样我可以快速发布物品而不需要手动输入详细信息。

#### Acceptance Criteria

1. WHEN User上传Item图片, THE App SHALL调用Image_Recognition_API进行识别
2. WHEN Image_Recognition_API返回识别结果, THE App SHALL显示物品名称
3. WHEN Image_Recognition_API返回识别结果, THE App SHALL生成Item_Price_Estimate
4. WHEN Image_Recognition_API返回识别结果, THE App SHALL生成Item_Description作为物品简介
5. WHEN Image_Recognition_API返回识别结果, THE App SHALL自动为Item生成Item_Tag用于分类
6. IF Image_Recognition_API调用失败, THEN THE App SHALL提示User手动输入物品信息
7. THE App SHALL在3秒内完成图像识别API调用并显示结果

### Requirement 2: 本地数据持久化存储

**User Story:** 作为用户，我希望应用能记住我的登录状态和使用数据，这样我不需要每次打开应用都重新登录或重新设置。

#### Acceptance Criteria

1. THE Storage_Module SHALL保存Login_State到本地存储
2. THE Storage_Module SHALL保存Favorite_List到本地存储
3. THE Storage_Module SHALL保存Chat_Record到本地存储
4. WHEN User启动App, THE Storage_Module SHALL加载Login_State
5. WHEN User添加或删除收藏, THE Storage_Module SHALL更新Favorite_List
6. WHEN User发送或接收消息, THE Storage_Module SHALL保存Chat_Record
7. THE Storage_Module SHALL在500毫秒内完成数据读取操作
8. IF 数据读取失败, THEN THE Storage_Module SHALL返回错误代码并使用默认值

### Requirement 3: 智能推荐系统

**User Story:** 作为用户，我希望看到与我兴趣相关的物品推荐，这样我可以更快找到我需要的二手物品。

#### Acceptance Criteria

1. THE Recommendation_Engine SHALL实现"猜你喜欢"推荐功能
2. THE Recommendation_Engine SHALL基于规则算法计算Recommendation_Score
3. THE Recommendation_Engine SHALL使用公式：Recommendation_Score = 距离权重 + 点击权重 + 收藏权重
4. WHEN User浏览物品列表, THE Recommendation_Engine SHALL根据Recommendation_Score排序显示推荐物品
5. WHEN User点击Item, THE Recommendation_Engine SHALL增加该物品的点击权重
6. WHEN User收藏Item, THE Recommendation_Engine SHALL增加该物品的收藏权重
7. THE Recommendation_Engine SHALL每隔24小时重新计算所有Item的Recommendation_Score
8. THE Recommendation_Engine SHALL返回至少10个推荐物品

### Requirement 4: 以物易物智能匹配系统

**User Story:** 作为用户，我希望系统能智能匹配可能与我的物品交换的其他用户物品，这样我可以更容易找到合适的交换对象。

#### Acceptance Criteria

1. THE Matching_System SHALL为每个Item附加Item_Tag
2. THE Matching_System SHALL基于Item_Tag进行分类匹配
3. THE Matching_System SHALL计算Item之间的关键词Similarity_Score
4. THE Matching_System SHALL基于Item_Tag和Similarity_Score生成Matching_Score
5. WHEN User查看Item详情, THE Matching_System SHALL显示匹配度最高的其他Item
6. THE Matching_System SHALL返回至少5个匹配物品
7. THE Matching_System SHALL在2秒内完成匹配计算
8. THE Matching_System SHALL按Matching_Score降序排列匹配结果

### Requirement 5: 物品浏览功能

**User Story:** 作为用户，我希望浏览所有发布的二手物品，这样我可以找到我感兴趣的物品。

#### Acceptance Criteria

1. THE App SHALL显示物品浏览界面
2. THE App SHALL显示Item的图片、名称、价格和简介
3. WHEN User点击Item, THE App SHALL显示Item的详细信息页面
4. THE App SHALL支持下拉刷新物品列表
5. THE App SHALL支持上滑加载更多物品
6. THE App SHALL每页加载20个Item
7. IF 无更多Item可加载, THEN THE App SHALL显示"没有更多物品"提示

### Requirement 6: 物品发布功能

**User Story:** 作为用户，我希望发布我的闲置物品，这样其他用户可以看到并与我交换。

#### Acceptance Criteria

1. THE App SHALL提供物品发布界面
2. THE App SHALL允许User上传至少1张Item图片
3. THE App SHALL允许User上传最多9张Item图片
4. THE App SHALL允许User输入或编辑Item的名称、价格和描述
5. THE App SHALL显示AI生成的Item_Tag供User确认或修改
6. WHEN User点击发布按钮, THE App SHALL验证所有必填信息已填写
7. WHEN User发布Item成功, THE App SHALL显示成功提示并跳转到物品详情页
8. IF 必填信息未填写, THEN THE App SHALL高亮显示缺失字段并提示User补充

### Requirement 7: 个人中心功能

**User Story:** 作为用户，我希望管理我的个人信息和发布的物品，这样我可以跟踪我的交换活动。

#### Acceptance Criteria

1. THE App SHALL显示个人中心界面
2. THE App SHALL显示User的头像、昵称和联系方式
3. THE App SHALL显示User发布的所有Item列表
4. THE App SHALL显示User的Favorite_List
5. WHEN User点击已发布Item, THE App SHALL允许User编辑或删除该Item
6. WHEN User点击收藏Item, THE App SHALL跳转到该Item详情页
7. THE App SHALL显示User的历史交换记录数量

### Requirement 8: 订单管理功能

**User Story:** 作为用户，我希望管理我的交换订单，这样我可以跟踪交换进度和历史记录。

#### Acceptance Criteria

1. THE App SHALL显示订单管理界面
2. THE App SHALL显示User的所有交换订单
3. THE App SHALL显示每个订单的状态（待确认、进行中、已完成、已取消）
4. WHEN User点击订单, THE App SHALL显示订单详情
5. WHILE 订单状态为待确认, THE App SHALL允许User确认或取消订单
6. WHILE 订单状态为进行中, THE App SHALL显示对方User的联系方式
7. WHEN 订单完成, THE App SHALL允许User对交换进行评价

### Requirement 9: 聊天系统功能

**User Story:** 作为用户，我希望与其他用户即时沟通，这样我可以协商交换细节。

#### Acceptance Criteria

1. THE App SHALL提供聊天界面
2. WHEN User点击联系卖家, THE App SHALL打开与该User的聊天窗口
3. THE App SHALL显示Chat_Record的历史消息
4. THE App SHALL允许User发送文字消息
5. THE App SHALL允许User发送图片消息
6. WHEN User发送消息, THE App SHALL在1秒内显示消息已发送状态
7. THE App SHALL按时间顺序显示Chat_Record
8. THE App SHALL显示未读消息数量标记

### Requirement 10: 收藏功能

**User Story:** 作为用户，我希望收藏感兴趣的物品，这样我可以稍后查看并考虑交换。

#### Acceptance Criteria

1. WHEN User点击收藏按钮, THE App SHALL将Item添加到Favorite_List
2. WHEN Item已在Favorite_List中且User点击收藏按钮, THE App SHALL从Favorite_List中移除该Item
3. THE App SHALL在收藏按钮上显示当前收藏状态
4. WHEN User添加或移除收藏, THE Storage_Module SHALL立即保存Favorite_List
5. THE App SHALL在个人中心显示完整的Favorite_List

### Requirement 11: 用户登录与认证

**User Story:** 作为用户，我希望安全地登录应用，这样我的个人信息和交换记录得到保护。

#### Acceptance Criteria

1. THE App SHALL提供登录界面
2. THE App SHALL支持手机号码和密码登录
3. WHEN User输入登录凭证并点击登录, THE App SHALL验证凭证
4. WHEN 登录成功, THE App SHALL保存Login_State到Storage_Module
5. WHEN 登录失败, THE App SHALL显示错误提示信息
6. THE App SHALL在3秒内完成登录验证
7. WHILE Login_State有效, THE App SHALL自动登录User
8. IF Login_State过期, THEN THE App SHALL要求User重新登录

### Requirement 12: 物品图片解析与Pretty Printer

**User Story:** 作为开发者，我希望正确解析和格式化物品数据，这样应用可以可靠地存储和显示物品信息。

#### Acceptance Criteria

1. THE App SHALL解析Image_Recognition_API返回的JSON格式数据
2. WHEN 收到有效JSON数据, THE App SHALL将数据转换为Item对象
3. WHEN 收到无效JSON数据, THE App SHALL返回描述性错误信息
4. THE App SHALL实现Item对象的格式化输出功能
5. FOR ALL 有效Item对象, 解析后格式化再解析 SHALL产生等价的Item对象（round-trip property）
6. THE App SHALL在100毫秒内完成JSON解析
7. THE App SHALL在50毫秒内完成Item对象格式化

### Requirement 13: 网络请求与错误处理

**User Story:** 作为用户，我希望应用能正确处理网络问题，这样我在网络不稳定时也能获得清晰的反馈。

#### Acceptance Criteria

1. WHEN App发起网络请求, THE App SHALL设置10秒超时时间
2. IF 网络请求超时, THEN THE App SHALL显示"网络超时，请重试"提示
3. IF 网络不可用, THEN THE App SHALL显示"网络不可用，请检查网络连接"提示
4. WHEN 网络请求失败, THE App SHALL记录错误日志
5. THE App SHALL在网络恢复后自动重试失败的请求
6. THE App SHALL最多重试3次网络请求
7. IF 重试3次后仍失败, THEN THE App SHALL停止重试并提示User

### Requirement 14: 标签体系与分类

**User Story:** 作为用户，我希望物品按类别组织，这样我可以快速找到特定类型的物品。

#### Acceptance Criteria

1. THE App SHALL维护预定义的Item_Tag分类体系
2. THE App SHALL支持以下分类：电子产品、书籍、服装、运动器材、生活用品、其他
3. WHEN Item被分配Item_Tag, THE App SHALL验证Item_Tag属于预定义分类
4. THE App SHALL允许User按Item_Tag筛选Item
5. WHEN User选择Item_Tag筛选, THE App SHALL仅显示包含该Item_Tag的Item
6. THE App SHALL在物品列表显示Item的主要Item_Tag
7. THE App SHALL允许一个Item拥有最多5个Item_Tag

### Requirement 15: 距离计算与位置服务

**User Story:** 作为用户，我希望优先看到距离我较近的物品，这样我可以更方便地进行线下交换。

#### Acceptance Criteria

1. WHEN User启动App, THE App SHALL请求User授权位置权限
2. WHEN User授权位置权限, THE App SHALL获取User的当前位置
3. THE App SHALL计算User位置与Item发布位置之间的距离
4. THE App SHALL在物品列表显示距离信息
5. THE Recommendation_Engine SHALL使用距离作为Recommendation_Score的权重之一
6. WHILE User未授权位置权限, THE App SHALL使用默认校区作为位置
7. THE App SHALL将距离格式化为"XX米"或"XX公里"显示

