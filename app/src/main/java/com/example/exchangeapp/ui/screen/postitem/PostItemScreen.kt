package com.example.exchangeapp.ui.screen.postitem

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.exchangeapp.ui.component.CameraStoragePermissionRequester
import com.example.exchangeapp.ui.component.LocationPermissionRequester
import kotlinx.coroutines.launch

/**
 * 发布物品屏幕
 *
 * 提供物品发布界面，支持图片上传(1-9张)、AI智能识别、表单输入(名称/描述/价格)、
 * 标签确认与修改，以及表单验证错误提示。
 *
 * 该界面通过[hiltViewModel]连接[PostItemViewModel]，遵循项目的依赖注入约定。
 * 导航通过回调实现（[onBack]、[onPostSuccess]），与其他界面保持一致。
 * 图片选择(系统相册/权限)由独立任务处理，此处通过[onAddImageClick]回调预留钩子。
 *
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8**
 *
 * Requirements:
 * - 1.1: 上传图片后调用AI识别
 * - 1.2: 显示物品名称
 * - 1.3: 生成物品估价
 * - 1.4: 生成物品简介
 * - 1.5: 自动生成标签
 * - 1.6: AI识别失败时提示手动输入
 * - 1.7: 3秒内完成图像识别(由ViewModel保证，界面反映Loading状态)
 * - 6.1: 提供物品发布界面
 * - 6.2: 允许上传至少1张图片
 * - 6.3: 允许上传最多9张图片
 * - 6.4: 允许输入/编辑名称、价格、描述
 * - 6.5: 显示AI生成的标签供确认/修改
 * - 6.6: 验证所有必填信息已填写
 * - 6.7: 发布成功显示提示并跳转
 * - 6.8: 必填信息未填写时高亮提示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostItemScreen(
    onBack: () -> Unit,
    onPostSuccess: (String) -> Unit,
    onAddImageClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PostItemViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val price by viewModel.price.collectAsStateWithLifecycle()
    val images by viewModel.images.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val recognitionState by viewModel.recognitionState.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val formErrors by viewModel.formErrors.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 请求位置权限以记录发布物品的位置 (Requirement 15.1)；
    // 拒绝时由LocationService回退到默认校区位置 (Requirement 15.6)。
    LocationPermissionRequester()

    // 相机与存储权限请求：用户点击“添加图片”时按需触发 (Requirement 6.2)。
    var requestImagePermission by remember { mutableStateOf(false) }
    CameraStoragePermissionRequester(
        request = requestImagePermission,
        onPermissionResult = { granted ->
            requestImagePermission = false
            if (granted) {
                // 权限通过后再打开图片选择入口
                onAddImageClick()
            } else {
                // 优雅处理权限拒绝场景：提示用户无法添加图片
                scope.launch {
                    snackbarHostState.showSnackbar("未授予相机或存储权限，无法添加图片")
                }
            }
        }
    )

    // 处理保存状态：成功后跳转到物品详情页(Req 6.7)，失败时通过Snackbar提示
    LaunchedEffect(saveState) {
        when (val state = saveState) {
            is SaveState.Success -> {
                snackbarHostState.showSnackbar("发布成功")
                onPostSuccess(state.itemId)
                viewModel.resetSaveState()
            }
            is SaveState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetSaveState()
            }
            else -> Unit
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "发布物品") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        PostItemContent(
            name = name,
            description = description,
            price = price,
            images = images,
            tags = tags,
            recognitionState = recognitionState,
            isSaving = saveState is SaveState.Loading,
            formErrors = formErrors,
            onNameChange = viewModel::updateName,
            onDescriptionChange = viewModel::updateDescription,
            onPriceChange = viewModel::updatePrice,
            onTagsChange = viewModel::updateTags,
            onAddImageClick = { requestImagePermission = true },
            onRemoveImage = viewModel::removeImage,
            onRecognize = viewModel::recognizeItemImage,
            onPost = viewModel::postItem,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

/**
 * 发布物品的内容区域（无状态）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostItemContent(
    name: String,
    description: String,
    price: Double,
    images: List<String>,
    tags: List<String>,
    recognitionState: RecognitionState,
    isSaving: Boolean,
    formErrors: List<String>,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriceChange: (Double) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onAddImageClick: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onRecognize: () -> Unit,
    onPost: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 图片上传区域 (Requirements 6.2, 6.3)
        ImageUploadSection(
            images = images,
            isError = formErrors.contains("images"),
            onAddImageClick = onAddImageClick,
            onRemoveImage = onRemoveImage
        )

        Spacer(modifier = Modifier.height(16.dp))

        // AI识别区域 (Requirements 1.1, 1.6, 1.7)
        AiRecognitionSection(
            hasImages = images.isNotEmpty(),
            recognitionState = recognitionState,
            onRecognize = onRecognize
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 名称输入 (Requirements 6.4, 6.8)
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("物品名称") },
            placeholder = { Text("请输入物品名称") },
            singleLine = true,
            isError = formErrors.contains("name"),
            supportingText = {
                if (formErrors.contains("name")) {
                    Text("请填写物品名称")
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 描述输入 (Requirements 6.4, 6.8)
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("物品描述") },
            placeholder = { Text("请描述物品的成色、规格等信息") },
            minLines = 3,
            maxLines = 6,
            isError = formErrors.contains("description"),
            supportingText = {
                if (formErrors.contains("description")) {
                    Text("请填写物品描述")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 价格输入 (Requirements 6.4, 6.8)
        PriceInputField(
            price = price,
            isError = formErrors.contains("price"),
            onPriceChange = onPriceChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 标签选择区域 (Requirement 6.5)
        TagSelectionSection(
            tags = tags,
            onTagsChange = onTagsChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 发布按钮 (Requirements 6.6, 6.7)
        Button(
            onClick = onPost,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = if (isSaving) "发布中..." else "发布")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * 图片上传区域，展示已上传图片缩略图(支持删除)以及添加入口。
 * 最多9张 (Requirement 6.3)，至少1张 (Requirement 6.2)。
 */
