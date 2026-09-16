package com.edumind.course.dto.overview;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class CourseAnnouncementUpdateDTO implements Serializable {
    @Size(max = 200)
    private String title;
    private String content;
    private Boolean pinned;
    private String status;
}
