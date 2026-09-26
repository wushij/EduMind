package com.edumind.ai.dto.tool;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AiToolSaveDTO {

    @NotBlank(message = "工具 ID 不能为空")
    @Size(max = 64, message = "工具 ID 长度不能超过 64")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "工具 ID 仅支持小写字母、数字与下划线，且以字母开头")
    private String id;

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

    /**
     * 运营登记的默认 Chat 模型（可选）。
     *
     * <p>注意：该字段<b>不参与运行时模型选择</b>。工具执行统一走 AI 网关的
     * 「场景路由 → 平台默认模型」解析，工具广场展示的也是该解析结果。
     * 此处仅作运营备注，留空表示"跟随平台默认模型"。</p>
     */
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
