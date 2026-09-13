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
 * 短信发送记录与审计实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_sms_log")
public class SysSmsLogEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收手机号 */
    private String phone;

    /** 验证码或短信内容摘要 */
    private String content;

    /** 短信业务类型 */
    private String smsType;

    /** 短信模板ID/CODE */
    private String templateId;

    /** 模板参数(JSON) */
    private String templateParams;

    /** 服务商(aliyunAuth/tencent) */
    private String provider;

    /** 状态(0-发送中 1-成功 2-失败) */
    private Integer status;

    /** 回执或错误原因明细 */
    private String resultMsg;

    /** 第三方回执业务ID */
    private String bizId;

    /** 发送时间 */
    private LocalDateTime sendTime;

    /** 触发用户ID */
    private Long userId;

    /** 关联业务模块 */
    private String bizType;

    /** 调用方客户端IP */
    private String ip;

    /** 创建时间 */
    private LocalDateTime createTime;
}
