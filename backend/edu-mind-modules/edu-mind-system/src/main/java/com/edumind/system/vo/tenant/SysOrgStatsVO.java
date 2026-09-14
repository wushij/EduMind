package com.edumind.system.vo.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 租户全校组织整体概览指标 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysOrgStatsVO implements Serializable {
    /** 校区单元数 */
    private Integer campusCount;
    /** 学院/年级单元数 */
    private Integer facultyCount;
    /** 行政班级总数 */
    private Integer classCount;
    /** 在册学生总数 */
    private Integer studentCount;
    /** 任课教师总数 */
    private Integer teacherCount;
}
