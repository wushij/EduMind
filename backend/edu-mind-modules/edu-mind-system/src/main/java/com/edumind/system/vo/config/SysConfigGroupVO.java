package com.edumind.system.vo.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 系统配置分组响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysConfigGroupVO implements Serializable {

    /** 分组标识 */
    private String groupCode;

    /** 分组名称 */
    private String groupName;

    /** 配置 JSON 内容 */
    private String configValue;

    /** 备注说明 */
    private String remark;
}
