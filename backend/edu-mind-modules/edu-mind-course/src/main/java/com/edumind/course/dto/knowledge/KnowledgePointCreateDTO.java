package com.edumind.course.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class KnowledgePointCreateDTO implements Serializable {

    private Long chapterId;

    @NotBlank(message = "知识点名称不能为空")
    private String title;

    private Integer sortOrder;
}
