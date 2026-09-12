package com.edumind.statistics.service.learning.impl;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.chapter.ChapterTreeVO;
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
        for (ChapterTreeVO chapter : chapters) {
            LearningPathVO.LearningPathWeekVO week = new LearningPathVO.LearningPathWeekVO();
            week.setWeekNo(weekNo++);
            week.setTheme(chapter.getTitle());
            List<LearningPathVO.LearningPathTaskVO> tasks = new ArrayList<>();
            LearningPathVO.LearningPathTaskVO readTask = new LearningPathVO.LearningPathTaskVO();
            readTask.setTitle("学习章节：" + chapter.getTitle());
            readTask.setType("READ");
            readTask.setRefId(chapter.getId());
            readTask.setStatus("PENDING");
            tasks.add(readTask);
            if (!questions.isEmpty()) {
                RecommendedQuestionVO question = questions.remove(0);
                LearningPathVO.LearningPathTaskVO practiceTask = new LearningPathVO.LearningPathTaskVO();
                practiceTask.setTitle("巩固练习：" + question.getStem());
                practiceTask.setType("PRACTICE");
                practiceTask.setRefId(question.getId());
                practiceTask.setStatus("PENDING");
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
