package com.edumind.notification.vo.broadcast;

import lombok.Data;

import java.io.Serializable;

@Data
public class BroadcastEstimateVO implements Serializable {
    private Long estimatedCount;
    private String targetType;
    private String formattedDesc;
}
