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
     * 任务执行状态 (PENDING / PROCESSING / SUCCESS / FAILED)
     */
    private String status;

    /**
     * 任务失败原因
     */
    private String errorMsg;

    /**
     * 导出生成的文件下载地址
     */
    private String fileUrl;

    /**
     * 下载鉴权令牌 (UUID 去横线)
     */
    private String downloadToken;

    /**
     * 对象存储 ObjectKey
     */
    private String objectKey;

    /**
     * 导出排版参数快照 (JSON)
     */
    private String exportParams;

    /**
     * 下载链接有效截止时间
     */
    private LocalDateTime expireTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
