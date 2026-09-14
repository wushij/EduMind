package com.edumind.system.dto.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 操作日志分页查询 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysOperLogPageQueryDTO implements Serializable {

    /** 页码，默认 1 */
    @Builder.Default
    private Integer pageNo = 1;

    /** 每页条数，默认 10 */
    @Builder.Default
    private Integer pageSize = 10;

    /** 模块名称 */
    private String title;

    /** 操作人员账号或姓名 */
    private String operName;

    /** 业务操作类型 */
    private Integer businessType;

    /** 操作状态(0正常 1异常) */
    private Integer status;

    /** 开始时间 (yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd) */
    private String startTime;

    /** 结束时间 (yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd) */
    private String endTime;

    /** 特定用户ID（用于用户画像详情页查询） */
    private Long operUserId;
}
