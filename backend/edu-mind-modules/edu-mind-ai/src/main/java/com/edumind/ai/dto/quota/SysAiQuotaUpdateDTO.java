package com.edumind.ai.dto.quota;

import lombok.Data;

@Data
public class SysAiQuotaUpdateDTO {

    private Integer dailyTokenLimit;
    private Integer dailyCallLimit;
}
