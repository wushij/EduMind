package com.edumind.notification.vo.broadcast;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class BroadcastRecipientVO implements Serializable {
    private Long id;
    private Long userId;
    private String username;
    private String realName;
    private String avatar;
    private String roleCode;
    private String roleName;
    private Integer isRead;
    private LocalDateTime createTime;
}
