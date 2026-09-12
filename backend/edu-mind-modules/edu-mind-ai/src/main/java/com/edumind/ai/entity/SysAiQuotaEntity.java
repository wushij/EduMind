package com.edumind.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_ai_quota")
public class SysAiQuotaEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer dailyTokenLimit;
    private Integer dailyCallLimit;
    private Integer usedTokensToday;
    private Integer usedCallsToday;
    private LocalDateTime updateTime;
}
