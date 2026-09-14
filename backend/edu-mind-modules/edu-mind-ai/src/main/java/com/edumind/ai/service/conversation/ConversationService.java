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
     * 删除单条消息及其配对的一问一答（删提问连带回复，删回复连带提问）。
     */
    List<String> deleteMessageWithPair(String messageId);
}