@Composable
private fun ImageUploadSection(
    images: List<String>,
    isError: Boolean,
    onAddImageClick: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxImages = 9
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "物品图片",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${images.size}/$maxImages",
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            images.forEachIndexed { index, base64 ->
                ImageThumbnail(
                    base64 = base64,
                    onRemove = { onRemoveImage(index) }
                )
            }

            // 添加图片入口，仅在未达上限时显示 (Requirement 6.3)
            if (images.size < maxImages) {
                AddImageTile(onClick = onAddImageClick)
            }
        }

        // 必填校验提示 (Requirements 6.2, 6.8)
        if (isError) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "请至少上传1张图片",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * 单张图片缩略图，右上角提供删除按钮。
 */
@Composable
private fun ImageThumbnail(
    base64: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val imageBitmap: ImageBitmap? = remember(base64) { decodeBase64ToImageBitmap(base64) }

    Box(
        modifier = modifier
            .size(96.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "已上传图片",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 删除按钮
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "删除图片",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * 添加图片占位入口。
 */
@Composable
private fun AddImageTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(96.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加图片",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "添加图片",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * AI识别区域：触发识别、显示加载状态、识别成功与失败(回退手动输入)的提示。
 *
 * Requirements:
 * - 1.1: 调用AI识别
 * - 1.6: 识别失败提示手动输入
 * - 1.7: 加载状态反映3秒内识别约束
 */
@Composable
private fun AiRecognitionSection(
    hasImages: Boolean,
    recognitionState: RecognitionState,
    onRecognize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onRecognize,
            enabled = hasImages && recognitionState !is RecognitionState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (recognitionState is RecognitionState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "识别中...")
            } else {
                Text(text = "AI智能识别")
            }
        }

        when (val state = recognitionState) {
            is RecognitionState.Success -> {
                Spacer(modifier = Modifier.height(8.dp))
                RecognitionBanner(
                    message = "AI识别完成，已自动填充名称、描述、估价和标签，您可以继续修改。",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            is RecognitionState.Error -> {
                // 识别失败，提示用户手动输入物品信息 (Requirement 1.6)
                Spacer(modifier = Modifier.height(8.dp))
                RecognitionBanner(
                    message = "AI识别失败：${state.message}。请手动输入物品信息。",
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            else -> Unit
        }
    }
}

/**
 * 识别结果提示横幅。
 */
@Composable
private fun RecognitionBanner(
    message: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
            modifier = Modifier.padding(12.dp)
        )
    }
}

/**
 * 价格输入字段：维护本地文本状态以支持小数输入，并同步AI填充的价格。
 */
@Composable
private fun PriceInputField(
    price: Double,
    isError: Boolean,
    onPriceChange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var priceText by remember { mutableStateOf(if (price > 0) formatPrice(price) else "") }

    // 当ViewModel中的价格变化(如AI填充)且与当前输入不一致时，同步到输入框
    LaunchedEffect(price) {
        val currentParsed = priceText.toDoubleOrNull() ?: 0.0
        if (price > 0 && currentParsed != price) {
            priceText = formatPrice(price)
        }
    }

    OutlinedTextField(
        value = priceText,
        onValueChange = { input ->
            // 只允许数字和小数点
            val filtered = input.filter { it.isDigit() || it == '.' }
            priceText = filtered
            onPriceChange(filtered.toDoubleOrNull() ?: 0.0)
        },
        label = { Text("价格") },
        placeholder = { Text("请输入价格") },
        prefix = { Text("¥") },
        singleLine = true,
        isError = isError,
        supportingText = {
            if (isError) {
                Text("请填写有效的价格")
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * 标签选择区域：展示AI建议/已选标签(可删除)，并允许用户新增标签 (Requirement 6.5)。
 *
 * 说明：当前领域模型未定义固定的预设分类，标签为自由文本，由用户确认或修改。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagSelectionSection(
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var newTag by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "标签",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (tags.isEmpty()) {
            Text(
                text = "暂无标签，可使用AI识别自动生成或手动添加",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tags.forEach { tag ->
                    AssistChip(
                        onClick = { onTagsChange(tags - tag) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "删除标签 $tag",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newTag,
                onValueChange = { newTag = it },
                label = { Text("添加标签") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val trimmed = newTag.trim()
                    if (trimmed.isNotEmpty() && !tags.contains(trimmed)) {
                        onTagsChange(tags + trimmed)
                    }
                    newTag = ""
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加标签"
                )
            }
        }
    }
}

/**
 * 将价格格式化为简洁字符串：整数省略小数位，否则保留原始小数。
 */
private fun formatPrice(price: Double): String {
    return if (price % 1.0 == 0.0) price.toLong().toString() else price.toString()
}

/**
 * 将Base64编码的图片字符串解码为[ImageBitmap]，解码失败时返回null。
 */
private fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
    return try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
