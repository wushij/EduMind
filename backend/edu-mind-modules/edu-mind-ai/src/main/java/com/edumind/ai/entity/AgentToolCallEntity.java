package com.edumind.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("agent_tool_call")
public class AgentToolCallEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String runId;
    private Long stepId;
    private String toolName;
    private String inputJson;
    private String outputJson;
    private String status;
    private Integer durationMs;
    private LocalDateTime createTime;
}
