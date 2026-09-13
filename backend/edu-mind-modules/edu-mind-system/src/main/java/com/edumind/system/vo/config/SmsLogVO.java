package com.edumind.system.vo.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 短信发送记录 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SmsLogVO implements Serializable {

    private Long id;
    private String phone;
    private String content;
    private String smsType;
    private String templateId;
    private String provider;
    private Integer status;
    private String resultMsg;
    private String bizId;
    private LocalDateTime sendTime;
    private LocalDateTime createTime;
}
