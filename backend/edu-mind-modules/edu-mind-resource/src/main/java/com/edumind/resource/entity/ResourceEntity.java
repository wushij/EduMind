package com.edumind.resource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 教学资源实体
 */
@Data
@TableName("teaching_resource")
public class ResourceEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long chapterId;
    private String title;
    private String resourceType;
    private String fileUrl;
    private String description;
    private Integer status;
    private LocalDateTime createTime;
}
