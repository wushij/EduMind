package com.edumind.knowledge.entity.ocr;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_ocr_task")
public class KnowledgeOcrTaskEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long documentId;
    private String engine;
    private Integer totalPages;
    private Integer processedPages;
    private String status;
    private String errorMsg;
    private LocalDateTime createTime;
}
