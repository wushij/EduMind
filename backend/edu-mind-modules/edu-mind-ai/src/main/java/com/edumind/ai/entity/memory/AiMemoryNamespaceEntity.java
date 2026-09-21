package com.edumind.ai.entity.memory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_memory_namespace")
public class AiMemoryNamespaceEntity implements Serializable {

    /**
     * 个人全局空间的占位课程 ID。
     * MySQL 唯一索引中 NULL 不参与去重，若全局空间用 NULL 表示，
     * 并发首访会插入多条全局空间并导致查询抛 TooManyResultsException；
     * 故统一用 0 承载 GLOBAL 语义，对外输出时再还原为 null。
     */
    public static final long GLOBAL_COURSE_ID = 0L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long userId;
    private Long courseId;
    private String scope;
    private Integer consentStatus;
    private Integer retentionDays;
    private Integer status;
    private LocalDateTime createTime;
}
