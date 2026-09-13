package com.edumind.system.vo.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 邮件发送记录 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailLogVO implements Serializable {

    private Long id;
    private String email;
    private String subject;
    private String content;
    private String scene;
    private String provider;
    private Integer status;
    private String resultMsg;
    private String ip;
    private LocalDateTime createTime;
}
