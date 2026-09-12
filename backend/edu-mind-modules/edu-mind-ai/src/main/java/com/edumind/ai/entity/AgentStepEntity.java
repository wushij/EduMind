package com.edumind.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("agent_step")
public class AgentStepEntity implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String runId;
    private Integer stepIndex;
    private String stepType;
    private String title;
    private String toolName;
    private String status;
    private String inputPreview;
    private String outputPreview;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
