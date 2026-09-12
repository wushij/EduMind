package com.edumind.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_message")
public class MessageEntity implements Serializable {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String conversationId;
    private String role;
    private String content;
    private String citationsJson;
    private Integer tokenCount;
    private LocalDateTime createTime;
}
