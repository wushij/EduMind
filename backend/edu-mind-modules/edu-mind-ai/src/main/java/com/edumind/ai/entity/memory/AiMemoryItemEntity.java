package com.edumind.ai.entity.memory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_memory_item")
public class AiMemoryItemEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long namespaceId;
    private String memoryType;
    private String summary;
    private String contentCiphertext;
    private String sensitivityLevel;
    private String vectorRef;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
}
