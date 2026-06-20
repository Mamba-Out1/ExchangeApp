package com.example.exchangeapp.domain.usecase

import android.util.Base64
import com.example.exchangeapp.data.repository.AIRepository
import com.example.exchangeapp.domain.model.ItemRecognitionResult
import javax.inject.Inject

/**
 * 识别物品图像的 Use Case。
 *
 * 封装单一业务逻辑：接收原始图片字节，将其进行 Base64 编码后调用 [AIRepository]
 * 进行物品识别，并以 [Result] 的形式返回识别结果或失败信息。
 *
 * 设计说明：
 * - 调用方（如 ViewModel）只需提供图片字节数组，无需关心编码细节。
 * - 当识别失败时（如网络错误、API 错误或解析错误），失败信息通过
 *   [Result.failure] 向上传播，调用方可据此提示用户手动输入物品信息。
 *
 * @property aiRepository AI 识别仓库，负责调用图像识别 API。
 *
 * **验证需求: Requirements 1.1, 1.6**
 */
class RecognizeItemImageUseCase @Inject constructor(
    private val aiRepository: AIRepository
) {

    /**
     * 对给定的图片字节进行识别。
     *
     * @param imageBytes 原始图片字节（例如从相机或相册读取的 JPEG/PNG 字节）。
     * @return 成功时返回 [Result.success] 包装的 [ItemRecognitionResult]；
     *         图片为空或识别失败时返回 [Result.failure]，由调用方决定降级处理（如手动输入）。
     */
    suspend operator fun invoke(imageBytes: ByteArray): Result<ItemRecognitionResult> {
        if (imageBytes.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("图片数据为空，无法进行识别")
            )
        }

        // 将图片字节进行 Base64 编码（NO_WRAP 避免插入换行符，确保编码字符串可直接用于 data URI）
        val imageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        // 调用仓库进行识别，并将其 Result 直接向上传播（含失败信息）
        return aiRepository.recognizeItem(imageBase64)
    }
}
