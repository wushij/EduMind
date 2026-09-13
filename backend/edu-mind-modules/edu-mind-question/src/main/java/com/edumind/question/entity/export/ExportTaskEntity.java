package com.edumind.question.entity.export;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 异步文档与试卷导出任务实体
 */
@Data
@TableName("export_task")
public class ExportTaskEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long userId;

    /**
     * 业务类型 (EXAM_PAPER / TEACHING_REPORT)
     */
    private String bizType;

    /**
     * 业务关联 ID (如 examId)
     */
    private Long bizId;

    /**
     * 任务执行状态 (PROCESSING / SUCCESS / FAILED)
     */
    private String status;

    /**
     * 导出生成的文件下载地址
     */
    private String fileUrl;

    /**
     * 下载链接有效截止时间
     */
    private LocalDateTime expireTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
