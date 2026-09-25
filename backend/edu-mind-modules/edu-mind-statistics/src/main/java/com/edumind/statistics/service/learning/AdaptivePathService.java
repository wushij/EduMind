package com.edumind.statistics.service.learning;

import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import com.edumind.statistics.vo.learning.LearningPathDetailVO;
import com.edumind.statistics.vo.learning.LearningPathStudentItemVO;
import com.edumind.statistics.vo.learning.LearningPathVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdaptivePathService {

    private final AdaptivePathOrchestrator orchestrator;

    public LearningPathVO buildAdaptivePath(Long courseId, Long studentId) {
        return orchestrator.buildAdaptivePath(courseId, studentId);
    }

    /**
     * 构建自适应学习路径，并复用调用方已加载的掌握度，避免同一请求内重复执行
     * 「全班 × 全考点」矩阵计算。
     *
     * @param presetMastery 调用方已加载的掌握度；为 null 时由编排器自行加载
     */
    public LearningPathVO buildAdaptivePath(Long courseId, Long studentId, KnowledgeMasteryVO presetMastery) {
        return orchestrator.buildAdaptivePath(courseId, studentId, presetMastery);
    }

    public LearningPathDetailVO buildDetail(Long courseId, Long studentId) {
        return orchestrator.buildDetail(courseId, studentId);
    }

    public List<LearningPathStudentItemVO> listCourseStudents(Long courseId) {
        return orchestrator.listCourseStudents(courseId);
    }
}
