package com.edumind.course.service.chapter.impl;

import com.edumind.common.exception.BusinessException;
import com.edumind.course.converter.CourseConverter;
import com.edumind.course.dao.ChapterDao;
import com.edumind.course.dao.ChapterKnowledgePointDao;
import com.edumind.course.dao.CourseDao;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.service.access.CourseAccessService;
import com.edumind.course.dto.chapter.ChapterCreateDTO;
import com.edumind.course.service.chapter.ChapterService;
import com.edumind.course.service.chapter.ChapterTreeEnrichService;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.knowledge.api.LessonContentIndexApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChapterServiceImpl implements ChapterService {

    private final CourseDao courseDao;
    private final ChapterDao chapterDao;
    private final ChapterKnowledgePointDao chapterKnowledgePointDao;
    private final CourseConverter courseConverter;
    private final CourseAccessService courseAccessService;
    private final ChapterTreeEnrichService chapterTreeEnrichService;
    private final LessonContentIndexApi lessonContentIndexApi;

    public ChapterServiceImpl(
            CourseDao courseDao,
            ChapterDao chapterDao,
            ChapterKnowledgePointDao chapterKnowledgePointDao,
            CourseConverter courseConverter,
            CourseAccessService courseAccessService,
            ChapterTreeEnrichService chapterTreeEnrichService,
            // knowledge 模块的课节索引服务反向依赖 course 的课节查询 API，构造期存在循环，
            // 必须用 @Lazy 打破（与 LessonServiceImpl 注入 LessonContentIndexApi 的写法一致）
            @Lazy LessonContentIndexApi lessonContentIndexApi) {
        this.courseDao = courseDao;
        this.chapterDao = chapterDao;
        this.chapterKnowledgePointDao = chapterKnowledgePointDao;
        this.courseConverter = courseConverter;
        this.courseAccessService = courseAccessService;
        this.chapterTreeEnrichService = chapterTreeEnrichService;
        this.lessonContentIndexApi = lessonContentIndexApi;
    }

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
    @Transactional(rollbackFor = Exception.class)
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

        // 本次需要删除的节点：章节自身 + 它下属的微课节（父节点被删，子节点必须同步移除）
        List<Long> deletedNodeIds = new ArrayList<>();
        deletedNodeIds.add(chapterId);
        for (com.edumind.course.entity.ChapterEntity child : chapterDao.findByCourseId(courseId)) {
            if (chapterId.equals(child.getParentId())) {
                deletedNodeIds.add(child.getId());
            }
        }

        for (Long nodeId : deletedNodeIds) {
            // 1. 清理该课节在知识库中生成的虚拟文档（文档 / 切片 / 切片纯文本 / 向量索引）。
            //    课节被删除后其讲义已不复存在，若不同步清理，知识库仍会召回已删除课节的内容。
            try {
                lessonContentIndexApi.removeLessonIndex(courseId, nodeId);
            } catch (Exception ex) {
                // 向量库或索引服务不可用时不允许阻塞课节删除，仅记录告警
                log.warn("Remove lesson index before delete failed courseId={} lessonId={}: {}",
                        courseId, nodeId, ex.getMessage());
            }
            // 2. 解除课节与知识点的引用关系。知识点本体是课程级资产（可能被其他课节或题目引用），
            //    是否删除由知识点管理页决定，这里只清理课节侧的关联记录，避免残留孤儿关联。
            chapterKnowledgePointDao.deleteByChapterId(nodeId);
        }

        for (Long nodeId : deletedNodeIds) {
            chapterDao.deleteById(nodeId);
        }
    }

}
