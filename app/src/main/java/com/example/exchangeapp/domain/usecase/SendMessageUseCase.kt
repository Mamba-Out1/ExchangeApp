package com.example.exchangeapp.domain.usecase

import com.example.exchangeapp.domain.model.ChatMessage
import com.example.exchangeapp.domain.repository.ChatRepository
import javax.inject.Inject

/**
 * 发送聊天消息的 Use Case。
 *
 * 封装单一业务逻辑：将一条 [ChatMessage] 通过 [ChatRepository] 发送出去。本 Use Case 以
 * [Result] 形式向上层（如聊天界面的 ViewModel）传播成功结果或失败信息。
 *
 * 设计说明：
 * - 调用方只需构造好 [ChatMessage] 并调用本 Use Case，无需关心消息的持久化与同步细节。
 * - 当发送失败时（如数据写入异常），失败信息通过 [Result.failure] 向上传播，
 *   调用方可据此提示用户重试。
 *
 * @property chatRepository 聊天仓库，负责消息的发送与持久化。
 *
 * **验证需求: Requirements 9.4**
 */
class SendMessageUseCase @Inject constructor(
    private val chatRepository: ChatRepository
) {

    /**
     * 发送一条聊天消息。
     *
     * @param message 待发送的消息。
     * @return 成功时返回 [Result.success]；发送失败时返回 [Result.failure]。
     */
    suspend operator fun invoke(message: ChatMessage): Result<Unit> =
        chatRepository.sendMessage(message)
}
