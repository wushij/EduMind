package com.edumind.ai.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAttachmentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 附件唯一标识 */
    private String attachmentId;

    /** 原始文件名 */
    private String fileName;

    /** 格式化后的大小（如 1.2 MB） */
    private String fileSizeText;

    /** 原始字节大小 */
    private Long fileSizeBytes;

    /** 文件类型/后缀 */
    private String fileType;

    /** 提取预览文本（前 200 字） */
    private String previewText;

    /** 提取总字符数 */
    private Integer charCount;

    /** 解析状态：SUCCESS / FAILED */
    private String parseStatus;

    /** 错误消息 */
    private String errorMessage;
}
