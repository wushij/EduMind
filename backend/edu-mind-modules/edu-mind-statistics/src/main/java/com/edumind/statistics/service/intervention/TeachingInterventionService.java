package com.edumind.statistics.service.intervention;

import com.edumind.statistics.dto.intervention.InterventionActionDTO;
import com.edumind.statistics.dto.intervention.InterventionCreateDTO;
import com.edumind.statistics.vo.intervention.TeachingInterventionVO;

import java.util.List;

/**
 * 教学干预建议与决策业务服务接口
 */
public interface TeachingInterventionService {

    /**
     * 创建教学干预建议提案
     */
    TeachingInterventionVO createIntervention(InterventionCreateDTO dto);

    /**
     * 获取指定课程或当前租户的教学干预建议列表
     */
    List<TeachingInterventionVO> listInterventions(Long courseId);

    /**
     * 教师审核批准干预方案
     */
    void approveIntervention(Long id, InterventionActionDTO dto);

    /**
     * 教师驳回或忽略干预建议
     */
    void rejectIntervention(Long id);

    /**
     * 正式执行分发干预（如向目标学生推送习题或微课）
     */
    void dispatchIntervention(Long id);
}
