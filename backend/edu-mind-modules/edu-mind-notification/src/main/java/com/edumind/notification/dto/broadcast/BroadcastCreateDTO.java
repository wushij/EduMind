package com.edumind.notification.dto.broadcast;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BroadcastCreateDTO {
    @NotBlank(message = "推送标题不能为空")
    private String title;
    @NotBlank(message = "推送内容不能为空")
    private String content;
    /** all | role */
    @NotBlank(message = "推送对象类型不能为空")
    private String targetType;
    /** ADMIN | TEACHER | STUDENT */
    private String targetPayload;
    @Min(0)
    @Max(2)
    private Integer priority;
}
