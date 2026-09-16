package com.edumind.course.dto.overview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class CourseAnnouncementCreateDTO implements Serializable {
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "公告内容不能为空")
    private String content;

    private Boolean pinned;
}
