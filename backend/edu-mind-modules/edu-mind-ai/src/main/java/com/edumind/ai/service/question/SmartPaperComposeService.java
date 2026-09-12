package com.edumind.ai.service.question;

import com.edumind.ai.vo.question.SmartPaperComposeVO;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.vo.question.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmartPaperComposeService {

    private final QuestionQueryApi questionQueryApi;

    public SmartPaperComposeVO compose(Long courseId, List<Long> knowledgePointIds, int totalCount, Set<Long> excludeIds) {
        SmartPaperComposeVO vo = new SmartPaperComposeVO();
        @SuppressWarnings("unchecked")
        List<QuestionVO> pool = (List<QuestionVO>) (List<?>) questionQueryApi.listQuestionsByCourseId(courseId);
        if (pool.isEmpty()) {
            vo.setSelectedCount(0);
            vo.setDistinctKnowledgePointCount(0);
            vo.setCoverageRate(0.0);
            return vo;
        }

        Set<Long> targetKp = knowledgePointIds != null && !knowledgePointIds.isEmpty()
                ? new HashSet<>(knowledgePointIds)
                : pool.stream().map(QuestionVO::getKnowledgePointId).filter(id -> id != null).collect(Collectors.toSet());

        Set<Long> usedKp = new HashSet<>();
        List<QuestionVO> selected = new ArrayList<>();
        for (QuestionVO q : pool) {
            if (excludeIds != null && excludeIds.contains(q.getId())) {
                continue;
            }
            if (!targetKp.isEmpty()
                    && q.getKnowledgePointId() != null
                    && !targetKp.contains(q.getKnowledgePointId())) {
                continue;
            }
            if (q.getKnowledgePointId() != null && usedKp.contains(q.getKnowledgePointId())) {
                continue;
            }
            selected.add(q);
            if (q.getKnowledgePointId() != null) {
                usedKp.add(q.getKnowledgePointId());
            }
            if (selected.size() >= totalCount) {
                break;
            }
        }
        while (selected.size() < totalCount && selected.size() < pool.size()) {
            for (QuestionVO q : pool) {
                if (excludeIds != null && excludeIds.contains(q.getId())) {
                    continue;
                }
                if (!selected.contains(q)) {
                    selected.add(q);
                    if (q.getKnowledgePointId() != null) {
                        usedKp.add(q.getKnowledgePointId());
                    }
                    break;
                }
            }
        }

        vo.setQuestions(selected);
        vo.setSelectedCount(selected.size());
        vo.setDistinctKnowledgePointCount(usedKp.size());
        int denominator = Math.max(1, targetKp.isEmpty() ? pool.stream()
                .map(QuestionVO::getKnowledgePointId)
                .filter(id -> id != null)
                .collect(Collectors.toSet()).size() : targetKp.size());
        vo.setCoverageRate(usedKp.size() * 1.0 / denominator);
        return vo;
    }
}
