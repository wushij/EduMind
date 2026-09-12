package com.edumind.ai.vo.quota;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysAiQuotaVO {

    private Long id;
    private Long userId;
    private Integer dailyTokenLimit;
    private Integer dailyCallLimit;
    private Integer usedTokensToday;
    private Integer usedCallsToday;
    private LocalDateTime updateTime;
}
