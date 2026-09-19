package com.edumind.course.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class KnowledgePointCreateDTO implements Serializable {

    private Long chapterId;

    @NotBlank(message = "知识点名称不能为空")
    private String title;

    private String code;
    private String description;
    private String cognitiveDimension;
    private Integer importance;
    private String examFocus;
    private Integer sortOrder;
    private List<Long> prerequisiteIds;
}
