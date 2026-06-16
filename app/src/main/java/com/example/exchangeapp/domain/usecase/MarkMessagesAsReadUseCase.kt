package com.example.exchangeapp.domain.usecase

import com.example.exchangeapp.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * 将会话消息标记为已读的 Use Case。
 *
 * 封装单一业务逻辑：将指定会话中的消息全部标记为已读。本 Use Case 以 [Result] 形式
 * 向上层（如聊天界面的 ViewModel）传播成功结果或失败信息。
 *
 * 设计说明：
 * - 调用方（如打开某个会话时的 ViewModel）只需提供会话ID即可清除该会话的未读状态。
 * - 当标记失败时（如数据写入异常），失败信息通过 [Result.failure] 向上传播，
 *   调用方可据此处理或重试。
 *
 * @property chatRepository 聊天仓库，负责消息的读取与已读标记。
 *
 * **验证需求: Requirements 9.6**
 */
class MarkMessagesAsReadUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {

    /**
     * 将指定会话中的消息全部标记为已读。
     *
     * @param conversationId 待标记为已读的会话ID。
     * @return 成功时返回 [Result.success]；标记失败时返回 [Result.failure]。
     */
    suspend operator fun invoke(conversationId: String): Result<Unit> = runCatching {
        chatRepository.markAsRead(conversationId)
    }
}
