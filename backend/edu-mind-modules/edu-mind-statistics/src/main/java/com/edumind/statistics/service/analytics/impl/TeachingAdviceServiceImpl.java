package com.edumind.statistics.service.analytics.impl;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.statistics.dto.analytics.TeachingAdviceRequestDTO;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.service.analytics.TeachingAdviceService;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import com.edumind.statistics.vo.analytics.TeachingAdviceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeachingAdviceServiceImpl implements TeachingAdviceService {

    private final KnowledgeMasteryService knowledgeMasteryService;
    private final CourseQueryApi courseQueryApi;

    @Override
    public TeachingAdviceVO generateAdvice(TeachingAdviceRequestDTO request) {
        TeachingAdviceVO vo = new TeachingAdviceVO();
        KnowledgeMasteryVO mastery = knowledgeMasteryService.getMastery(request.getCourseId(), null);
        List<String> weakTitles = mastery.getWeakPoints().stream()
                .map(KnowledgeMasteryVO.WeakPointVO::getTitle)
                .limit(3)
                .collect(Collectors.toList());
        if (request.getFocusKnowledgePointIds() != null && !request.getFocusKnowledgePointIds().isEmpty()) {
            weakTitles = request.getFocusKnowledgePointIds().stream()
                    .map(id -> {
                        KnowledgePointVO kp = courseQueryApi.getKnowledgePointById(id);
                        return kp != null ? kp.getTitle() : "知识点" + id;
                    })
                    .collect(Collectors.toList());
        }
        if (weakTitles.isEmpty()) {
            vo.setSummary("班级整体掌握情况良好，可适度增加综合应用题训练。");
            vo.setActions(List.of("组织阶段性测验", "推送拓展阅读资源"));
            return vo;
        }
        vo.setSummary("当前班级在「" + weakTitles.get(0) + "」等知识点掌握度偏低，建议针对性强化。");
        List<String> actions = new ArrayList<>();
        actions.add("下一次课程重点加强：" + weakTitles.get(0));
        if (weakTitles.size() > 1) {
            actions.add("补充练习：" + weakTitles.get(1));
        }
        actions.add("布置变式题巩固薄弱项");
        vo.setActions(actions);
        return vo;
    }
}
