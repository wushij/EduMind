package com.edumind.statistics.service.analytics.impl;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.statistics.dao.KnowledgeMasteryDao;
import com.edumind.statistics.entity.KnowledgeMasteryEntity;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeMasteryServiceImpl implements KnowledgeMasteryService {

    private final KnowledgeMasteryDao knowledgeMasteryDao;
    private final CourseQueryApi courseQueryApi;

    @Override
    public KnowledgeMasteryVO getMastery(Long courseId, Long studentId) {
        KnowledgeMasteryVO vo = new KnowledgeMasteryVO();
        List<KnowledgePointVO> points = courseQueryApi.listKnowledgePointsByCourseId(courseId);
        if (points.isEmpty()) {
            return vo;
        }
        List<KnowledgeMasteryEntity> all = knowledgeMasteryDao.listByCourse(courseId);
        Map<Long, String> titles = points.stream()
                .collect(Collectors.toMap(KnowledgePointVO::getId, KnowledgePointVO::getTitle, (a, b) -> a));

        for (KnowledgePointVO point : points) {
            vo.getDimensions().add(point.getTitle());
        }

        if (studentId != null) {
            Map<Long, BigDecimal> personal = knowledgeMasteryDao.listByCourseAndStudent(courseId, studentId)
                    .stream()
                    .collect(Collectors.toMap(KnowledgeMasteryEntity::getKnowledgePointId,
                            KnowledgeMasteryEntity::getMasteryScore, (a, b) -> a));
            for (KnowledgePointVO point : points) {
                BigDecimal score = personal.getOrDefault(point.getId(), BigDecimal.ZERO);
                vo.getPersonal().add(score.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue());
            }
        }

        Map<Long, List<BigDecimal>> classScores = new HashMap<>();
        for (KnowledgeMasteryEntity entity : all) {
            classScores.computeIfAbsent(entity.getKnowledgePointId(), k -> new ArrayList<>())
                    .add(entity.getMasteryScore());
        }
        for (KnowledgePointVO point : points) {
            List<BigDecimal> scores = classScores.getOrDefault(point.getId(), List.of());
            double avg = scores.isEmpty() ? 0
                    : scores.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
            vo.getClassAvg().add((int) Math.round(avg * 100));
        }

        List<KnowledgeMasteryVO.WeakPointVO> weakPoints = new ArrayList<>();
        if (studentId != null) {
            for (KnowledgeMasteryEntity entity : knowledgeMasteryDao.listByCourseAndStudent(courseId, studentId)) {
                if (entity.getMasteryScore().doubleValue() < 0.7) {
                    KnowledgeMasteryVO.WeakPointVO weak = new KnowledgeMasteryVO.WeakPointVO();
                    weak.setKnowledgePointId(entity.getKnowledgePointId());
                    weak.setTitle(titles.getOrDefault(entity.getKnowledgePointId(), "知识点"));
                    weak.setMastery(entity.getMasteryScore().doubleValue());
                    weak.setSuggestion("建议复习相关章节并完成变式练习");
                    weakPoints.add(weak);
                }
            }
        }
        weakPoints.sort(Comparator.comparing(KnowledgeMasteryVO.WeakPointVO::getMastery));
        vo.setWeakPoints(weakPoints.stream().limit(5).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public void upsertMastery(Long studentId, Long courseId, Long knowledgePointId, double scoreRatio) {
        if (studentId == null || knowledgePointId == null) {
            return;
        }
        KnowledgeMasteryEntity existing = knowledgeMasteryDao.findByStudentAndKp(studentId, knowledgePointId);
        BigDecimal newScore = BigDecimal.valueOf(Math.min(1.0, Math.max(0.0, scoreRatio)))
                .setScale(4, RoundingMode.HALF_UP);
        if (existing == null) {
            KnowledgeMasteryEntity entity = new KnowledgeMasteryEntity();
            entity.setStudentId(studentId);
            entity.setCourseId(courseId);
            entity.setKnowledgePointId(knowledgePointId);
            entity.setMasteryScore(newScore);
            entity.setSampleCount(1);
            entity.setLastAssessedAt(LocalDateTime.now());
            knowledgeMasteryDao.insert(entity);
            return;
        }
        int count = existing.getSampleCount() + 1;
        double weighted = (existing.getMasteryScore().doubleValue() * existing.getSampleCount() + newScore.doubleValue()) / count;
        existing.setMasteryScore(BigDecimal.valueOf(weighted).setScale(4, RoundingMode.HALF_UP));
        existing.setSampleCount(count);
        existing.setLastAssessedAt(LocalDateTime.now());
        knowledgeMasteryDao.updateById(existing);
    }

}
