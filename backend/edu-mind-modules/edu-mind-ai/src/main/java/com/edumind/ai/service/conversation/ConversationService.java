package com.edumind.ai.service.conversation;

import com.edumind.ai.dto.ConversationCreateDTO;
import com.edumind.ai.dto.ConversationRenameDTO;
import com.edumind.ai.vo.ConversationVO;
import com.edumind.ai.vo.MessageVO;

import java.util.List;

public interface ConversationService {

    List<ConversationVO> listConversations(Long courseId);

    ConversationVO createConversation(ConversationCreateDTO dto);

    List<MessageVO> listMessages(String conversationId);

    void renameConversation(String conversationId, ConversationRenameDTO dto);

    void deleteConversation(String conversationId);

    String generateTitle(String conversationId);

    /**
     * 会话标题仍是「提问前缀」时，异步用模型生成短标题。
     *
     * <p>标题生成不能只依赖前端那次 generate-title 请求（页面切走 / 请求被中断就丢了，
     * 表现为历史列表长期显示提问前 20 字），因此答完一轮后由服务端兜底触发；
     * 用户手动改过名字的会话不会被覆盖。</p>
     */
    void generateTitleIfAutoDerivedAsync(String conversationId);

    /**
     * 删除单条消息及其配对的一问一答（删提问连带回复，删回复连带提问）。
     */
    List<String> deleteMessageWithPair(String messageId);
}
