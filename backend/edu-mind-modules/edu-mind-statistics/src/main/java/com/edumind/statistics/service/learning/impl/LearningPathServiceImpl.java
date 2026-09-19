package com.edumind.statistics.service.learning.impl;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.statistics.service.learning.LearningPathService;
import com.edumind.statistics.service.learning.RecommendationService;
import com.edumind.statistics.vo.learning.LearningPathVO;
import com.edumind.statistics.vo.learning.RecommendedQuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LearningPathServiceImpl implements LearningPathService {

    private final CourseQueryApi courseQueryApi;
    private final RecommendationService recommendationService;

    @Override
    public LearningPathVO buildPath(Long courseId) {
        LearningPathVO path = new LearningPathVO();
        path.setCourseId(courseId);
        path.setTitle("课程学习路径");
        List<ChapterTreeVO> chapters = courseQueryApi.listChaptersByCourseId(courseId);
        List<RecommendedQuestionVO> questions = recommendationService.recommendQuestions(courseId, null, 12);

        int weekNo = 1;
        List<KnowledgePointVO> courseKps = courseQueryApi.listKnowledgePointsByCourseId(courseId);
        for (ChapterTreeVO chapter : chapters) {
            LearningPathVO.LearningPathWeekVO week = new LearningPathVO.LearningPathWeekVO();
            week.setWeekNo(weekNo++);
            week.setTheme(chapter.getTitle());
            Long chapterKpId = courseKps.stream()
                    .filter(k -> chapter.getId() != null && chapter.getId().equals(k.getChapterId()))
                    .map(KnowledgePointVO::getId)
                    .findFirst()
                    .orElse(null);
            week.setKnowledgePointId(chapterKpId);
            List<LearningPathVO.LearningPathTaskVO> tasks = new ArrayList<>();
            LearningPathVO.LearningPathTaskVO readTask = new LearningPathVO.LearningPathTaskVO();
            readTask.setId("chapter-" + chapter.getId() + "-read");
            readTask.setTitle("学习章节：" + chapter.getTitle());
            readTask.setType("READ");
            readTask.setTypeLabel("章节学习");
            readTask.setRefId(chapter.getId());
            readTask.setKnowledgePointId(chapterKpId);
            readTask.setStatus("PENDING");
            readTask.setEstimatedMinutes(20);
            readTask.setTargetUrl("/course/" + courseId + "/chapters");
            readTask.setActionLabel("去学习");
            tasks.add(readTask);
            if (!questions.isEmpty()) {
                RecommendedQuestionVO question = questions.remove(0);
                LearningPathVO.LearningPathTaskVO practiceTask = new LearningPathVO.LearningPathTaskVO();
                practiceTask.setId("chapter-" + chapter.getId() + "-practice");
                practiceTask.setTitle("巩固练习：" + question.getStem());
                practiceTask.setType("PRACTICE");
                practiceTask.setTypeLabel("巩固练习");
                practiceTask.setRefId(question.getId());
                practiceTask.setKnowledgePointId(question.getKnowledgePointId());
                practiceTask.setStatus("PENDING");
                practiceTask.setEstimatedMinutes(15);
                practiceTask.setTargetUrl("/learning/practice?courseId=" + courseId);
                practiceTask.setActionLabel("开始练习");
                tasks.add(practiceTask);
            }
            week.setTasks(tasks);
            path.getWeeks().add(week);
            if (weekNo > 4) {
                break;
            }
        }
        return path;
    }
}
