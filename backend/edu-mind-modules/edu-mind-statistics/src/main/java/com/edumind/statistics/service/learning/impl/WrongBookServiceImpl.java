package com.edumind.statistics.service.learning.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.api.CourseAccessApi;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.knowledge.api.KnowledgePointRelationCommandApi;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.statistics.api.KnowledgeMasteryQueryApi;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.enums.WrongErrorType;
import com.edumind.statistics.service.analytics.WrongQuestionDiagnosisService;
import com.edumind.statistics.service.learning.WrongBookService;
import com.edumind.statistics.vo.learning.WrongBookDetailVO;
import com.edumind.statistics.vo.learning.WrongBookItemVO;
import com.edumind.statistics.vo.learning.WrongBookListVO;
import com.edumind.statistics.vo.learning.WrongBookOverviewVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WrongBookServiceImpl implements WrongBookService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final double WEAK_THRESHOLD = 0.7;

    private final WrongQuestionRecordDao wrongQuestionRecordDao;
    private final QuestionQueryApi questionQueryApi;
    private final CourseQueryApi courseQueryApi;
    private final CourseAccessApi courseAccessApi;
    private final KnowledgeMasteryQueryApi knowledgeMasteryQueryApi;
    private final KnowledgePointRelationCommandApi knowledgePointRelationCommandApi;
    private final WrongQuestionDiagnosisService wrongQuestionDiagnosisService;

    @Override
    public WrongBookListVO list(Long studentId, Long courseId, int page, int pageSize,
                                String errorType, Long knowledgePointId, Integer status) {
        courseAccessApi.assertCanView(courseId);
        int effectiveStatus = status != null ? status : 0;
        Page<WrongQuestionRecordEntity> result = wrongQuestionRecordDao.pageByStudent(
                new Page<>(page, pageSize), studentId, courseId, knowledgePointId, errorType, effectiveStatus);

        WrongBookListVO vo = new WrongBookListVO();
        vo.setTotal(result.getTotal());
        if (CollectionUtils.isEmpty(result.getRecords())) {
            return vo;
        }
        Map<Long, QuestionVO> questionMap = loadQuestions(result.getRecords());
        Map<Long, Double> masteryMap = knowledgeMasteryQueryApi.getMasteryByStudentAndCourse(studentId, courseId);
        for (WrongQuestionRecordEntity entity : result.getRecords()) {
            vo.getList().add(toItem(entity, questionMap.get(entity.getQuestionId()), masteryMap));
        }
        return vo;
    }

    @Override
    public WrongBookOverviewVO overview(Long studentId, Long courseId) {
        courseAccessApi.assertCanView(courseId);
        WrongBookOverviewVO vo = new WrongBookOverviewVO();
        vo.setPendingCount(wrongQuestionRecordDao.countByStudentCourseAndStatus(studentId, courseId, 0));
        vo.setMasteredCount(wrongQuestionRecordDao.countByStudentCourseAndStatus(studentId, courseId, 1));

        Map<Long, Double> masteryMap = knowledgeMasteryQueryApi.getMasteryByStudentAndCourse(studentId, courseId);
        List<WrongQuestionRecordEntity> active = wrongQuestionRecordDao.listActiveByStudentAndCourse(studentId, courseId, 500);
        Set<Long> weakKpIds = new HashSet<>();
        for (WrongQuestionRecordEntity record : active) {
            Long kpId = record.getKnowledgePointId();
            if (kpId == null) {
                continue;
            }
            double mastery = masteryMap.getOrDefault(kpId, 0.0);
            if (mastery < WEAK_THRESHOLD) {
                weakKpIds.add(kpId);
            }
        }
        vo.setWeakKnowledgePointCount(weakKpIds.size());
        vo.setVariantConquerRatePercent(computeVariantConquerRate(studentId, courseId, masteryMap));
        return vo;
    }

    @Override
    public WrongBookDetailVO detail(Long studentId, Long recordId) {
        WrongQuestionRecordEntity entity = requireOwnedRecord(studentId, recordId);
        courseAccessApi.assertCanView(entity.getCourseId());

        Map<Long, Double> masteryMap = knowledgeMasteryQueryApi.getMasteryByStudentAndCourse(
                studentId, entity.getCourseId());
        QuestionVO question = questionQueryApi.getQuestionById(entity.getQuestionId());
        Map<Long, QuestionVO> qMap = question != null
                ? Map.of(entity.getQuestionId(), question) : Map.of();

        WrongBookDetailVO vo = new WrongBookDetailVO();
        copyItemFields(vo, toItem(entity, question, masteryMap));
        vo.setPrerequisiteNodes(buildPrerequisiteNodes(entity, masteryMap));
        vo.setVariantQuestions(buildVariantSummaries(entity));
        return vo;
    }

    @Override
    public WrongBookItemVO diagnose(Long studentId, Long recordId) {
        WrongQuestionRecordEntity entity = requireOwnedRecord(studentId, recordId);
        WrongQuestionRecordEntity updated = wrongQuestionDiagnosisService.diagnoseRecord(recordId);
        QuestionVO question = questionQueryApi.getQuestionById(updated.getQuestionId());
        Map<Long, Double> masteryMap = knowledgeMasteryQueryApi.getMasteryByStudentAndCourse(
                studentId, entity.getCourseId());
        return toItem(updated, question, masteryMap);
    }

    @Override
    public void markMastered(Long studentId, Long recordId) {
        WrongQuestionRecordEntity entity = requireOwnedRecord(studentId, recordId);
        entity.setStatus(1);
        entity.setMasteredTime(LocalDateTime.now());
        wrongQuestionRecordDao.updateById(entity);
    }

    private WrongQuestionRecordEntity requireOwnedRecord(Long studentId, Long recordId) {
        WrongQuestionRecordEntity entity = wrongQuestionRecordDao.findById(recordId);
        if (entity == null) {
            throw new BusinessException("错题记录不存在");
        }
        if (!studentId.equals(entity.getStudentId())) {
            throw new BusinessException("无权访问该错题记录");
        }
        return entity;
    }

    private Map<Long, QuestionVO> loadQuestions(List<WrongQuestionRecordEntity> records) {
        List<Long> ids = records.stream()
                .map(WrongQuestionRecordEntity::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return questionQueryApi.listQuestionsByIds(ids).stream()
                .collect(Collectors.toMap(QuestionVO::getId, q -> q, (a, b) -> a));
    }

    private WrongBookItemVO toItem(WrongQuestionRecordEntity entity, QuestionVO question,
                                   Map<Long, Double> masteryMap) {
        WrongBookItemVO item = new WrongBookItemVO();
        item.setId(entity.getId());
        item.setQuestionId(entity.getQuestionId());
        item.setKnowledgePointId(entity.getKnowledgePointId());
        item.setWrongCount(entity.getWrongCount());
        item.setStatus(entity.getStatus() != null ? entity.getStatus() : 0);
        item.setDiagnosis(entity.getDiagnosis());
        item.setStudentAnswer(entity.getLastStudentAnswer());
        if (entity.getCreateTime() != null) {
            item.setCreateTime(entity.getCreateTime().format(TIME_FMT));
        }
        if (StringUtils.hasText(entity.getErrorTypes())) {
            item.setErrorTypes(Arrays.stream(entity.getErrorTypes().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList()));
            item.setErrorTypeLabels(WrongErrorType.labelsForStoredCodes(entity.getErrorTypes()));
        }
        item.setVariantQuestionIds(parseVariantIds(entity.getVariantQuestionIds()));

        if (question != null) {
            item.setStem(question.getStem());
            item.setType(question.getType());
            item.setDifficulty(mapDifficulty(question.getDifficulty()));
            item.setOptions(question.getOptions());
            item.setAnswer(question.getAnswer());
            item.setAnalysis(question.getAnalysis());
            if (StringUtils.hasText(question.getKnowledgePointName())) {
                item.setKnowledgePointName(question.getKnowledgePointName());
            } else if (question.getKnowledgePointId() != null) {
                KnowledgePointVO kp = courseQueryApi.getKnowledgePointById(question.getKnowledgePointId());
                if (kp != null) {
                    item.setKnowledgePointName(kp.getTitle());
                }
            }
        } else if (entity.getKnowledgePointId() != null) {
            KnowledgePointVO kp = courseQueryApi.getKnowledgePointById(entity.getKnowledgePointId());
            if (kp != null) {
                item.setKnowledgePointName(kp.getTitle());
            }
        }
        return item;
    }

    private void copyItemFields(WrongBookDetailVO target, WrongBookItemVO source) {
        target.setId(source.getId());
        target.setQuestionId(source.getQuestionId());
        target.setKnowledgePointId(source.getKnowledgePointId());
        target.setKnowledgePointName(source.getKnowledgePointName());
        target.setWrongCount(source.getWrongCount());
        target.setStatus(source.getStatus());
        target.setDiagnosis(source.getDiagnosis());
        target.setErrorTypes(source.getErrorTypes());
        target.setErrorTypeLabels(source.getErrorTypeLabels());
        target.setVariantQuestionIds(source.getVariantQuestionIds());
        target.setStem(source.getStem());
        target.setType(source.getType());
        target.setDifficulty(source.getDifficulty());
        target.setOptions(source.getOptions());
        target.setAnswer(source.getAnswer());
        target.setAnalysis(source.getAnalysis());
        target.setStudentAnswer(source.getStudentAnswer());
        target.setCreateTime(source.getCreateTime());
    }

    private List<WrongBookDetailVO.KnowledgeGraphNodeVO> buildPrerequisiteNodes(
            WrongQuestionRecordEntity entity, Map<Long, Double> masteryMap) {
        Long kpId = entity.getKnowledgePointId();
        if (kpId == null) {
            return List.of();
        }
        Map<Long, List<Long>> prereqMap = knowledgePointRelationCommandApi
                .listPrerequisiteTargetsBySourceIds(List.of(kpId));
        List<Long> prereqIds = prereqMap.getOrDefault(kpId, List.of());
        List<WrongBookDetailVO.KnowledgeGraphNodeVO> nodes = new ArrayList<>();
        for (Long pid : prereqIds) {
            WrongBookDetailVO.KnowledgeGraphNodeVO node = new WrongBookDetailVO.KnowledgeGraphNodeVO();
            node.setKnowledgePointId(pid);
            node.setCurrent(false);
            node.setName(resolveKpName(pid));
            node.setMasteryPercent(toMasteryPercent(masteryMap.get(pid)));
            nodes.add(node);
        }
        WrongBookDetailVO.KnowledgeGraphNodeVO current = new WrongBookDetailVO.KnowledgeGraphNodeVO();
        current.setKnowledgePointId(kpId);
        current.setCurrent(true);
        current.setName(resolveKpName(kpId));
        current.setMasteryPercent(toMasteryPercent(masteryMap.get(kpId)));
        nodes.add(current);
        return nodes;
    }

    private List<WrongBookDetailVO.VariantQuestionSummaryVO> buildVariantSummaries(
            WrongQuestionRecordEntity entity) {
        List<Long> variantIds = parseVariantIds(entity.getVariantQuestionIds());
        if (variantIds.isEmpty()) {
            return List.of();
        }
        Map<Long, QuestionVO> map = questionQueryApi.listQuestionsByIds(variantIds).stream()
                .collect(Collectors.toMap(QuestionVO::getId, q -> q, (a, b) -> a));
        List<WrongBookDetailVO.VariantQuestionSummaryVO> list = new ArrayList<>();
        for (Long vid : variantIds) {
            WrongBookDetailVO.VariantQuestionSummaryVO summary = new WrongBookDetailVO.VariantQuestionSummaryVO();
            summary.setQuestionId(vid);
            QuestionVO q = map.get(vid);
            summary.setStemPreview(truncateStem(q != null ? q.getStem() : null, vid));
            list.add(summary);
        }
        return list;
    }

    private int computeVariantConquerRate(Long studentId, Long courseId, Map<Long, Double> masteryMap) {
        List<WrongQuestionRecordEntity> withVariants = wrongQuestionRecordDao.listByStudentAndCourse(
                studentId, courseId, 500).stream()
                .filter(r -> StringUtils.hasText(r.getVariantQuestionIds()))
                .collect(Collectors.toList());
        if (withVariants.isEmpty()) {
            return 0;
        }
        long conquered = 0;
        for (WrongQuestionRecordEntity record : withVariants) {
            if (record.getStatus() != null && record.getStatus() == 1) {
                conquered++;
                continue;
            }
            Long kpId = record.getKnowledgePointId();
            if (kpId != null && masteryMap.getOrDefault(kpId, 0.0) >= WEAK_THRESHOLD) {
                conquered++;
            }
        }
        return (int) Math.round(conquered * 100.0 / withVariants.size());
    }

    private String resolveKpName(Long kpId) {
        KnowledgePointVO kp = courseQueryApi.getKnowledgePointById(kpId);
        return kp != null && StringUtils.hasText(kp.getTitle()) ? kp.getTitle() : "考点 #" + kpId;
    }

    private static int toMasteryPercent(Double rate) {
        if (rate == null) {
            return 0;
        }
        return (int) Math.round(Math.min(1.0, Math.max(0.0, rate)) * 100);
    }

    private static String mapDifficulty(Integer diff) {
        if (diff == null) {
            return "MEDIUM";
        }
        if (diff <= 1) {
            return "EASY";
        }
        if (diff >= 3) {
            return "HARD";
        }
        return "MEDIUM";
    }

    private static List<Long> parseVariantIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(Long::valueOf)
                .collect(Collectors.toList());
    }

    private static String truncateStem(String stem, Long questionId) {
        if (!StringUtils.hasText(stem)) {
            return "变式题 Q-" + questionId;
        }
        String plain = stem.replaceAll("<[^>]+>", "").trim();
        if (plain.length() <= 80) {
            return plain;
        }
        return plain.substring(0, 80) + "...";
    }
}
