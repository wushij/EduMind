package com.edumind.system.vo.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户候选项 VO（供系统配置审核人等下拉选择）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOptionVO implements Serializable {

    private Long id;
    private String label;
}
