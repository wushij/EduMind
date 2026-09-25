package com.edumind.statistics.dto.intervention;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 教学干预审批与自定义配置操作请求 DTO
 */
@Data
public class InterventionActionDTO implements Serializable {

    /**
     * 教师审批或处理备注
     */
    private String remark;

    /**
     * 调整后的干预方案描述
     */
    private String proposalText;

    /**
     * 自定义替换的微课/课件资源 ID 列表
     */
    private List<Long> resourceIds;

    /**
     * 定向推送的靶向题目 ID 列表
     */
    private List<Long> customQuestionIds;

    /**
     * 自定义调整的目标预警学生用户 ID 列表
     */
    private List<Long> targetStudentIds;
}
