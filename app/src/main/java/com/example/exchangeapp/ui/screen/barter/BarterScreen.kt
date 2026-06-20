package com.example.exchangeapp.ui.screen.barter

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.MatchedItem
import com.example.exchangeapp.ui.component.CameraStoragePermissionRequester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarterScreen(
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BarterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var requestImagePermission by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                if (bytes != null) {
                    viewModel.addImageFromBytes(bytes)
                } else {
                    snackbarHostState.showSnackbar("图片读取失败")
                }
            }
        }
    }

    CameraStoragePermissionRequester(
        request = requestImagePermission,
        onPermissionResult = { granted ->
            requestImagePermission = false
            if (granted) {
                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                scope.launch { snackbarHostState.showSnackbar("未授予图片权限") }
            }
        }
    )

    LaunchedEffect(uiState.message) {
        uiState.message?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("以物易物") })
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                BarterPostForm(
                    uiState = uiState,
                    onAddImage = { requestImagePermission = true },
                    onRemoveImage = viewModel::removeImage,
                    onNameChange = viewModel::updateName,
                    onDescriptionChange = viewModel::updateDescription,
                    onPriceChange = viewModel::updatePrice,
                    onTagsChange = viewModel::updateTags,
                    onWantedNameChange = viewModel::updateWantedItemName,
                    onWantedTagsChange = viewModel::updateWantedTags,
                    onParseWantedTags = viewModel::parseWantedTags,
                    onPost = viewModel::postBarterItem
                )
            }

            item {
                SectionTitle("我的易物商品")
                if (uiState.myItems.isEmpty()) {
                    EmptyLine("发布一件带有想要标签的商品后，会在这里显示。")
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.myItems.forEach { item ->
                            FilterChip(
                                selected = uiState.selectedItemId == item.id,
                                onClick = { viewModel.selectItem(item.id) },
                                label = { Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle("智能匹配")
                if (uiState.isLoadingMatches) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (uiState.matches.isEmpty()) {
                    EmptyLine("暂无合适匹配。补充更具体的商品标签和想要标签后，匹配会更准。")
                }
            }

            items(uiState.matches, key = { it.item.id }) { match ->
                MatchRow(match = match, onClick = { onItemClick(match.item.id) })
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun BarterPostForm(
    uiState: BarterUiState,
    onAddImage: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onWantedNameChange: (String) -> Unit,
    onWantedTagsChange: (List<String>) -> Unit,
    onParseWantedTags: () -> Unit,
    onPost: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("发布易物商品")

        ImageStrip(
            images = uiState.images,
            onAddImage = onAddImage,
            onRemoveImage = onRemoveImage
        )

        if (uiState.isRecognizingItem) {
            InlineLoading("正在识别商品信息")
        }

        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = { Text("商品名称") },
            isError = uiState.formErrors.contains("name"),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.description,
            onValueChange = onDescriptionChange,
            label = { Text("商品描述") },
            isError = uiState.formErrors.contains("description"),
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.price,
            onValueChange = onPriceChange,
            label = { Text("估价") },
            prefix = { Text("￥") },
            isError = uiState.formErrors.contains("price"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        EditableTags(
            title = "商品标签",
            tags = uiState.tags,
            onTagsChange = onTagsChange
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = uiState.wantedItemName,
                onValueChange = onWantedNameChange,
                label = { Text("想要的物品") },
                isError = uiState.formErrors.contains("wanted"),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = onParseWantedTags,
                enabled = !uiState.isParsingWantedTags
            ) {
                if (uiState.isParsingWantedTags) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("AI标签")
                }
            }
        }

        EditableTags(
            title = "想要的商品标签",
            tags = uiState.wantedTags,
            onTagsChange = onWantedTagsChange
        )

        Button(
            onClick = onPost,
            enabled = !uiState.isPosting,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.isPosting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (uiState.isPosting) "发布中..." else "发布易物商品")
        }
    }
}

@Composable
private fun ImageStrip(
    images: List<String>,
    onAddImage: () -> Unit,
    onRemoveImage: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        images.forEachIndexed { index, base64 ->
            val bitmap = remember(base64) { decodeBase64ToImageBitmap(base64) }
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                IconButton(
                    onClick = { onRemoveImage(index) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "删除图片", modifier = Modifier.size(14.dp))
                }
            }
        }
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(onClick = onAddImage),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "添加图片")
        }
    }
}

@Composable
private fun EditableTags(
    title: String,
    tags: List<String>,
    onTagsChange: (List<String>) -> Unit
) {
    var input by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                AssistChip(
                    onClick = { onTagsChange(tags - tag) },
                    label = { Text(tag) },
                    trailingIcon = {
                        Icon(Icons.Default.Close, contentDescription = "删除$tag", modifier = Modifier.size(14.dp))
                    }
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("添加标签") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    val tag = input.trim().lowercase()
                    if (tag.isNotBlank()) {
                        onTagsChange((tags + tag).distinct())
                    }
                    input = ""
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加标签")
            }
        }
    }
}

@Composable
private fun MatchRow(match: MatchedItem, onClick: () -> Unit) {
    val item = match.item
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("${(match.matchingScore * 100).toInt()}%", color = MaterialTheme.colorScheme.primary)
            }
            Text(item.description, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (item.wantedItemName.isNotBlank()) {
                Text("TA想要：${item.wantedItemName}", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item.tags.take(4).forEach { tag ->
                    Text("#$tag", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun InlineLoading(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}

private fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
    return try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
