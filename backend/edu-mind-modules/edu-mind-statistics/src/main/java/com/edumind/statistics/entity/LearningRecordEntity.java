package com.edumind.statistics.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("learning_record")
public class LearningRecordEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long courseId;
    private String actionType;
    private Integer durationMinutes;
    private Long resourceId;
    private LocalDateTime createTime;
}
