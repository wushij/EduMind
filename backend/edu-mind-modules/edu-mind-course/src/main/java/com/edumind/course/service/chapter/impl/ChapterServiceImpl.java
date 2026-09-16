package com.edumind.course.service.chapter.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.course.converter.CourseConverter;
import com.edumind.course.dao.ChapterDao;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.service.access.CourseAccessService;
import com.edumind.course.service.chapter.ChapterService;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {

    private final CourseDao courseDao;
    private final ChapterDao chapterDao;
    private final CourseConverter courseConverter;
    private final CourseAccessService courseAccessService;

    @Override
    public List<ChapterTreeVO> getChapterTree(Long courseId) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        courseAccessService.assertCanView(course);
        return courseConverter.toChapterTree(chapterDao.findByCourseId(courseId));
    }

    @Override
    public Long createChapter(Long courseId, String title, Long parentId, Integer sortOrder) {
        CourseEntity course = courseDao.findById(courseId);
        if (course == null) {
            throw new BusinessException("课程不存在");
        }
        courseAccessService.assertCanEdit(course);
        com.edumind.course.entity.ChapterEntity chapter = new com.edumind.course.entity.ChapterEntity();
        chapter.setCourseId(courseId);
        chapter.setTitle(title);
        chapter.setParentId(parentId != null ? parentId : 0L);
        chapter.setSortOrder(sortOrder != null ? sortOrder : 1);
        chapter.setCreateTime(java.time.LocalDateTime.now());
        chapterDao.insert(chapter);
        return chapter.getId();
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
