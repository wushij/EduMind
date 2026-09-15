package com.edumind.notification.vo.broadcast;

import com.edumind.common.api.PageResult;
import lombok.Data;

import java.io.Serializable;

@Data
public class BroadcastRecipientSummaryVO implements Serializable {
    private Long broadcastId;
    private String broadcastTitle;
    private Integer totalCount;
    private Integer readCount;
    private Integer unreadCount;
    private Integer readRate;
    private PageResult<BroadcastRecipientVO> recipients;
}
