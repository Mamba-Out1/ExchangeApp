package com.example.exchangeapp.ui.screen.postitem

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.exchangeapp.domain.exception.ValidationException
import com.example.exchangeapp.domain.model.Item
import com.example.exchangeapp.domain.model.ItemFormData
import com.example.exchangeapp.domain.model.ItemRecognitionResult
import com.example.exchangeapp.domain.model.ItemStatus
import com.example.exchangeapp.domain.model.Location
import com.example.exchangeapp.domain.service.CurrentUserProvider
import com.example.exchangeapp.domain.service.LocationService
import com.example.exchangeapp.domain.usecase.RecognizeItemImageUseCase
import com.example.exchangeapp.domain.usecase.SaveItemUseCase
import com.example.exchangeapp.domain.validation.ItemFormValidator
import com.example.exchangeapp.util.ImageCompressor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * 物品发布界面的ViewModel
 *
 * 管理物品发布表单状态，处理图片上传(Base64编码)，调用RecognizeItemImageUseCase识别物品，
 * 调用ItemFormValidator验证表单，调用SaveItemUseCase保存物品
 *
 * **Validates: Requirements 1.1, 1.2, 1.3, 1.4, 1.5, 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 6.7, 6.8**
 *
 * Requirements:
 * - 1.1: WHEN User上传Item图片, THE App SHALL调用Image_Recognition_API进行识别
 * - 1.2: WHEN Image_Recognition_API返回识别结果, THE App SHALL显示物品名称
 * - 1.3: WHEN Image_Recognition_API返回识别结果, THE App SHALL生成Item_Price_Estimate
 * - 1.4: WHEN Image_Recognition_API返回识别结果, THE App SHALL生成Item_Description作为物品简介
 * - 1.5: WHEN Image_Recognition_API返回识别结果, THE App SHALL自动为Item生成Item_Tag用于分类
 * - 1.6: IF Image_Recognition_API调用失败, THEN THE App SHALL提示User手动输入物品信息
 * - 1.7: THE App SHALL在3秒内完成图像识别API调用并显示结果
 * - 6.1: THE App SHALL提供物品发布界面
 * - 6.2: THE App SHALL允许User上传至少1张Item图片
 * - 6.3: THE App SHALL允许User上传最多9张Item图片
 * - 6.4: THE App SHALL允许User输入或编辑Item的名称、价格和描述
 * - 6.5: THE App SHALL显示AI生成的Item_Tag供User确认或修改
 * - 6.6: WHEN User点击发布按钮, THE App SHALL验证所有必填信息已填写
 * - 6.7: WHEN User发布Item成功, THE App SHALL显示成功提示并跳转到物品详情页
 * - 6.8: IF 必填信息未填写, THEN THE App SHALL高亮显示缺失字段并提示User补充
 */
