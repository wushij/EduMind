package com.edumind.system.vo.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 角色候选项 VO（供系统配置下拉选择）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleOptionVO implements Serializable {

    private Long id;
    private String name;
    private String code;
}
