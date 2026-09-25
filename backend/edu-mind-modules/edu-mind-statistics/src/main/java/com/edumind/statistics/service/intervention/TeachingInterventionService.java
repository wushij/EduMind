package com.edumind.statistics.service.intervention;

import com.edumind.statistics.dto.intervention.InterventionActionDTO;
import com.edumind.statistics.dto.intervention.InterventionCreateDTO;
import com.edumind.statistics.vo.intervention.InterventionOverviewStatsVO;
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
     * 获取教学干预决策工作台顶部 KPI 统计概览
     */
    InterventionOverviewStatsVO getOverviewStats(Long courseId);

    /**
     * 执行学情诊断巡检：扫描指定课程薄弱考点与预警学生，自动生成循证干预提案
     */
    TeachingInterventionVO scanAndGenerateProposal(Long courseId);

    /**
     * 自定义调整干预方案中的微课与强化题
     */
    void customizeIntervention(Long id, InterventionActionDTO dto);

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

    /**
     * 联动大模型与真实学情 Prompt，推演生成精准教学干预方案（用于前端表单预览与一键回填）
     */
    TeachingInterventionVO generateAiInterventionProposal(com.edumind.statistics.dto.intervention.InterventionAiProposeDTO dto);

    /**
     * 删除指定教学干预决策提案
     */
    void deleteIntervention(Long id);
}