@HiltViewModel
class PostItemViewModel @Inject constructor(
    private val recognizeItemImageUseCase: RecognizeItemImageUseCase,
    private val saveItemUseCase: SaveItemUseCase,
    private val itemFormValidator: ItemFormValidator,
    private val currentUserProvider: CurrentUserProvider,
    private val locationService: LocationService
) : ViewModel() {

    companion object {
        private const val MAX_IMAGES = 9 // 最多9张图片 (Requirement 6.3)
        private const val MIN_IMAGES = 1 // 至少1张图片 (Requirement 6.2)
        private const val RECOGNITION_TIMEOUT_MS = 30000L
    }

    // 表单数据状态
    private val _name = MutableStateFlow("")
    private val _description = MutableStateFlow("")
    private val _price = MutableStateFlow(0.0)
    private val _images = MutableStateFlow<List<String>>(emptyList()) // Base64编码的图片字符串
    private val _tags = MutableStateFlow<List<String>>(emptyList())

    val name: StateFlow<String> = _name.asStateFlow()
    val description: StateFlow<String> = _description.asStateFlow()
    val price: StateFlow<Double> = _price.asStateFlow()
    val images: StateFlow<List<String>> = _images.asStateFlow()
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    // AI识别状态
    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val recognitionState: StateFlow<RecognitionState> = _recognitionState.asStateFlow()

    // 保存状态
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    // 表单错误状态
    private val _formErrors = MutableStateFlow<List<String>>(emptyList())
    val formErrors: StateFlow<List<String>> = _formErrors.asStateFlow()

    // 是否已使用AI识别填充表单
    private var isAiFilled = false

    private var recognitionJob: Job? = null

    /**
     * 更新物品名称
     *
     * @param newName 新物品名称
     */
    fun updateName(newName: String) {
        _name.value = newName
        clearFormErrors("name")
    }

    /**
     * 更新物品描述
     *
     * @param newDescription 新物品描述
     */
    fun updateDescription(newDescription: String) {
        _description.value = newDescription
        clearFormErrors("description")
    }

    /**
     * 更新物品价格
     *
     * @param newPrice 新物品价格
     */
    fun updatePrice(newPrice: Double) {
        _price.value = newPrice
        clearFormErrors("price")
    }

    /**
     * 更新物品标签
     *
     * @param newTags 新物品标签列表
     */
    fun updateTags(newTags: List<String>) {
        _tags.value = newTags
    }

    /**
     * 添加图片
     *
     * @param imageBase64 Base64编码的图片字符串
     */
    fun addImage(imageBase64: String) {
        val currentImages = _images.value.toMutableList()
        if (currentImages.size < MAX_IMAGES) {
            currentImages.add(imageBase64)
            _images.value = currentImages
            clearFormErrors("images")
        }
    }

    /**
     * 删除指定索引的图片
     *
     * @param index 图片索引
     */
    fun removeImage(index: Int) {
        val currentImages = _images.value.toMutableList()
        if (index in currentImages.indices) {
            currentImages.removeAt(index)
            _images.value = currentImages
            if (currentImages.isEmpty()) {
                _formErrors.value = _formErrors.value + "images"
            }
        }
    }

    /**
     * 使用图片字节添加图片（调用方提供原始图片字节）
     *
     * @param imageBytes 图片字节数组
     */
    fun addImageFromBytes(imageBytes: ByteArray) {
        if (imageBytes.isEmpty()) return

        val wasEmpty = _images.value.isEmpty()

        // 上传前压缩图片（降采样 + JPEG质量压缩），减小存储与传输体积 (Requirements 6.2, 6.3)
        val compressedBytes = ImageCompressor.compress(imageBytes)

        // 将压缩后的图片字节进行Base64编码（NO_WRAP避免插入换行符）
        val imageBase64 = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
        addImage(imageBase64)

        if (wasEmpty && _images.value.isNotEmpty()) {
            recognizeItemImage()
        }
    }

    /**
     * 识别物品图片（使用第一张图片进行识别）
     *
     * 实现Requirement 1.1: 调用Image_Recognition_API进行识别
     */
    fun recognizeItemImage() {
        val imagesList = _images.value
        if (imagesList.isEmpty()) {
            _recognitionState.value = RecognitionState.Error("请先上传图片")
            return
        }

        // 使用第一张图片进行识别
        val firstImageBase64 = imagesList.first()
        val imageBytes = decodeBase64Image(firstImageBase64)
        
        recognitionJob?.cancel()
        recognitionJob = viewModelScope.launch {
            try {
                _recognitionState.value = RecognitionState.Loading
                kotlinx.coroutines.yield()
                
                // 使用withTimeout确保3秒内完成识别 (Requirement 1.7)
                val result = kotlinx.coroutines.withTimeout(RECOGNITION_TIMEOUT_MS) {
                    recognizeItemImageUseCase(imageBytes)
                }

                when {
                    result.isSuccess -> {
                        val recognitionResult = result.getOrThrow()
                        // 使用AI识别结果填充表单
                        applyRecognitionResult(recognitionResult)
                        isAiFilled = true
                        _recognitionState.value = RecognitionState.Success
                    }
                    result.isFailure -> {
                        // 识别失败，显示错误但允许用户手动输入 (Requirement 1.6)
                        val errorMessage = result.exceptionOrNull()?.message ?: "识别失败"
                        _recognitionState.value = RecognitionState.Error(errorMessage)
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // 超时处理
                _recognitionState.value = RecognitionState.Error("识别超时，请稍后重试")
            } catch (e: kotlinx.coroutines.CancellationException) {
                if (_recognitionState.value is RecognitionState.Loading) {
                    _recognitionState.value = RecognitionState.Idle
                }
            } catch (e: Exception) {
                _recognitionState.value = RecognitionState.Error("识别失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    /**
     * 应用AI识别结果到表单
     *
     * 实现Requirements 1.2, 1.3, 1.4, 1.5: 显示识别结果
     */
    private fun applyRecognitionResult(result: ItemRecognitionResult) {
        // 仅当用户未手动修改过或字段为空时才自动填充
        if (_name.value.isBlank()) {
            _name.value = result.name
        }
        
        if (_description.value.isBlank()) {
            _description.value = result.description
        }
        
        if (_price.value <= 0) {
            _price.value = result.estimatedPrice
        }
        
        if (_tags.value.isEmpty()) {
            _tags.value = result.tags
        }
    }

    /**
     * 发布物品
     *
     * 实现Requirement 6.6: 验证所有必填信息已填写
     * 实现Requirement 6.7: 发布成功并跳转到物品详情页
     * 实现Requirement 6.8: 高亮显示缺失字段并提示补充
     */
    fun postItem() {
        val userId = currentUserProvider.getCurrentUserId()
        if (userId == null) {
            _saveState.value = SaveState.Error("请先登录")
            return
        }

        // 创建表单数据
        val formData = ItemFormData(
            name = _name.value,
            description = _description.value,
            price = _price.value,
            images = _images.value,
            tags = _tags.value
        )

        // 验证表单
        val validationResult = itemFormValidator.validate(formData)
        if (validationResult.isFailure) {
            val validationException = validationResult.exceptionOrNull() as? ValidationException
            val missingFields = validationException?.missingFields ?: emptyList()
            _formErrors.value = missingFields
            _saveState.value = SaveState.Error("请填写所有必填字段")
            return
        }

        _saveState.value = SaveState.Loading
        _formErrors.value = emptyList()

        viewModelScope.launch {
            try {
                // 获取用户当前位置
                val userLocation = locationService.getCurrentLocation()
                
                // 创建Item对象
                val item = Item(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    name = formData.name,
                    description = formData.description,
                    estimatedPrice = formData.price,
                    images = formData.images,
                    tags = formData.tags,
                    location = userLocation,
                    status = ItemStatus.AVAILABLE,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                // 保存物品
                val saveResult = saveItemUseCase(item)
                
                when {
                    saveResult.isSuccess -> {
                        val savedItem = saveResult.getOrThrow()
                        _saveState.value = SaveState.Success(savedItem.id)
                        // 重置表单状态（可选）
                        resetForm()
                    }
                    saveResult.isFailure -> {
                        val errorMessage = saveResult.exceptionOrNull()?.message ?: "发布失败"
                        _saveState.value = SaveState.Error(errorMessage)
                    }
                }
            } catch (e: Exception) {
                _saveState.value = SaveState.Error("发布失败: ${e.message ?: "未知错误"}")
            }
        }
    }

    /**
     * 重置表单
     */
    fun resetForm() {
        _name.value = ""
        _description.value = ""
        _price.value = 0.0
        _images.value = emptyList()
        _tags.value = emptyList()
        isAiFilled = false
        _formErrors.value = emptyList()
    }

    /**
     * 清除指定字段的错误状态
     *
     * @param field 字段名称
     */
    private fun clearFormErrors(field: String) {
        _formErrors.value = _formErrors.value.filter { it != field }
    }

    /**
     * 重置识别状态
     */
    fun resetRecognitionState() {
        _recognitionState.value = RecognitionState.Idle
    }

    /**
     * 重置保存状态
     */
    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }

    /**
     * 取消正在进行的识别任务
     */
    fun cancelRecognition() {
        recognitionJob?.cancel()
        _recognitionState.value = RecognitionState.Idle
    }

    /**
     * 检查是否有足够的图片
     *
     * @return true如果至少有1张图片，false否则
     */
    fun hasEnoughImages(): Boolean {
        return _images.value.size >= MIN_IMAGES
    }

    /**
     * 检查是否已达到图片上限
     *
     * @return true如果已达到9张图片，false否则
     */
    fun hasReachedImageLimit(): Boolean {
        return _images.value.size >= MAX_IMAGES
    }

    /**
     * 检查表单是否已由AI填充
     *
     * @return true如果表单已由AI填充，false否则
     */
    fun isAiFilled(): Boolean {
        return isAiFilled
    }

    private fun decodeBase64Image(imageBase64: String): ByteArray {
        return try {
            Base64.decode(imageBase64, Base64.DEFAULT)
        } catch (e: Exception) {
            try {
                java.util.Base64.getDecoder().decode(imageBase64)
            } catch (ignored: IllegalArgumentException) {
                imageBase64.toByteArray()
            }
        } ?: imageBase64.toByteArray()
    }
}

/**
 * AI识别状态密封类
 */
sealed class RecognitionState {
    /**
     * 空闲状态，初始状态
     */
    object Idle : RecognitionState()

    /**
     * 加载中状态，表示正在识别物品图片
     */
    object Loading : RecognitionState()

    /**
     * 成功状态，表示识别成功
     */
    object Success : RecognitionState()

    /**
     * 错误状态，包含错误信息
     *
     * @param message 错误信息
     */
    data class Error(val message: String) : RecognitionState()
}

/**
 * 保存状态密封类
 */
sealed class SaveState {
    /**
     * 空闲状态，初始状态
     */
    object Idle : SaveState()

    /**
     * 加载中状态，表示正在保存物品
     */
    object Loading : SaveState()

    /**
     * 成功状态，表示保存成功
     *
     * @param itemId 保存的物品ID
     */
    data class Success(val itemId: String) : SaveState()

    /**
     * 错误状态，包含错误信息
     *
     * @param message 错误信息
     */
    data class Error(val message: String) : SaveState()
}
