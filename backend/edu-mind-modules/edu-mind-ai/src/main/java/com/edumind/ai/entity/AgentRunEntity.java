package com.edumind.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("agent_run")
public class AgentRunEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String runId;
    private String agentCode;
    private Long userId;
    private Long courseId;
    private String goal;
    private String status;
    private String resultJson;
    private String modelKey;
    private Integer tokenUsage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
