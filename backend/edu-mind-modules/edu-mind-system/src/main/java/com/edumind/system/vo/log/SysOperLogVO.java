package com.edumind.system.vo.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志前端展示模型 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysOperLogVO implements Serializable {

    private Long id;

    private Long tenantId;

    /** 模块标题 */
    private String title;

    /** 业务操作类型 */
    private Integer businessType;

    /** 业务类型文本(如: 新增、修改、删除) */
    private String businessTypeText;

    /** 方法名称 */
    private String method;

    /** 请求方式 */
    private String requestMethod;

    /** 操作人用户ID */
    private Long operUserId;

    /** 操作人员展示名称（含用户名与真实姓名） */
    private String operName;

    /** 请求URL */
    private String operUrl;

    /** 客户端IP */
    private String operIp;

    /** 请求参数(JSON) */
    private String operParam;

    /** 返回参数(JSON) */
    private String jsonResult;

    /** 操作状态(0正常 1异常) */
    private Integer status;

    /** 错误消息 */
    private String errorMsg;

    /** 消耗时间(ms) */
    private Long costTime;

    /** 操作时间 */
    private LocalDateTime operTime;
}
