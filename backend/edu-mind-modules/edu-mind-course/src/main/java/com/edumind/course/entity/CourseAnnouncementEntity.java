package com.edumind.course.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("course_announcement")
public class CourseAnnouncementEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long courseId;
    private String title;
    private String content;
    private Boolean pinned;
    private String status;
    private LocalDateTime publishTime;
    private Long publisherId;
    private String publisherName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
