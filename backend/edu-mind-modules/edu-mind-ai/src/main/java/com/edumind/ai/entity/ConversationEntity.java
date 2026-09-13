package com.edumind.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 会话持久化实体
 */
@Data
@TableName("ai_conversation")
public class ConversationEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private Long tenantId;
    private Long userId;
    private Long courseId;
    private String title;
    private Integer messageCount;
    private Integer totalTokens;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }
}
