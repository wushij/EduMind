package com.edumind.statistics.service.learning.support;

import com.edumind.ai.api.QuestionGenerateApi;
import com.edumind.ai.dto.QuestionGenerateDTO;
import com.edumind.question.api.QuestionCommandApi;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.dto.question.QuestionBatchCreateDTO;
import com.edumind.question.dto.question.QuestionCreateDTO;
import com.edumind.question.vo.question.QuestionBatchSaveVO;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.statistics.api.KnowledgeMasteryQueryApi;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.dto.learning.AiPracticeStartDTO;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AiPracticeQuestionSelector {

    private final QuestionQueryApi questionQueryApi;
    private final QuestionGenerateApi questionGenerateApi;
    private final QuestionCommandApi questionCommandApi;
    private final KnowledgeMasteryQueryApi knowledgeMasteryQueryApi;
    private final WrongQuestionRecordDao wrongQuestionRecordDao;

    public SelectionResult select(Long studentId, AiPracticeStartDTO dto) {
        int count = dto.getCount() != null && dto.getCount() > 0 ? dto.getCount() : 5;
        Long courseId = dto.getCourseId();
        String mode = StringUtils.hasText(dto.getMode()) ? dto.getMode() : "WEAK_POINT";

        Set<Long> weakKpIds = knowledgeMasteryQueryApi.getMasteryByStudentAndCourse(studentId, courseId).entrySet()
                .stream()
                .filter(e -> e.getValue() < 0.7)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        List<QuestionVO> pool = Optional.ofNullable(questionQueryApi.listQuestionsByCourseId(courseId))
                .orElse(List.of())
                .stream()
                .filter(q -> q.getStatus() == null || q.getStatus() == 1)
                .collect(Collectors.toCollection(ArrayList::new));

        LinkedHashSet<Long> pickedIds = new LinkedHashSet<>();
        List<QuestionVO> picked = new ArrayList<>();

        if (!CollectionUtils.isEmpty(dto.getSeedQuestionIds())) {
            for (Long qid : dto.getSeedQuestionIds()) {
                addQuestionById(qid, pickedIds, picked);
                if (picked.size() >= count) {
                    break;
                }
            }
        }

        if ("WEAK_POINT".equals(mode)) {
            fillWeakPointQuestions(studentId, courseId, count, pickedIds, picked, weakKpIds, pool);
        } else if ("WRONG_BATCH".equals(mode)) {
            fillWrongBatchQuestions(studentId, courseId, count, pickedIds, picked);
        } else if ("VARIANT".equals(mode) || "SINGLE_VARIANT".equals(mode)) {
            fillVariantQuestions(studentId, courseId, count, pickedIds, picked, dto.getSeedQuestionIds());
        } else if ("KNOWLEDGE_TIER".equals(mode)) {
            fillKnowledgeTierQuestions(dto, count, pickedIds, picked, weakKpIds, pool);
        } else if ("ADAPTIVE_SPRINT".equals(mode)) {
            fillAdaptiveSprintQuestions(count, pickedIds, picked, weakKpIds, pool);
        } else {
            fillDefaultWeak(count, pickedIds, picked, weakKpIds, pool);
        }

        if (picked.size() < count) {
            List<QuestionVO> rest = pool.stream()
                    .filter(q -> !pickedIds.contains(q.getId()))
                    .collect(Collectors.toCollection(ArrayList::new));
            Collections.shuffle(rest);
            for (QuestionVO q : rest) {
                if (picked.size() >= count) {
                    break;
                }
                pickedIds.add(q.getId());
                picked.add(q);
            }
        }

        if (picked.size() < count && "WEAK_POINT".equals(mode)) {
            generateVariantFill(studentId, courseId, count - picked.size(), weakKpIds, pickedIds, picked);
        }

        SelectionResult result = new SelectionResult();
        result.setQuestions(picked.stream().limit(count).collect(Collectors.toList()));
        result.setWeakKnowledgePointCount(weakKpIds.size());
        result.setPendingWrongQuestionCount(
                wrongQuestionRecordDao.countByStudentAndCourse(studentId, courseId));
        result.setWeakPointHint(buildWeakHint(weakKpIds.size(), result.getPendingWrongQuestionCount()));
        result.setEstimatedMinutes(estimateMinutes(count));
        return result;
    }

    private void fillWeakPointQuestions(Long studentId, Long courseId, int count,
                                        Set<Long> pickedIds, List<QuestionVO> picked,
                                        Set<Long> weakKpIds, List<QuestionVO> pool) {
        List<WrongQuestionRecordEntity> wrongRecords =
                wrongQuestionRecordDao.listActiveByStudentAndCourse(studentId, courseId, 50);

        for (WrongQuestionRecordEntity record : wrongRecords) {
            if (picked.size() >= count) {
                break;
            }
            List<Long> variantIds = parseVariantIds(record.getVariantQuestionIds());
            for (Long vid : variantIds) {
                addQuestionById(vid, pickedIds, picked);
                if (picked.size() >= count) {
                    return;
                }
            }
        }

        List<QuestionVO> weakPool = pool.stream()
                .filter(q -> !pickedIds.contains(q.getId()))
                .filter(q -> weakKpIds.isEmpty()
                        || (q.getKnowledgePointId() != null && weakKpIds.contains(q.getKnowledgePointId())))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(weakPool);
        for (QuestionVO q : weakPool) {
            if (picked.size() >= count) {
                break;
            }
            pickedIds.add(q.getId());
            picked.add(q);
        }

        for (WrongQuestionRecordEntity record : wrongRecords) {
            if (picked.size() >= count) {
                break;
            }
            addQuestionById(record.getQuestionId(), pickedIds, picked);
        }
    }

    private void fillWrongBatchQuestions(Long studentId, Long courseId, int count,
                                         Set<Long> pickedIds, List<QuestionVO> picked) {
        List<WrongQuestionRecordEntity> wrongRecords =
                wrongQuestionRecordDao.listActiveByStudentAndCourse(studentId, courseId, 100);
        for (WrongQuestionRecordEntity record : wrongRecords) {
            if (picked.size() >= count) {
                break;
            }
            for (Long vid : parseVariantIds(record.getVariantQuestionIds())) {
                addQuestionById(vid, pickedIds, picked);
                if (picked.size() >= count) {
                    return;
                }
            }
        }
        for (WrongQuestionRecordEntity record : wrongRecords) {
            if (picked.size() >= count) {
                break;
            }
            addQuestionById(record.getQuestionId(), pickedIds, picked);
        }
    }

    private void fillVariantQuestions(Long studentId, Long courseId, int count,
                                      Set<Long> pickedIds, List<QuestionVO> picked,
                                      List<Long> seedQuestionIds) {
        Long seedId = !CollectionUtils.isEmpty(seedQuestionIds) ? seedQuestionIds.get(0) : null;
        WrongQuestionRecordEntity record = null;
        if (seedId != null) {
            record = wrongQuestionRecordDao.findByStudentAndQuestion(studentId, seedId);
        }
        if (record != null) {
            for (Long vid : parseVariantIds(record.getVariantQuestionIds())) {
                addQuestionById(vid, pickedIds, picked);
                if (picked.size() >= count) {
                    return;
                }
            }
            addQuestionById(record.getQuestionId(), pickedIds, picked);
        }
    }

    private void fillKnowledgeTierQuestions(AiPracticeStartDTO dto, int count,
                                            Set<Long> pickedIds, List<QuestionVO> picked,
                                            Set<Long> weakKpIds, List<QuestionVO> pool) {
        Long kpId = dto.getKnowledgePointId();
        String cognitive = dto.getCognitiveLevel();
        List<QuestionVO> filtered = pool.stream()
                .filter(q -> !pickedIds.contains(q.getId()))
                .filter(q -> kpId == null || kpId <= 0 || kpId.equals(q.getKnowledgePointId()))
                .filter(q -> matchesCognitiveLevel(cognitive, q.getCognitiveLevel()))
                .filter(q -> weakKpIds.isEmpty() || q.getKnowledgePointId() == null
                        || weakKpIds.contains(q.getKnowledgePointId()))
                .collect(Collectors.toCollection(ArrayList::new));
        if (filtered.isEmpty()) {
            filtered = pool.stream()
                    .filter(q -> !pickedIds.contains(q.getId()))
                    .filter(q -> kpId == null || kpId <= 0 || kpId.equals(q.getKnowledgePointId()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        Collections.shuffle(filtered);
        for (QuestionVO q : filtered) {
            if (picked.size() >= count) {
                break;
            }
            pickedIds.add(q.getId());
            picked.add(q);
        }
    }

    private void fillAdaptiveSprintQuestions(int count, Set<Long> pickedIds, List<QuestionVO> picked,
                                             Set<Long> weakKpIds, List<QuestionVO> pool) {
        List<QuestionVO> candidates = pool.stream()
                .filter(q -> !pickedIds.contains(q.getId()))
                .filter(q -> weakKpIds.isEmpty() || q.getKnowledgePointId() == null
                        || weakKpIds.contains(q.getKnowledgePointId()))
                .sorted(Comparator.comparingInt(q -> q.getDifficulty() != null ? q.getDifficulty() : 2))
                .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            candidates = pool.stream()
                    .filter(q -> !pickedIds.contains(q.getId()))
                    .sorted(Comparator.comparingInt(q -> q.getDifficulty() != null ? q.getDifficulty() : 2))
                    .collect(Collectors.toList());
        }
        int easy = Math.max(1, count / 3);
        int medium = Math.max(1, count / 3);
        int hard = Math.max(0, count - easy - medium);
        pickByDifficulty(candidates, 1, easy, pickedIds, picked);
        pickByDifficulty(candidates, 2, medium, pickedIds, picked);
        pickByDifficulty(candidates, 3, hard, pickedIds, picked);
        for (QuestionVO q : candidates) {
            if (picked.size() >= count) {
                break;
            }
            if (!pickedIds.contains(q.getId())) {
                pickedIds.add(q.getId());
                picked.add(q);
            }
        }
    }

    private void fillDefaultWeak(int count, Set<Long> pickedIds, List<QuestionVO> picked,
                                 Set<Long> weakKpIds, List<QuestionVO> pool) {
        List<QuestionVO> candidates = pool.stream()
                .filter(q -> !pickedIds.contains(q.getId()))
                .filter(q -> weakKpIds.isEmpty() || (q.getKnowledgePointId() != null
                        && weakKpIds.contains(q.getKnowledgePointId())))
                .collect(Collectors.toCollection(ArrayList::new));
        if (candidates.isEmpty()) {
            candidates = new ArrayList<>(pool);
        }
        Collections.shuffle(candidates);
        for (QuestionVO q : candidates) {
            if (picked.size() >= count) {
                break;
            }
            if (!pickedIds.contains(q.getId())) {
                pickedIds.add(q.getId());
                picked.add(q);
            }
        }
    }

    private void pickByDifficulty(List<QuestionVO> candidates, int difficulty, int limit,
                                  Set<Long> pickedIds, List<QuestionVO> picked) {
        int added = 0;
        for (QuestionVO q : candidates) {
            if (added >= limit) {
                break;
            }
            int d = q.getDifficulty() != null ? q.getDifficulty() : 2;
            if (d != difficulty || pickedIds.contains(q.getId())) {
                continue;
            }
            pickedIds.add(q.getId());
            picked.add(q);
            added++;
        }
    }

    private void generateVariantFill(Long studentId, Long courseId, int need,
                                     Set<Long> weakKpIds, Set<Long> pickedIds, List<QuestionVO> picked) {
        if (need <= 0) {
            return;
        }
        try {
            QuestionGenerateDTO generateDto = new QuestionGenerateDTO();
            generateDto.setCourseId(courseId);
            generateDto.setCount(need);
            generateDto.setDifficulty("MEDIUM");
            generateDto.setQuestionScene("错题变式");
            generateDto.setQuestionTypes(List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE"));
            if (!weakKpIds.isEmpty()) {
                generateDto.setKnowledgePointIds(new ArrayList<>(weakKpIds).subList(0,
                        Math.min(3, weakKpIds.size())));
            }
            List<QuestionVO> generated = questionGenerateApi.generate(generateDto);
            if (CollectionUtils.isEmpty(generated)) {
                return;
            }
            QuestionBatchCreateDTO batchDto = new QuestionBatchCreateDTO();
            batchDto.setCourseId(courseId);
            batchDto.setQuestions(generated.stream()
                    .map(q -> toCreateDto(q, courseId))
                    .collect(Collectors.toList()));
            QuestionBatchSaveVO saved = questionCommandApi.batchSave(batchDto);
            if (saved.getQuestionIds() == null) {
                return;
            }
            int targetSize = picked.size() + need;
            for (Long qid : saved.getQuestionIds()) {
                addQuestionById(qid, pickedIds, picked);
                if (picked.size() >= targetSize) {
                    break;
                }
            }
        } catch (Exception ignored) {
            // 生成失败时保留已有选题
        }
    }

    private QuestionCreateDTO toCreateDto(QuestionVO q, Long courseId) {
        QuestionCreateDTO dto = new QuestionCreateDTO();
        dto.setCourseId(courseId);
        dto.setKnowledgePointId(q.getKnowledgePointId());
        dto.setStem(q.getStem());
        dto.setType(q.getType() != null ? q.getType() : "SINGLE_CHOICE");
        dto.setOptions(q.getOptions());
        dto.setAnswer(q.getAnswer());
        dto.setAnalysis(q.getAnalysis());
        dto.setDifficulty(q.getDifficulty() != null ? q.getDifficulty() : 2);
        dto.setScore(q.getScore() != null ? q.getScore() : 5);
        return dto;
    }

    private void addQuestionById(Long questionId, Set<Long> pickedIds, List<QuestionVO> picked) {
        if (questionId == null || pickedIds.contains(questionId)) {
            return;
        }
        QuestionVO q = questionQueryApi.getQuestionById(questionId);
        if (q != null && (q.getStatus() == null || q.getStatus() == 1)) {
            pickedIds.add(q.getId());
            picked.add(q);
        }
    }

    private List<Long> parseVariantIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : raw.split(",")) {
            try {
                ids.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        return ids;
    }

    private boolean matchesCognitiveLevel(String pref, String questionLevel) {
        if (!StringUtils.hasText(pref) || "ALL".equalsIgnoreCase(pref)) {
            return true;
        }
        if (!StringUtils.hasText(questionLevel)) {
            return true;
        }
        String q = questionLevel.toUpperCase(Locale.ROOT);
        return switch (pref.toUpperCase(Locale.ROOT)) {
            case "UNDERSTAND" -> q.contains("REMEMBER") || q.contains("UNDERSTAND");
            case "APPLY" -> q.contains("APPLY");
            case "EVALUATE" -> q.contains("ANALYZE") || q.contains("EVALUATE") || q.contains("CREATE");
            default -> true;
        };
    }

    private String buildWeakHint(int weakKpCount, long wrongCount) {
        if (weakKpCount > 0 && wrongCount > 0) {
            return "已结合 " + weakKpCount + " 个薄弱考点与 " + wrongCount + " 道历史错题生成练习";
        }
        if (weakKpCount > 0) {
            return "已针对 " + weakKpCount + " 个掌握度偏低的考点选题";
        }
        if (wrongCount > 0) {
            return "已优先纳入错题本中的变式与高频错题";
        }
        return "已从课程题库中为你匹配自适应练习";
    }

    private int estimateMinutes(int count) {
        if (count <= 3) {
            return 5;
        }
        if (count <= 5) {
            return 12;
        }
        return 25;
    }

    @lombok.Data
    public static class SelectionResult {
        private List<QuestionVO> questions = new ArrayList<>();
        private int weakKnowledgePointCount;
        private long pendingWrongQuestionCount;
        private String weakPointHint;
        private int estimatedMinutes;
    }
}
