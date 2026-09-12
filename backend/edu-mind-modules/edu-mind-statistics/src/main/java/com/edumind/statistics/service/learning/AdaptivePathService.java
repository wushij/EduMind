package com.edumind.statistics.service.learning;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import com.edumind.statistics.vo.learning.LearningPathVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdaptivePathService {

    private final KnowledgeMasteryService knowledgeMasteryService;
    private final CourseQueryApi courseQueryApi;

    public LearningPathVO buildAdaptivePath(Long courseId, Long studentId) {
        LearningPathVO path = new LearningPathVO();
        path.setCourseId(courseId);
        KnowledgeMasteryVO mastery = knowledgeMasteryService.getMastery(courseId, studentId);
        List<KnowledgeMasteryVO.WeakPointVO> weak = new ArrayList<>(mastery.getWeakPoints());
        weak.sort(Comparator.comparing(KnowledgeMasteryVO.WeakPointVO::getMastery));
        int week = 1;
        for (KnowledgeMasteryVO.WeakPointVO wp : weak.stream().limit(4).toList()) {
            LearningPathVO.LearningPathWeekVO weekPlan = new LearningPathVO.LearningPathWeekVO();
            weekPlan.setWeekNo(week++);
            weekPlan.setTheme("强化：" + wp.getTitle());
            for (String taskTitle : List.of("复习知识点", "完成变式练习", "AI 讲解巩固")) {
                LearningPathVO.LearningPathTaskVO task = new LearningPathVO.LearningPathTaskVO();
                task.setTitle(taskTitle);
                task.setType("PRACTICE");
                task.setStatus("PENDING");
                weekPlan.getTasks().add(task);
            }
            path.getWeeks().add(weekPlan);
        }
        if (path.getWeeks().isEmpty()) {
            List<KnowledgePointVO> points = courseQueryApi.listKnowledgePointsByCourseId(courseId);
            if (!points.isEmpty()) {
                LearningPathVO.LearningPathWeekVO weekPlan = new LearningPathVO.LearningPathWeekVO();
                weekPlan.setWeekNo(1);
                weekPlan.setTheme("基础巩固：" + points.get(0).getTitle());
                LearningPathVO.LearningPathTaskVO task = new LearningPathVO.LearningPathTaskVO();
                task.setTitle("完成章节练习");
                task.setType("READ");
                task.setStatus("PENDING");
                weekPlan.getTasks().add(task);
                path.getWeeks().add(weekPlan);
            }
        }
        return path;
    }
}
