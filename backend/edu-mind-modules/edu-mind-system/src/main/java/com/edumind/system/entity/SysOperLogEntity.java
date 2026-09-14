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
 * 系统业务操作日志持久化实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_oper_log")
public class SysOperLogEntity implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 模块标题 */
    private String title;

    /** 业务类型(0其它 1新增 2修改 3删除 4查询 5导出 6导入 7授权/变更 8清空) */
    private Integer businessType;

    /** Java方法签名 */
    private String method;

    /** 请求方式(GET/POST/PUT/DELETE) */
    private String requestMethod;

    /** 操作人用户ID */
    private Long operUserId;

    /** 操作人员账号或姓名 */
    private String operName;

    /** 请求URL */
    private String operUrl;

    /** 客户端IP */
    private String operIp;

    /** 请求入参(JSON，含action/diffItems/params，已脱敏) */
    private String operParam;

    /** 返回参数(JSON，截断保护) */
    private String jsonResult;

    /** 操作状态(0正常 1异常) */
    private Integer status;

    /** 错误消息/异常摘要 */
    private String errorMsg;

    /** 消耗时间(ms) */
    private Long costTime;

    /** 操作时间 */
    private LocalDateTime operTime;
}
