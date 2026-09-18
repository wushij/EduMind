package com.edumind.course.service.chapter.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.course.converter.CourseConverter;
import com.edumind.course.dao.ChapterDao;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.service.access.CourseAccessService;
import com.edumind.course.dto.chapter.ChapterCreateDTO;
import com.edumind.course.service.chapter.ChapterService;
import com.edumind.course.service.chapter.ChapterTreeEnrichService;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {

    private final CourseDao courseDao;
    private final ChapterDao chapterDao;
    private final CourseConverter courseConverter;
    private final CourseAccessService courseAccessService;
    private final ChapterTreeEnrichService chapterTreeEnrichService;

    @Override
    public List<ChapterTreeVO> getChapterTree(Long courseId) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        courseAccessService.assertCanView(course);
        List<com.edumind.course.entity.ChapterEntity> flat = chapterDao.findByCourseId(courseId);
        List<ChapterTreeVO> tree = courseConverter.toChapterTree(flat);
        try {
            chapterTreeEnrichService.enrichTree(tree, flat);
        } catch (Exception ex) {
            // Allow chapter tree when lesson-progress / junction tables are not migrated yet
            log.warn("Chapter tree enrich skipped: {}", ex.getMessage());
        }
        return tree;
    }

    @Override
    public Long createChapter(Long courseId, String title, Long parentId, Integer sortOrder) {
        ChapterCreateDTO dto = new ChapterCreateDTO();
        dto.setTitle(title);
        dto.setParentId(parentId);
        dto.setSortOrder(sortOrder);
        return createChapter(courseId, dto);
    }

    @Override
    public Long createChapter(Long courseId, ChapterCreateDTO dto) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        courseAccessService.assertCanEdit(course);
        com.edumind.course.entity.ChapterEntity chapter = new com.edumind.course.entity.ChapterEntity();
        chapter.setCourseId(courseId);
        chapter.setTitle(dto.getTitle());
        chapter.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        chapter.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 1);
        chapter.setDescription(dto.getDescription());
        if (chapter.isLessonNode()) {
            chapter.setDurationMinutes(dto.getDurationMinutes());
            chapter.setLessonType(normalizeLessonType(dto.getLessonType()));
            chapter.setContentStatus("DRAFT");
        }
        chapter.setCreateTime(java.time.LocalDateTime.now());
        chapterDao.insert(chapter);
        return chapter.getId();
    }

    private String normalizeLessonType(String type) {
        if (!StringUtils.hasText(type)) {
            return "LECTURE";
        }
        return type.trim().toUpperCase();
    }

    @Override
    public void updateChapter(Long courseId, Long chapterId, String title, Integer sortOrder) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        courseAccessService.assertCanEdit(course);
        com.edumind.course.entity.ChapterEntity chapter = chapterDao.findById(chapterId);
        if (chapter == null || !courseId.equals(chapter.getCourseId())) {
            throw new BusinessException("章节不存在");
        }
        if (title != null && !title.isBlank()) {
            chapter.setTitle(title.trim());
        }
        if (sortOrder != null) {
            chapter.setSortOrder(sortOrder);
        }
        chapterDao.updateById(chapter);
    }

    @Override
    public void deleteChapter(Long courseId, Long chapterId) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        courseAccessService.assertCanEdit(course);
        com.edumind.course.entity.ChapterEntity chapter = chapterDao.findById(chapterId);
        if (chapter == null || !courseId.equals(chapter.getCourseId())) {
            throw new BusinessException("章节不存在");
        }
        chapterDao.deleteById(chapterId);
        List<com.edumind.course.entity.ChapterEntity> allChapters = chapterDao.findByCourseId(courseId);
        for (com.edumind.course.entity.ChapterEntity c : allChapters) {
            if (chapterId.equals(c.getParentId())) {
                chapterDao.deleteById(c.getId());
            }
        }
    }

}
