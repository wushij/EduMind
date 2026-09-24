package com.edumind.statistics.service.learning.impl;

import com.edumind.common.model.UserContext;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.resource.api.ResourceQueryApi;
import com.edumind.resource.vo.ResourceVO;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.service.learning.RecommendationService;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import com.edumind.statistics.vo.learning.RecommendedQuestionVO;
import com.edumind.statistics.vo.learning.RecommendedResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final QuestionQueryApi questionQueryApi;
    private final ResourceQueryApi resourceQueryApi;
    private final KnowledgeMasteryService knowledgeMasteryService;
    private final CourseQueryApi courseQueryApi;

    @Override
    public List<RecommendedQuestionVO> recommendQuestions(Long courseId, Long chapterId, Integer limit) {
        return recommendQuestionsForStudent(courseId, chapterId, limit, UserContext.getUserId());
    }

    @Override
    public List<RecommendedQuestionVO> recommendQuestionsForStudent(
            Long courseId, Long chapterId, Integer limit, Long studentId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        int size = limit != null && limit > 0 ? limit : 10;
        String courseName = resolveCourseName(courseId);
        // 章节范围收敛：题目表只挂了 knowledge_point_id，没有 chapter_id，
        // 因此「按章节出题」只能走「章节 → 知识点 → 题目」这条链路。
        Set<Long> chapterKpIds = resolveChapterKnowledgePointIds(courseId, chapterId);
        boolean chapterScoped = chapterKpIds != null && !chapterKpIds.isEmpty();

        List<QuestionVO> ordered = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        if (studentId != null) {
            KnowledgeMasteryVO mastery = knowledgeMasteryService.getMastery(courseId, studentId);
            List<KnowledgeMasteryVO.WeakPointVO> weak = new ArrayList<>(mastery.getWeakPoints());
            weak.sort(Comparator.comparing(KnowledgeMasteryVO.WeakPointVO::getMastery));
            for (KnowledgeMasteryVO.WeakPointVO wp : weak) {
                if (wp.getKnowledgePointId() == null) {
                    continue;
                }
                if (chapterScoped && !chapterKpIds.contains(wp.getKnowledgePointId())) {
                    continue;
                }
                appendKnowledgePointQuestions(ordered, seen, wp.getKnowledgePointId());
            }
        }

        if (ordered.size() < size) {
            if (chapterScoped) {
                // 指定章节时只在该章节的知识点范围内补题，避免「第二章的计划推第一章的题」
                for (Long kpId : chapterKpIds) {
                    if (ordered.size() >= size * 2) {
                        break;
                    }
                    appendKnowledgePointQuestions(ordered, seen, kpId);
                }
            } else {
                List<QuestionVO> coursePool = questionQueryApi.listQuestionsByCourseId(courseId);
                if (coursePool != null) {
                    for (QuestionVO q : coursePool) {
                        if (q.getId() != null && seen.add(q.getId())) {
                            ordered.add(q);
                        }
                        if (ordered.size() >= size * 2) {
                            break;
                        }
                    }
                }
            }
        }

        Map<Long, Double> weakMastery = loadWeakMasteryMap(courseId, studentId);
        return ordered.stream()
                .limit(size)
                .map(q -> toQuestionRecommendation(q, courseName, weakMastery))
                .collect(Collectors.toList());
    }

    @Override
    public List<RecommendedResourceVO> recommendResources(Long courseId, Long chapterId, Integer limit) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        int size = limit != null && limit > 0 ? limit : 10;
        String courseName = resolveCourseName(courseId);
        List<ResourceVO> resources = resourceQueryApi.listResourcesByCourse(courseId, chapterId, size * 4);
        if (resources == null || resources.isEmpty()) {
            return Collections.emptyList();
        }
        Long studentId = UserContext.getUserId();
        Set<Long> weakKpIds = loadWeakKnowledgePointIds(courseId, studentId);

        List<ResourceVO> sorted = new ArrayList<>(resources);
        sorted.sort((a, b) -> Integer.compare(
                scoreResource(b, weakKpIds),
                scoreResource(a, weakKpIds)));

        int index = 0;
        List<RecommendedResourceVO> result = new ArrayList<>();
        for (ResourceVO resource : sorted) {
            if (result.size() >= size) {
                break;
            }
            RecommendedResourceVO vo = toResourceRecommendation(resource, courseName, index++, weakKpIds);
            result.add(vo);
        }
        return result;
    }

    /**
     * 解析章节对应的知识点 ID 集合。章节下没有录入知识点时返回 null，
     * 由调用方回退到课程维度，避免「按章节推荐」退化为空结果。
     */
    private Set<Long> resolveChapterKnowledgePointIds(Long courseId, Long chapterId) {
        if (chapterId == null) {
            return null;
        }
        List<KnowledgePointVO> points = courseQueryApi.listKnowledgePointsByCourseId(courseId);
        if (points.isEmpty()) {
            return null;
        }
        Set<Long> ids = points.stream()
                .filter(p -> chapterId.equals(p.getChapterId()))
                .map(KnowledgePointVO::getId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return ids.isEmpty() ? null : ids;
    }

    private void appendKnowledgePointQuestions(List<QuestionVO> ordered, Set<Long> seen, Long kpId) {
        if (kpId == null) {
            return;
        }
        List<QuestionVO> kpQuestions = questionQueryApi.listQuestionsByKnowledgePointId(kpId, 3);
        if (kpQuestions == null) {
            return;
        }
        for (QuestionVO q : kpQuestions) {
            if (q.getId() != null && seen.add(q.getId())) {
                ordered.add(q);
            }
        }
    }

    private Map<Long, Double> loadWeakMasteryMap(Long courseId, Long studentId) {
        if (studentId == null) {
            return Map.of();
        }
        KnowledgeMasteryVO mastery = knowledgeMasteryService.getMastery(courseId, studentId);
        return mastery.getWeakPoints().stream()
                .filter(wp -> wp.getKnowledgePointId() != null)
                .collect(Collectors.toMap(
                        KnowledgeMasteryVO.WeakPointVO::getKnowledgePointId,
                        wp -> wp.getMastery() != null ? wp.getMastery() : 0.5,
                        (a, b) -> a));
    }

    private Set<Long> loadWeakKnowledgePointIds(Long courseId, Long studentId) {
        if (studentId == null) {
            return Set.of();
        }
        KnowledgeMasteryVO mastery = knowledgeMasteryService.getMastery(courseId, studentId);
        return mastery.getWeakPoints().stream()
                .map(KnowledgeMasteryVO.WeakPointVO::getKnowledgePointId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private int scoreResource(ResourceVO resource, Set<Long> weakKpIds) {
        if (resource.getChapterId() != null && weakKpIds.contains(resource.getChapterId())) {
            return 3;
        }
        return 1;
    }

    private String resolveCourseName(Long courseId) {
        List<CourseVO> list = courseQueryApi.listCoursesByIds(List.of(courseId));
        if (list != null && !list.isEmpty() && list.get(0).getName() != null) {
            return list.get(0).getName();
        }
        return "当前课程";
    }

    private RecommendedQuestionVO toQuestionRecommendation(
            QuestionVO question,
            String courseName,
            Map<Long, Double> weakMastery) {
        RecommendedQuestionVO vo = new RecommendedQuestionVO();
        vo.setId(question.getId());
        vo.setStem(question.getStem());
        vo.setType(question.getType());
        vo.setDifficulty(question.getDifficulty());
        vo.setCourseId(question.getCourseId());
        vo.setCourseName(courseName);
        vo.setKnowledgePointId(question.getKnowledgePointId());
        vo.setKnowledgePointName(question.getKnowledgePointName());
        Double mastery = question.getKnowledgePointId() != null
                ? weakMastery.get(question.getKnowledgePointId()) : null;
        if (mastery != null) {
            int gap = (int) Math.round((1.0 - mastery) * 100);
            vo.setMatchScore(Math.min(98, Math.max(72, 70 + gap / 2)));
            vo.setReason("该考点掌握度偏低（约 " + Math.round(mastery * 100) + "%），建议优先巩固");
        } else {
            vo.setMatchScore(80);
            vo.setReason("基于课程题库与学习轨迹的巩固推荐");
        }
        return vo;
    }

    private RecommendedResourceVO toResourceRecommendation(
            ResourceVO resource,
            String courseName,
            int index,
            Set<Long> weakKpIds) {
        RecommendedResourceVO vo = new RecommendedResourceVO();
        vo.setId(resource.getId());
        vo.setTitle(resource.getTitle());
        vo.setResourceType(resource.getResourceType());
        vo.setFileUrl(resource.getFileUrl());
        vo.setCourseId(resource.getCourseId());
        vo.setChapterId(resource.getChapterId());
        vo.setCourseName(courseName);
        boolean weakRelated = resource.getChapterId() != null && weakKpIds.contains(resource.getChapterId());
        vo.setMatchScore(weakRelated ? Math.max(85, 92 - index * 2) : Math.max(70, 88 - index * 3));
        vo.setReason(weakRelated
                ? "与当前薄弱章节相关，建议优先研读"
                : "拓展学习资料，配合当前课程进度阅读");
        return vo;
    }
}
