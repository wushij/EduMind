package com.edumind.course.vo.overview;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAnnouncementVO implements Serializable {
    private Long id;
    private Long courseId;
    private String title;
    private String content;
    private Boolean pinned;
    private String status;
    private LocalDateTime publishTime;
    private Long publisherId;
    private String publisherName;
}
