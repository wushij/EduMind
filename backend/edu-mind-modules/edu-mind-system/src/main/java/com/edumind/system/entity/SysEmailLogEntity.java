package com.edumind.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 邮件发送记录与审计实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_email_log")
public class SysEmailLogEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收邮箱 */
    private String email;

    /** 邮件主题 */
    private String subject;

    /** 验证码或邮件摘要 */
    private String content;

    /** 邮件应用场景 */
    private String scene;

    /** 发件服务类型 */
    private String provider;

    /** 发送状态(1-成功 2-失败) */
    private Integer status;

    /** 回执信息或失败异常 */
    private String resultMsg;

    /** 调用方客户端IP */
    private String ip;

    /** 创建时间 */
    private LocalDateTime createTime;
}
