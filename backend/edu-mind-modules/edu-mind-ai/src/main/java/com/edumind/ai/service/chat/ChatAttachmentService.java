package com.edumind.ai.service.chat;

import com.edumind.ai.vo.chat.ChatAttachmentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ChatAttachmentService {

    /**
     * 上传并解析聊天临时附件
     *
     * @param file 上传的文件
     * @return 附件元数据及解析预览
     */
    ChatAttachmentVO uploadAndParse(MultipartFile file);

    /**
     * 获取指定附件的解析文本
     *
     * @param attachmentId 附件唯一标识
     * @return 解析后的文本内容，若不存在或已过期则返回空字符串
     */
    String getAttachmentText(String attachmentId);

    /**
     * 批量获取附件文本内容组合
     *
     * @param attachmentIds 附件标识列表
     * @return 格式化后的附件全文上下文
     */
    String buildAttachmentsContext(List<String> attachmentIds);
}
