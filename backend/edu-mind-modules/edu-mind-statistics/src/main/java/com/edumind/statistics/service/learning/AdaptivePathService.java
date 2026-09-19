package com.edumind.statistics.service.learning;

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

    public LearningPathDetailVO buildDetail(Long courseId, Long studentId) {
        return orchestrator.buildDetail(courseId, studentId);
    }

    public List<LearningPathStudentItemVO> listCourseStudents(Long courseId) {
        return orchestrator.listCourseStudents(courseId);
    }
}
