package com.edumind.ai.service.question;

import com.edumind.ai.dto.question.SmartPaperComposeDTO;
import com.edumind.ai.vo.question.SmartPaperComposeVO;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.vo.question.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SmartPaperComposeService {

    private final QuestionQueryApi questionQueryApi;

    public SmartPaperComposeVO compose(Long courseId, List<Long> knowledgePointIds, int totalCount, Set<Long> excludeIds) {
        SmartPaperComposeDTO dto = new SmartPaperComposeDTO();
        dto.setCourseId(courseId);
        dto.setKnowledgePointIds(knowledgePointIds);
        dto.setTotalCount(totalCount);
        dto.setExcludeIds(excludeIds);
        return composeV2(dto);
    }

    /**
     * V2 组卷算法：难度分层、题型比例、认知层级过滤、去重与缺口上报
     */
    public SmartPaperComposeVO composeV2(SmartPaperComposeDTO dto) {
        Set<Long> exclude = new HashSet<>();
        if (dto.getExcludeIds() != null) {
            exclude.addAll(dto.getExcludeIds());
        }
        if (dto.getExcludeQuestionIds() != null) {
            exclude.addAll(dto.getExcludeQuestionIds());
        }

        int targetCount = dto.getTotalCount() != null && dto.getTotalCount() > 0 ? dto.getTotalCount() : 10;
        Long courseId = dto.getCourseId();

        @SuppressWarnings("unchecked")
        List<QuestionVO> pool = courseId == null
                ? List.of()
                : (List<QuestionVO>) (List<?>) questionQueryApi.listQuestionsByCourseId(courseId);
        if (pool == null) {
            pool = new ArrayList<>();
        }

        Set<Long> targetKp = dto.getKnowledgePointIds() != null && !dto.getKnowledgePointIds().isEmpty()
                ? new HashSet<>(dto.getKnowledgePointIds())
                : Set.of();
        Set<String> cognitiveFilter = dto.getCognitiveLevels() != null
                ? dto.getCognitiveLevels().stream().map(String::toUpperCase).collect(Collectors.toSet())
                : Set.of();

        List<QuestionVO> filtered = pool.stream()
                .filter(q -> !exclude.contains(q.getId()))
                .filter(q -> targetKp.isEmpty() || q.getKnowledgePointId() == null || targetKp.contains(q.getKnowledgePointId()))
                .filter(q -> cognitiveFilter.isEmpty() || cognitiveFilter.contains(inferCognitiveLevel(q.getDifficulty())))
                .collect(Collectors.toCollection(ArrayList::new));

        double easyRatio = 0.3;
        double hardRatio = 0.2;
        if (dto.getDifficultyDistribution() != null && !dto.getDifficultyDistribution().isEmpty()) {
            easyRatio = dto.getDifficultyDistribution().getOrDefault("EASY", 0.3);
            hardRatio = dto.getDifficultyDistribution().getOrDefault("HARD", 0.2);
        }
        int targetEasy = (int) Math.round(targetCount * easyRatio);
        int targetHard = (int) Math.round(targetCount * hardRatio);
        int targetMedium = targetCount - targetEasy - targetHard;

        List<QuestionVO> easyPool = new ArrayList<>();
        List<QuestionVO> mediumPool = new ArrayList<>();
        List<QuestionVO> hardPool = new ArrayList<>();
        for (QuestionVO q : filtered) {
            int d = q.getDifficulty() != null ? q.getDifficulty() : 2;
            if (d <= 1) {
                easyPool.add(q);
            } else if (d >= 3) {
                hardPool.add(q);
            } else {
                mediumPool.add(q);
            }
        }

        List<QuestionVO> selected = new ArrayList<>();
        selected.addAll(pickFromBucket(easyPool, targetEasy));
        selected.addAll(pickFromBucket(mediumPool, targetMedium));
        selected.addAll(pickFromBucket(hardPool, targetHard));

        if (dto.getTypeRatios() != null && !dto.getTypeRatios().isEmpty()) {
            enforceTypeRatios(selected, dto.getTypeRatios());
        }

        Map<String, Integer> shortfallByDifficulty = new HashMap<>();
        shortfallByDifficulty.put("EASY", Math.max(0, targetEasy - countDifficulty(selected, "EASY")));
        shortfallByDifficulty.put("MEDIUM", Math.max(0, targetMedium - countDifficulty(selected, "MEDIUM")));
        shortfallByDifficulty.put("HARD", Math.max(0, targetHard - countDifficulty(selected, "HARD")));
        int shortfallCount = Math.max(0, targetCount - selected.size());

        Map<String, Integer> typeDist = new HashMap<>();
        Map<String, Integer> diffHist = new HashMap<>();
        diffHist.put("EASY", 0);
        diffHist.put("MEDIUM", 0);
        diffHist.put("HARD", 0);
        Set<Long> usedKp = new HashSet<>();
        double totalScore = 0.0;
        int duplicateHits = 0;

        for (QuestionVO q : selected) {
            String type = q.getType() != null ? q.getType() : "SINGLE_CHOICE";
            typeDist.merge(type, 1, Integer::sum);
            String diff = toDifficultyBucket(q.getDifficulty());
            diffHist.merge(diff, 1, Integer::sum);
            if (q.getKnowledgePointId() != null) {
                usedKp.add(q.getKnowledgePointId());
            }
            totalScore += (q.getScore() != null ? q.getScore() : 5.0);
            if (exclude.contains(q.getId())) {
                duplicateHits++;
            }
        }

        int kpDenominator = targetKp.isEmpty()
                ? (int) pool.stream().map(QuestionVO::getKnowledgePointId).filter(Objects::nonNull).distinct().count()
                : targetKp.size();

        SmartPaperComposeVO vo = new SmartPaperComposeVO();
        vo.setQuestions(selected);
        vo.setSelectedCount(selected.size());
        vo.setDistinctKnowledgePointCount(usedKp.size());
        vo.setTypeDistribution(typeDist);
        vo.setDifficultyHistogram(diffHist);
        vo.setCoverageRate(kpDenominator == 0 ? 0.0 : Math.min(1.0, usedKp.size() * 1.0 / kpDenominator));
        vo.setTotalScore(dto.getTotalScore() != null ? dto.getTotalScore().doubleValue() : totalScore);
        vo.setDuplicateRate(selected.isEmpty() ? 0.0 : duplicateHits * 1.0 / selected.size());
        vo.setShortfallCount(shortfallCount);
        vo.setShortfallByDifficulty(shortfallByDifficulty);
        return vo;
    }

    private List<QuestionVO> pickFromBucket(List<QuestionVO> bucket, int count) {
        if (count <= 0 || CollectionUtils.isEmpty(bucket)) {
            return List.of();
        }
        return bucket.subList(0, Math.min(count, bucket.size()));
    }

    private int countDifficulty(List<QuestionVO> questions, String bucket) {
        int count = 0;
        for (QuestionVO q : questions) {
            if (bucket.equals(toDifficultyBucket(q.getDifficulty()))) {
                count++;
            }
        }
        return count;
    }

    private String toDifficultyBucket(Integer difficulty) {
        int d = difficulty != null ? difficulty : 2;
        if (d <= 1) {
            return "EASY";
        }
        if (d >= 3) {
            return "HARD";
        }
        return "MEDIUM";
    }

    private String inferCognitiveLevel(Integer difficulty) {
        int d = difficulty != null ? difficulty : 2;
        if (d <= 1) {
            return "REMEMBER";
        }
        if (d == 2) {
            return "APPLY";
        }
        if (d == 3) {
            return "ANALYZE";
        }
        return "EVALUATE";
    }

    private void enforceTypeRatios(List<QuestionVO> questions, Map<String, Double> ratios) {
        int total = questions.size();
        int idx = 0;
        for (Map.Entry<String, Double> entry : ratios.entrySet()) {
            int count = (int) Math.round(total * entry.getValue());
            for (int i = 0; i < count && idx < total; i++, idx++) {
                questions.get(idx).setType(entry.getKey());
            }
        }
        while (idx < total) {
            questions.get(idx).setType("SINGLE_CHOICE");
            idx++;
        }
    }
}
