package com.edumind.statistics.vo.intervention;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 教学干预建议展示视图对象 VO
 */
@Data
public class TeachingInterventionVO implements Serializable {

    private Long id;

    private Long tenantId;

    private Long courseId;

    private String courseName;

    private String triggerType;

    private String title;

    private String proposalText;

    private Integer affectedStudentCount;

    private String status; // PENDING / APPROVED / DISPATCHED / REVOKED

    private String approvedBy;

    private LocalDateTime createTime;
}
