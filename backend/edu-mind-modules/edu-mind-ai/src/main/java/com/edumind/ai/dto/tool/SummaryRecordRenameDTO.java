package com.edumind.ai.dto.tool;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 总结记录重命名请求。
 */
@Data
public class SummaryRecordRenameDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 60, message = "标题长度不能超过 60 字")
    private String title;
}
