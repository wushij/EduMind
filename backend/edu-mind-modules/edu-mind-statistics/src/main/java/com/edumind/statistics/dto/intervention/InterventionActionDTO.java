package com.edumind.statistics.dto.intervention;

import lombok.Data;

import java.util.List;

/**
 * 教学干预审批与操作请求 DTO
 */
@Data
public class InterventionActionDTO {

    /**
     * 教师审批或处理备注
     */
    private String remark;

    /**
     * 定向推送的靶向题目 ID 列表
     */
    private List<Long> customQuestionIds;
}
