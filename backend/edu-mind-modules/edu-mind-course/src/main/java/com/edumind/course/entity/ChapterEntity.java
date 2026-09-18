package com.edumind.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("course_chapter")
public class ChapterEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long parentId;
    private String title;
    private String description;
    private Integer sortOrder;
    private Integer durationMinutes;
    private String lessonType;
    private String contentJson;
    private String contentStatus;
    private LocalDateTime publishedAt;
    private LocalDateTime createTime;

    public boolean isLessonNode() {
        return parentId != null && parentId > 0;
    }
}
