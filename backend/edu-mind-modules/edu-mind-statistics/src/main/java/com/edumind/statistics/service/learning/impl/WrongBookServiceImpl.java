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
import com.edumind.common.markdown.LatexTextNormalizer;
import com.edumind.statistics.vo.learning.WrongBookDetailVO;
import com.edumind.statistics.vo.learning.WrongBookItemVO;
import com.edumind.statistics.vo.learning.WrongBookListVO;
import com.edumind.statistics.vo.learning.WrongBookOverviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WrongBookServiceImpl implements WrongBookService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final double WEAK_THRESHOLD = 0.7;

    /**
     * 演示/历史预置结论的固定格式：以失分类型 code + 冒号开头（如 "CALC: 等价无穷小代换条件应用错误"）。
     * 诊断提示词要求模型把类型标注在句末，因此句首 code 可判定为非大模型产出，
     * 用于在 UI 上区分「演示假结论」与「AI 真结论」。
     */
    private static final Pattern LEGACY_DIAGNOSIS_PATTERN =
            Pattern.compile("^(CONCEPT|LOGIC|CALC|READING)\\s*[:：]");

    /**
     * 历史遗留格式：正文里残留「类型：CONCEPT」「（READING）」「属于 LOGIC」这类标记。
     * 这些标记只用于提取 error_types，当前链路写入前已统一剥离，
     * 因此正文中还能匹配到它们，就说明该结论并非当前链路产出，需要引导用户重新诊断。
     */
    private static final Pattern STALE_TYPE_MARKER_PATTERN = Pattern.compile(
            "(?:错因)?类型\\s*[:：]?\\s*(?:CONCEPT|LOGIC|CALC|READING)"
                    + "|[（(]\\s*(?:CONCEPT|LOGIC|CALC|READING)\\s*[)）]"
                    + "|(?:属于|归为|标记为|判定为|划分为)\\s*(?:CONCEPT|LOGIC|CALC|READING)",
            Pattern.CASE_INSENSITIVE);

    public static final String SOURCE_NONE = "NONE";
    public static final String SOURCE_UNANSWERED = "UNANSWERED";
    public static final String SOURCE_LEGACY = "LEGACY";
    public static final String SOURCE_AI = "AI";

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
        vo.setVariantQuestions(buildVariantSummaries(parseVariantIds(entity.getVariantQuestionIds())));
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
    public List<WrongBookDetailVO.VariantQuestionSummaryVO> generateVariants(Long studentId, Long recordId,
                                                                            boolean regenerate) {
        WrongQuestionRecordEntity entity = requireOwnedRecord(studentId, recordId);
        courseAccessApi.assertCanView(entity.getCourseId());
        return buildVariantSummaries(wrongQuestionDiagnosisService.generateVariants(recordId, regenerate));
    }

    @Override
    public void cancelAiDiagnosis(Long studentId, Long recordId) {
        // 所有权校验：只能中止自己的错题
        requireOwnedRecord(studentId, recordId);
        wrongQuestionDiagnosisService.cancelDiagnosis(recordId);
    }

    @Override
    public void cancelAiVariants(Long studentId, Long recordId) {
        requireOwnedRecord(studentId, recordId);
        wrongQuestionDiagnosisService.cancelVariants(recordId);
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
        item.setDiagnosisSource(resolveDiagnosisSource(entity));
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
        target.setDiagnosisSource(source.getDiagnosisSource());
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

    private List<WrongBookDetailVO.VariantQuestionSummaryVO> buildVariantSummaries(List<Long> variantIds) {
        if (CollectionUtils.isEmpty(variantIds)) {
            return List.of();
        }
        Map<Long, QuestionVO> map = questionQueryApi.listQuestionsByIds(variantIds).stream()
                .collect(Collectors.toMap(QuestionVO::getId, q -> q, (a, b) -> a));
        List<WrongBookDetailVO.VariantQuestionSummaryVO> list = new ArrayList<>();
        for (Long vid : variantIds) {
            QuestionVO question = map.get(vid);
            if (question == null) {
                // 变式题已被删除 / 记录里的 ID 已失效：不再展示"立即自测"点进去必然报错的死题
                log.warn("[错题变式题] 变式题 {} 已不存在，跳过展示", vid);
                continue;
            }
            String stem = question.getStem();
            WrongBookDetailVO.VariantQuestionSummaryVO summary = new WrongBookDetailVO.VariantQuestionSummaryVO();
            summary.setQuestionId(vid);
            summary.setStemPreview(truncateStem(stem, vid));
            // 完整题干并补全裸 LaTeX/Unicode 数学定界符：前端按行数裁切，避免截断公式导致无法渲染
            summary.setStem(LatexTextNormalizer.wrapBareMath(stem));
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

    /** 判定归因结论来源，供前端区分演示数据与大模型实时结论 */
    private static String resolveDiagnosisSource(WrongQuestionRecordEntity entity) {
        String diagnosis = entity.getDiagnosis();
        if (!StringUtils.hasText(diagnosis)) {
            return SOURCE_NONE;
        }
        String trimmed = diagnosis.trim();
        if (WrongQuestionDiagnosisService.UNANSWERED_DIAGNOSIS.equals(trimmed)) {
            return SOURCE_UNANSWERED;
        }
        if (LEGACY_DIAGNOSIS_PATTERN.matcher(trimmed).find()
                || STALE_TYPE_MARKER_PATTERN.matcher(trimmed).find()) {
            return SOURCE_LEGACY;
        }
        return SOURCE_AI;
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
