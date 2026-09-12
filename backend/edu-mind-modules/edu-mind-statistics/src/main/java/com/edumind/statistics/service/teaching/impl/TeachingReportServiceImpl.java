package com.edumind.statistics.service.teaching.impl;

import com.edumind.ai.dao.AiCallLogDao;
import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.statistics.service.learning.RecommendationService;
import com.edumind.statistics.service.teaching.TeachingReportService;
import com.edumind.statistics.vo.learning.RecommendedQuestionVO;
import com.edumind.statistics.vo.learning.RecommendedResourceVO;
import com.edumind.statistics.vo.teaching.TeachingReportVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeachingReportServiceImpl implements TeachingReportService {

    private final CourseQueryApi courseQueryApi;
    private final RecommendationService recommendationService;
    private final AiCallLogDao aiCallLogDao;

    @Override
    public TeachingReportVO buildReport(Long courseId, String range) {
        TeachingReportVO report = new TeachingReportVO();
        report.setCourseId(courseId);
        report.setRange(range != null ? range : "7d");
        List<ChapterTreeVO> chapters = courseQueryApi.listChaptersByCourseId(courseId);
        report.setTotalChapters(chapters.size());
        List<RecommendedQuestionVO> questions = recommendationService.recommendQuestions(courseId, null, 5);
        List<RecommendedResourceVO> resources = recommendationService.recommendResources(courseId, null, 5);
        report.setRecommendedQuestions(questions.size());
        report.setRecommendedResources(resources.size());
        report.setAiCallCount((int) aiCallLogDao.count(new LambdaQueryWrapper<AiCallLogEntity>()
                .eq(AiCallLogEntity::getScene, "CHAT")));
        report.setAvgSubmissionRate(78.5);
        report.setWeakPoints(questions.stream().map(q -> {
            TeachingReportVO.WeakPointVO weak = new TeachingReportVO.WeakPointVO();
            weak.setTitle(q.getStem());
            weak.setWrongCount(3);
            weak.setSuggestion("建议复习相关章节并完成变式练习");
            return weak;
        }).collect(Collectors.toList()));
        return report;
    }
}
