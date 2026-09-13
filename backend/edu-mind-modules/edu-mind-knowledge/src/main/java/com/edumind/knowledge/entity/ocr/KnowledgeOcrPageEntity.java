package com.edumind.knowledge.entity.ocr;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("knowledge_ocr_page")
public class KnowledgeOcrPageEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer pageNo;
    private String rawText;
    private String proofreadText;
    private String blocksJson;
    private BigDecimal confidenceScore;
    private Integer proofreadStatus;
}
