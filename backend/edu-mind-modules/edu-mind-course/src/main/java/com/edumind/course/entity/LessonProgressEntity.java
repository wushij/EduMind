package com.edumind.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("course_lesson_progress")
public class LessonProgressEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long courseId;
    private Long lessonChapterId;
    private String status;
    private Integer progressPercent;
    private String lastBlockId;
    private LocalDateTime lastStudyAt;
    private LocalDateTime completedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
