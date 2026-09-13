package com.edumind.ai.entity.memory;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_memory_feedback")
public class AiMemoryFeedbackEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long memoryId;
    private Long userId;
    private String feedbackAction;
    private String correctContent;
    private String reason;
    private LocalDateTime createTime;
}
