package com.edumind.statistics.service.analytics.impl;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import com.edumind.statistics.service.analytics.WrongQuestionAnalyticsService;
import com.edumind.statistics.vo.analytics.WrongQuestionAnalyticsVO;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.user.UserBriefVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WrongQuestionAnalyticsServiceImpl implements WrongQuestionAnalyticsService {

    private final WrongQuestionRecordDao wrongQuestionRecordDao;
    private final QuestionQueryApi questionQueryApi;
    private final CourseQueryApi courseQueryApi;
    private final UserQueryApi userQueryApi;

    @Override
    public WrongQuestionAnalyticsVO listWrongQuestions(Long courseId, Long knowledgePointId, int page, int pageSize) {
        WrongQuestionAnalyticsVO vo = new WrongQuestionAnalyticsVO();
        if (courseId == null || courseId <= 0) {
            return vo;
        }

        // 1. 获取课程下所有错题原始记录
        List<WrongQuestionRecordEntity> allRecords = wrongQuestionRecordDao.listByCourse(courseId, knowledgePointId);
        if (CollectionUtils.isEmpty(allRecords)) {
            return vo;
        }

        // 2. 获取该课程的在读学生数（作为班级错误率分母）
        List<Long> enrolledStudentIds = courseQueryApi.listStudentUserIdsByCourseId(courseId);
        int classStudentCount = (enrolledStudentIds != null && !enrolledStudentIds.isEmpty())
                ? enrolledStudentIds.size() : 1;

        // 3. 按 questionId 进行教学维度聚合分组
        Map<Long, List<WrongQuestionRecordEntity>> recordsByQuestion = allRecords.stream()
                .filter(r -> r.getQuestionId() != null)
                .collect(Collectors.groupingBy(
                        WrongQuestionRecordEntity::getQuestionId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 4. 批量查询题库元数据（题干、题型、选项、标准答案、解析等）
        List<Long> questionIds = new ArrayList<>(recordsByQuestion.keySet());
        Map<Long, QuestionVO> questionMap = loadQuestionMap(questionIds);

        // 5. 批量查询做错学生基本信息
        Set<Long> wrongStudentIds = allRecords.stream()
                .map(WrongQuestionRecordEntity::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, UserBriefVO> userMap = userQueryApi.mapUserBriefsByIds(wrongStudentIds);

        // 6. 聚合生成各题目的教学分析明细列表
        List<WrongQuestionAnalyticsVO.WrongQuestionItemVO> allAggregatedItems = new ArrayList<>();
        Map<String, Integer> globalErrorTypeCount = new HashMap<>();
        globalErrorTypeCount.put("CONCEPT", 0);
        globalErrorTypeCount.put("CALC", 0);
        globalErrorTypeCount.put("LOGIC", 0);
        globalErrorTypeCount.put("READING", 0);

        Map<Long, Integer> kpWrongCountMap = new HashMap<>();
        Map<Long, String> kpNameMap = new HashMap<>();
        Set<Long> allVariantIds = new HashSet<>();

        for (Map.Entry<Long, List<WrongQuestionRecordEntity>> entry : recordsByQuestion.entrySet()) {
            Long qId = entry.getKey();
            List<WrongQuestionRecordEntity> group = entry.getValue();
            QuestionVO question = questionMap.get(qId);
            // 优先选择包含学生实际作答的记录作为教学诊断样本
            WrongQuestionRecordEntity sampleRecord = group.stream()
                    .filter(r -> StringUtils.hasText(r.getLastStudentAnswer()))
                    .findFirst()
                    .orElse(group.get(0));

            WrongQuestionAnalyticsVO.WrongQuestionItemVO item = new WrongQuestionAnalyticsVO.WrongQuestionItemVO();
            item.setId(sampleRecord.getId());
            item.setQuestionId(qId);
            item.setClassStudentCount(classStudentCount);


            // 补充题库元数据
            if (question != null) {
                item.setStem(question.getStem());
                item.setType(question.getType());
                item.setTypeName(formatTypeName(question.getType()));
                item.setDifficulty(question.getDifficulty() != null ? question.getDifficulty() : 2);
                item.setOptions(question.getOptions());
                item.setAnswer(question.getAnswer());
                item.setAnalysis(question.getAnalysis());
                item.setKnowledgePointId(question.getKnowledgePointId());
                item.setKnowledgePointName(question.getKnowledgePointName());
            } else {
                item.setStem("试题 " + qId + "（题库已归档或未公开）");
                item.setType("SINGLE");
                item.setTypeName("选择题");
                item.setDifficulty(2);
                item.setKnowledgePointId(sampleRecord.getKnowledgePointId());
            }

            // 兜底补全考点名称
            if (!StringUtils.hasText(item.getKnowledgePointName()) && item.getKnowledgePointId() != null) {
                String kpName = resolveKpName(item.getKnowledgePointId());
                item.setKnowledgePointName(kpName);
            }
            if (item.getKnowledgePointId() != null && StringUtils.hasText(item.getKnowledgePointName())) {
                kpNameMap.put(item.getKnowledgePointId(), item.getKnowledgePointName());
            }

            // 统计做错学生数与班级错误率
            Set<Long> studentsWhoFailed = group.stream()
                    .map(WrongQuestionRecordEntity::getStudentId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            item.setWrongStudentCount(studentsWhoFailed.size());

            int totalWrongTimes = group.stream()
                    .mapToInt(r -> r.getWrongCount() != null ? r.getWrongCount() : 1)
                    .sum();
            item.setWrongCount(totalWrongTimes);

            double rate = Math.min(100.0, (studentsWhoFailed.size() * 100.0) / classStudentCount);
            BigDecimal rateBd = BigDecimal.valueOf(rate).setScale(1, RoundingMode.HALF_UP);
            item.setErrorRate(rateBd.doubleValue());

            // 聚合错因类型
            Set<String> distinctErrorTypes = new LinkedHashSet<>();
            for (WrongQuestionRecordEntity record : group) {
                List<String> types = parseErrorTypes(record.getErrorTypes());
                for (String t : types) {
                    distinctErrorTypes.add(t);
                    globalErrorTypeCount.put(t, globalErrorTypeCount.getOrDefault(t, 0) + 1);
                }
            }
            if (distinctErrorTypes.isEmpty()) {
                distinctErrorTypes.add("CONCEPT");
                globalErrorTypeCount.put("CONCEPT", globalErrorTypeCount.getOrDefault("CONCEPT", 0) + 1);
            }
            item.setErrorTypes(new ArrayList<>(distinctErrorTypes));
            item.setErrorTypeLabels(distinctErrorTypes.stream()
                    .map(this::formatErrorTypeLabel)
                    .collect(Collectors.toList()));

            // 聚合 AI 诊断结论
            String diagnosis = group.stream()
                    .map(WrongQuestionRecordEntity::getDiagnosis)
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse(null);
            if (StringUtils.hasText(diagnosis)) {
                item.setDiagnosis(diagnosis);
                item.setDiagnosisSource(diagnosis.contains("根本原因") ? "AI" : "LEGACY");
            } else {
                item.setDiagnosis("全班共有 " + item.getWrongStudentCount() + " 人在该考点出现偏差，建议开展靶向变式巩固。");
                item.setDiagnosisSource("LEGACY");
            }

            // 聚合变式题 IDs
            Set<Long> variantIds = new LinkedHashSet<>();
            for (WrongQuestionRecordEntity record : group) {
                variantIds.addAll(parseVariantIds(record.getVariantQuestionIds()));
            }
            item.setVariantQuestionIds(new ArrayList<>(variantIds));
            item.setVariantCount(variantIds.size());
            allVariantIds.addAll(variantIds);

            // 构造做错学生明细（穿透数据）
            List<WrongQuestionAnalyticsVO.StudentWrongDetailVO> studentDetails = new ArrayList<>();
            for (WrongQuestionRecordEntity record : group) {
                WrongQuestionAnalyticsVO.StudentWrongDetailVO sVo = new WrongQuestionAnalyticsVO.StudentWrongDetailVO();
                sVo.setStudentId(record.getStudentId());
                UserBriefVO u = userMap.get(record.getStudentId());
                if (u != null) {
                    sVo.setStudentName(StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername());
                    sVo.setStudentNo(u.getUsername());
                } else {
                    sVo.setStudentName("学生 " + record.getStudentId());
                    sVo.setStudentNo(String.valueOf(record.getStudentId()));
                }
                sVo.setLastStudentAnswer(record.getLastStudentAnswer());
                sVo.setWrongCount(record.getWrongCount() != null ? record.getWrongCount() : 1);
                sVo.setUpdateTime(record.getUpdateTime() != null ? record.getUpdateTime() : record.getCreateTime());
                studentDetails.add(sVo);
            }
            studentDetails.sort(Comparator.comparing(WrongQuestionAnalyticsVO.StudentWrongDetailVO::getWrongCount).reversed());
            item.setStudentWrongList(studentDetails);

            // 考点错题人次累加
            if (item.getKnowledgePointId() != null) {
                kpWrongCountMap.put(item.getKnowledgePointId(),
                        kpWrongCountMap.getOrDefault(item.getKnowledgePointId(), 0) + item.getWrongStudentCount());
            }

            allAggregatedItems.add(item);
        }

        // 7. 排序规则：优先做错学生数最多（高频错题），其次总做错次数
        allAggregatedItems.sort(Comparator
                .comparing(WrongQuestionAnalyticsVO.WrongQuestionItemVO::getWrongStudentCount).reversed()
                .thenComparing(Comparator.comparing(WrongQuestionAnalyticsVO.WrongQuestionItemVO::getWrongCount).reversed()));

        // 8. 填充宏观大盘统计数据
        vo.setTotalWrongQuestions((long) allAggregatedItems.size());
        vo.setTotalWrongRecords((long) allRecords.size());
        double avgRate = allAggregatedItems.stream()
                .mapToDouble(WrongQuestionAnalyticsVO.WrongQuestionItemVO::getErrorRate)
                .average()
                .orElse(0.0);
        vo.setAvgErrorRate(BigDecimal.valueOf(avgRate).setScale(1, RoundingMode.HALF_UP).doubleValue());
        vo.setWeakKnowledgePointCount(kpWrongCountMap.size());
        vo.setTotalVariantQuestions(allVariantIds.size());
        vo.setErrorTypeDistribution(globalErrorTypeCount);

        // 填充易错考点 TOP 5
        List<WrongQuestionAnalyticsVO.WeakKpSummaryVO> topWeakKps = kpWrongCountMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    WrongQuestionAnalyticsVO.WeakKpSummaryVO weak = new WrongQuestionAnalyticsVO.WeakKpSummaryVO();
                    weak.setKnowledgePointId(e.getKey());
                    weak.setKnowledgePointName(kpNameMap.getOrDefault(e.getKey(), "考点 " + e.getKey()));
                    weak.setWrongCount(e.getValue());
                    double kRate = Math.min(100.0, (e.getValue() * 100.0) / classStudentCount);
                    weak.setErrorRate(BigDecimal.valueOf(kRate).setScale(1, RoundingMode.HALF_UP).doubleValue());
                    return weak;
                })
                .collect(Collectors.toList());
        vo.setTopWeakKnowledgePoints(topWeakKps);

        // 9. 分页切片
        vo.setTotal((long) allAggregatedItems.size());
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        int fromIndex = (safePage - 1) * safePageSize;
        if (fromIndex >= allAggregatedItems.size()) {
            vo.setList(Collections.emptyList());
        } else {
            int toIndex = Math.min(fromIndex + safePageSize, allAggregatedItems.size());
            vo.setList(allAggregatedItems.subList(fromIndex, toIndex));
        }

        return vo;
    }

    private Map<Long, QuestionVO> loadQuestionMap(List<Long> questionIds) {
        if (CollectionUtils.isEmpty(questionIds)) {
            return Collections.emptyMap();
        }
        try {
            List<QuestionVO> questions = questionQueryApi.listQuestionsByIds(questionIds);
            if (CollectionUtils.isEmpty(questions)) {
                return Collections.emptyMap();
            }
            return questions.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(QuestionVO::getId, q -> q, (a, b) -> a));
        } catch (Exception e) {
            log.error("[错题分析] 跨模块获取题目详情失败: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private String resolveKpName(Long knowledgePointId) {
        try {
            KnowledgePointVO kp = courseQueryApi.getKnowledgePointById(knowledgePointId);
            if (kp != null && StringUtils.hasText(kp.getTitle())) {
                return kp.getTitle();
            }
            return "考点 " + knowledgePointId;
        } catch (Exception e) {
            return "考点 " + knowledgePointId;
        }
    }

    private String formatTypeName(String type) {
        if (!StringUtils.hasText(type)) {
            return "选择题";
        }
        return switch (type.toUpperCase()) {
            case "SINGLE" -> "单选题";
            case "MULTIPLE" -> "多选题";
            case "JUDGE" -> "判断题";
            case "QA", "ESSAY" -> "简答题";
            case "FILL" -> "填空题";
            default -> "综合题";
        };
    }

    private String formatErrorTypeLabel(String code) {
        if (!StringUtils.hasText(code)) {
            return "概念偏差";
        }
        return switch (code.toUpperCase()) {
            case "CONCEPT" -> "概念偏差";
            case "CALC" -> "计算失误";
            case "LOGIC" -> "逻辑漏洞";
            case "READING" -> "审题偏差";
            default -> code;
        };
    }

    private List<String> parseErrorTypes(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private List<Long> parseVariantIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(s -> {
                    try {
                        return Long.valueOf(s);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}

