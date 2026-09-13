package com.edumind.notification.vo.broadcast;

import lombok.Data;

import java.io.Serializable;

@Data
public class BroadcastStatsVO implements Serializable {
    private Long totalBroadcasts;
    private Long totalReach;
    private Long totalRead;
    private Double avgReadRate;
}
