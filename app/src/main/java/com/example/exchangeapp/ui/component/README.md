# 通用Composable组件

根据任务15.1要求实现的通用Composable组件，满足Requirements 5.2和5.7。

## 已实现的组件

### 1. ItemCard.kt - 物品卡片组件
**功能**:
- 显示物品图片、名称、价格和简介 (满足Requirement 5.2)
- 显示收藏状态和收藏按钮
- 显示标签和距离信息
- 支持点击事件

**用法**:
```kotlin
ItemCard(
    item = item,
    isFavorite = isFavorite,
    distance = "500米",
    onItemClick = { /* 点击物品处理 */ },
    onFavoriteClick = { /* 收藏按钮点击处理 */ }
)
```

### 2. ItemImageCarousel.kt - 图片轮播组件
**功能**:
- 支持多张图片轮播显示
- 自动轮播功能
- 图片索引指示器
- 左右切换按钮
- 点击图片回调

**用法**:
```kotlin
ItemImageCarousel(
    images = item.images,
    autoPlay = true,
    onImageClick = { /* 点击图片处理 */ }
)
```

### 3. EmptyStateView.kt - 空状态视图组件
**功能**:
- 显示空状态图标、标题和描述
- 支持操作按钮
- 特定场景的空状态视图:
  - `EmptyItemsState()` - 物品列表为空
  - `EmptySearchState()` - 搜索结果为空  
  - `EmptyFavoritesState()` - 收藏列表为空
  - `NoMoreItemsState()` - 无更多物品加载 (满足Requirement 5.7)
  - `NetworkErrorState()` - 网络错误
  - `NoNetworkState()` - 无网络连接

**用法**:
```kotlin
EmptyStateView(
    iconResId = R.drawable.ic_menu_view,
    title = "暂无物品",
    description = "还没有物品发布，去发布第一个物品吧！",
    actionText = "浏览物品",
    onActionClick = { /* 操作按钮点击处理 */ }
)

// 或者使用特定场景组件
NoMoreItemsState() // 显示"没有更多物品"提示
```

### 4. LoadingView.kt - 加载动画组件
**功能**:
- 全屏加载动画
- 内联加载动画
- 进度条加载动画
- 骨架屏加载动画
- 特定场景的加载组件:
  - `LoadMoreLoadingView()` - 列表加载更多
  - `RefreshLoadingView()` - 下拉刷新
  - `ImageUploadLoadingView()` - 图片上传

**用法**:
```kotlin
LoadingView(message = "正在加载...")

// 或者使用特定场景组件
LoadMoreLoadingView() // 上滑加载更多
```

### 5. ErrorView.kt - 错误提示组件
**功能**:
- 显示错误图标和错误信息
- 重试和次要操作按钮
- 特定场景的错误组件:
  - `NetworkErrorView()` - 网络错误
  - `DataLoadErrorView()` - 数据加载错误
  - `PermissionErrorView()` - 权限错误
  - `LocationServiceErrorView()` - 位置服务错误
  - `ServerErrorView()` - 服务器错误
  - `NoNetworkView()` - 无网络连接
  - `OperationErrorView()` - 操作失败
  - `InlineErrorView()` - 内联错误提示
  - `SimpleErrorView()` - 简化错误提示

**用法**:
```kotlin
ErrorView(
    title = "网络连接失败",
    message = "无法连接到服务器，请检查您的网络连接后重试。",
    retryText = "重试",
    onRetryClick = { /* 重试处理 */ }
)

// 或者使用特定场景组件
NetworkErrorView(
    onRetryClick = { /* 重试处理 */ },
    onCheckNetwork = { /* 检查网络处理 */ }
)
```

## 主题和样式

组件使用Material3主题，并支持自定义颜色:
- `CustomColors.kt` - 自定义颜色主题
- 通过`LocalCustomColors` CompositionLocal提供
- 支持亮色和暗色模式

## 组件预览

`ComponentPreview.kt`文件包含所有组件的预览函数，可在Android Studio中预览。

## 注意事项

1. **图标资源**: 当前使用`android.R.drawable`系统图标，在实际项目中应替换为Material Icons或自定义矢量图标
2. **图片加载**: 使用Coil库进行异步图片加载
3. **响应式设计**: 所有组件支持Modifier参数进行自定义样式
4. **可访问性**: 组件包含适当的contentDescription
5. **性能优化**: 使用Compose最佳实践，避免不必要的重组

## 与现有代码集成

组件已设计为与现有ViewModels配合使用:
- `HomeViewModel` - 使用ItemCard显示物品列表
- 支持`ItemsState`、`LoadMoreState`、`RefreshState`等状态
- 与`ToggleFavoriteUseCase`集成处理收藏功能

## 测试建议

每个组件都应编写:
1. 单元测试验证业务逻辑
2. UI测试验证渲染和交互
3. 快照测试验证UI一致性