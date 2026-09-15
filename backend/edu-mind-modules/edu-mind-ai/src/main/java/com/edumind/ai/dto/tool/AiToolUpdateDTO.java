package com.edumind.ai.dto.tool;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiToolUpdateDTO {

    @NotBlank(message = "工具名称不能为空")
    @Size(max = 128, message = "工具名称长度不能超过 128")
    private String name;

    @Size(max = 512, message = "工具描述长度不能超过 512")
    private String description;

    @Size(max = 1024, message = "详细介绍长度不能超过 1024")
    private String detailedIntro;

    @NotBlank(message = "工具分类不能为空")
    private String category;

    @Size(max = 64, message = "图标名称长度不能超过 64")
    private String icon;

    @Size(max = 64, message = "模型 ID 长度不能超过 64")
    private String modelId;

    private String route;

    private String executionMode;

    @Size(max = 256, message = "标签长度不能超过 256")
    private String tags;

    private Boolean isRecommended;

    private Boolean isHot;

    private Integer sortOrder;

    private Integer status;
}
