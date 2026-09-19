package com.edumind.course.service.knowledge.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.course.converter.CourseConverter;
import com.edumind.course.dao.ChapterDao;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.dao.KnowledgePointDao;
import com.edumind.course.dto.knowledge.KnowledgePointCreateDTO;
import com.edumind.course.dto.knowledge.KnowledgePointUpdateDTO;
import com.edumind.course.entity.ChapterEntity;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.entity.KnowledgePointEntity;
import com.edumind.course.service.access.CourseAccessService;
import com.edumind.course.service.knowledge.KnowledgePointService;
import com.edumind.course.vo.knowledge.KnowledgePointBriefVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.knowledge.api.KnowledgePointRelationCommandApi;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class KnowledgePointServiceImpl implements KnowledgePointService {

    private final CourseDao courseDao;
    private final ChapterDao chapterDao;
    private final KnowledgePointDao knowledgePointDao;
    private final CourseConverter courseConverter;
    private final CourseAccessService courseAccessService;
    private final KnowledgePointRelationCommandApi knowledgePointRelationCommandApi;

    public KnowledgePointServiceImpl(
            CourseDao courseDao,
            ChapterDao chapterDao,
            KnowledgePointDao knowledgePointDao,
            CourseConverter courseConverter,
            CourseAccessService courseAccessService,
            @Lazy KnowledgePointRelationCommandApi knowledgePointRelationCommandApi) {
        this.courseDao = courseDao;
        this.chapterDao = chapterDao;
        this.knowledgePointDao = knowledgePointDao;
        this.courseConverter = courseConverter;
        this.courseAccessService = courseAccessService;
        this.knowledgePointRelationCommandApi = knowledgePointRelationCommandApi;
    }

    @Override
    public List<KnowledgePointVO> listByCourse(Long courseId, Long chapterId) {
        CourseEntity course = requireCourse(courseId);
        courseAccessService.assertCanView(course);
        List<KnowledgePointEntity> entities = knowledgePointDao.findByCourseId(courseId, chapterId);
        return enrichList(courseId, entities);
    }

    @Override
    public KnowledgePointVO getById(Long courseId, Long kpId) {
        CourseEntity course = requireCourse(courseId);
        courseAccessService.assertCanView(course);
        KnowledgePointEntity entity = requireKnowledgePoint(courseId, kpId);
        return enrichOne(courseId, entity);
    }

    @Override
    public KnowledgePointVO create(Long courseId, KnowledgePointCreateDTO dto) {
        CourseEntity course = requireCourse(courseId);
        courseAccessService.assertCanEdit(course);
        assertChapterBelongsToCourse(courseId, dto.getChapterId());
        validatePrerequisiteIds(courseId, null, dto.getPrerequisiteIds());

        KnowledgePointEntity entity = new KnowledgePointEntity();
        entity.setCourseId(courseId);
        applyWritableFields(entity, dto.getChapterId(), dto.getTitle(), dto.getCode(), dto.getDescription(),
                dto.getCognitiveDimension(), dto.getImportance(), dto.getExamFocus(), dto.getSortOrder());
        entity.setCreateTime(LocalDateTime.now());
        if (!StringUtils.hasText(entity.getCode())) {
            long seq = knowledgePointDao.countByCourseId(courseId) + 1;
            entity.setCode(String.format("KP-%d-%03d", courseId, seq));
        }
        knowledgePointDao.insert(entity);
        if (entity.getId() == null) {
            throw new BusinessException("知识点创建失败");
        }
        knowledgePointRelationCommandApi.syncPrerequisites(entity.getId(), dto.getPrerequisiteIds());
        return enrichOne(courseId, entity);
    }

    @Override
    public KnowledgePointVO update(Long courseId, Long kpId, KnowledgePointUpdateDTO dto) {
        CourseEntity course = requireCourse(courseId);
        courseAccessService.assertCanEdit(course);
        KnowledgePointEntity entity = requireKnowledgePoint(courseId, kpId);
        assertChapterBelongsToCourse(courseId, dto.getChapterId());
        validatePrerequisiteIds(courseId, kpId, dto.getPrerequisiteIds());

        applyWritableFields(entity, dto.getChapterId(), dto.getTitle(), dto.getCode(), dto.getDescription(),
                dto.getCognitiveDimension(), dto.getImportance(), dto.getExamFocus(), dto.getSortOrder());
        knowledgePointDao.updateById(entity);
        knowledgePointRelationCommandApi.syncPrerequisites(kpId, dto.getPrerequisiteIds());
        return enrichOne(courseId, entity);
    }

    @Override
    public void delete(Long courseId, Long kpId) {
        CourseEntity course = requireCourse(courseId);
        courseAccessService.assertCanEdit(course);
        requireKnowledgePoint(courseId, kpId);
        knowledgePointRelationCommandApi.deleteAllRelationsForPoint(kpId);
        knowledgePointDao.deleteById(kpId);
    }

    private CourseEntity requireCourse(Long courseId) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        return course;
    }

    private KnowledgePointEntity requireKnowledgePoint(Long courseId, Long kpId) {
        KnowledgePointEntity entity = knowledgePointDao.findById(kpId);
        if (entity == null || !Objects.equals(entity.getCourseId(), courseId)) {
            throw new BusinessException("知识点不存在");
        }
        return entity;
    }

    private void assertChapterBelongsToCourse(Long courseId, Long chapterId) {
        if (chapterId == null) {
            return;
        }
        ChapterEntity chapter = chapterDao.findById(chapterId);
        if (chapter == null || !Objects.equals(chapter.getCourseId(), courseId)) {
            throw new BusinessException("所属章节不属于当前课程");
        }
    }

    private void validatePrerequisiteIds(Long courseId, Long selfId, List<Long> prerequisiteIds) {
        if (CollectionUtils.isEmpty(prerequisiteIds)) {
            return;
        }
        Set<Long> unique = new HashSet<>();
        for (Long id : prerequisiteIds) {
            if (id == null) {
                continue;
            }
            if (selfId != null && id.equals(selfId)) {
                throw new BusinessException("知识点不能将自身设为前置要求");
            }
            unique.add(id);
        }
        if (unique.isEmpty()) {
            return;
        }
        List<KnowledgePointEntity> all = knowledgePointDao.findByCourseId(courseId, null);
        Set<Long> courseKpIds = all.stream().map(KnowledgePointEntity::getId).collect(Collectors.toSet());
        for (Long id : unique) {
            if (!courseKpIds.contains(id)) {
                throw new BusinessException("前置知识点必须属于当前课程");
            }
        }
    }

    private void applyWritableFields(KnowledgePointEntity entity, Long chapterId, String title, String code,
                                     String description, String cognitiveDimension, Integer importance,
                                     String examFocus, Integer sortOrder) {
        if (chapterId != null) {
            entity.setChapterId(chapterId);
        }
        if (StringUtils.hasText(title)) {
            entity.setTitle(title.trim());
        }
        if (code != null) {
            entity.setCode(StringUtils.hasText(code) ? code.trim() : null);
        }
        if (description != null) {
            entity.setDescription(StringUtils.hasText(description) ? description.trim() : null);
        }
        if (cognitiveDimension != null) {
            entity.setCognitiveDimension(StringUtils.hasText(cognitiveDimension) ? cognitiveDimension.trim() : null);
        }
        if (importance != null) {
            entity.setImportance(importance);
        }
        if (examFocus != null) {
            entity.setExamFocus(StringUtils.hasText(examFocus) ? examFocus.trim() : null);
        }
        if (sortOrder != null) {
            entity.setSortOrder(sortOrder);
        }
    }

    private List<KnowledgePointVO> enrichList(Long courseId, List<KnowledgePointEntity> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            return List.of();
        }
        Map<Long, String> titleById = buildTitleMap(courseId);
        List<Long> sourceIds = entities.stream()
                .map(KnowledgePointEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, List<Long>> prereqMap = knowledgePointRelationCommandApi.listPrerequisiteTargetsBySourceIds(sourceIds);
        List<KnowledgePointVO> result = new ArrayList<>();
        for (KnowledgePointEntity entity : entities) {
            result.add(toEnrichedVo(entity, titleById, prereqMap.get(entity.getId())));
        }
        return result;
    }

    private KnowledgePointVO enrichOne(Long courseId, KnowledgePointEntity entity) {
        Map<Long, String> titleById = buildTitleMap(courseId);
        Map<Long, List<Long>> prereqMap = knowledgePointRelationCommandApi.listPrerequisiteTargetsBySourceIds(
                List.of(entity.getId()));
        return toEnrichedVo(entity, titleById, prereqMap.get(entity.getId()));
    }

    private Map<Long, String> buildTitleMap(Long courseId) {
        List<KnowledgePointEntity> all = knowledgePointDao.findByCourseId(courseId, null);
        Map<Long, String> map = new HashMap<>();
        for (KnowledgePointEntity kp : all) {
            if (kp.getId() != null) {
                map.put(kp.getId(), kp.getTitle());
            }
        }
        return map;
    }

    private KnowledgePointVO toEnrichedVo(KnowledgePointEntity entity, Map<Long, String> titleById,
                                           List<Long> prerequisiteIds) {
        KnowledgePointVO vo = courseConverter.toKnowledgePointVO(entity);
        if (prerequisiteIds == null || prerequisiteIds.isEmpty()) {
            vo.setPrerequisiteIds(List.of());
            vo.setPrerequisites(List.of());
            return vo;
        }
        vo.setPrerequisiteIds(prerequisiteIds);
        List<KnowledgePointBriefVO> briefs = new ArrayList<>();
        for (Long pid : prerequisiteIds) {
            String title = titleById.get(pid);
            if (title != null) {
                briefs.add(KnowledgePointBriefVO.builder().id(pid).title(title).build());
            }
        }
        vo.setPrerequisites(briefs);
        return vo;
    }
}
