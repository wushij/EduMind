package com.edumind.course.service.lesson.impl;

import com.edumind.common.event.LearningActivityEvent;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.converter.CourseConverter;
import com.edumind.course.dao.ChapterDao;
import com.edumind.course.dao.ChapterKnowledgePointDao;
import com.edumind.course.dao.KnowledgePointDao;
import com.edumind.course.dao.LessonProgressDao;
import com.edumind.course.dto.lesson.LessonProgressUpdateDTO;
import com.edumind.course.dto.lesson.LessonUpdateDTO;
import com.edumind.course.entity.ChapterEntity;
import com.edumind.course.entity.ChapterKnowledgePointEntity;
import com.edumind.course.entity.KnowledgePointEntity;
import com.edumind.course.entity.LessonProgressEntity;
import com.edumind.course.service.access.CourseAccessService;
import com.edumind.course.service.lesson.LessonService;
import com.edumind.course.vo.lesson.CourseLessonProgressSummaryVO;
import com.edumind.course.vo.lesson.LessonDetailVO;
import com.edumind.course.vo.lesson.LessonProgressVO;
import com.edumind.course.vo.lesson.LessonResourceSummaryVO;
import com.edumind.knowledge.api.LessonContentIndexApi;
import com.edumind.resource.api.ResourceQueryApi;
import com.edumind.resource.vo.ResourceVO;
import com.edumind.security.context.LoginUserResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonServiceImpl implements LessonService {

    private final ChapterDao chapterDao;
    private final ChapterKnowledgePointDao chapterKnowledgePointDao;
    private final KnowledgePointDao knowledgePointDao;
    private final LessonProgressDao lessonProgressDao;
    private final CourseConverter courseConverter;
    private final CourseAccessService courseAccessService;
    private final ResourceQueryApi resourceQueryApi;
    private final ApplicationEventPublisher eventPublisher;
    private final LessonContentIndexApi lessonContentIndexApi;

    @Override
    public LessonDetailVO getLessonDetail(Long courseId, Long lessonId, boolean previewDraft) {
        AccessContext access = loadLessonAccess(courseId, lessonId);
        ChapterEntity lesson = access.lesson;
        if (!previewDraft && !"PUBLISHED".equalsIgnoreCase(lesson.getContentStatus())) {
            throw new BusinessException("课节尚未发布，暂不可学习");
        }
        ChapterEntity parent = chapterDao.findById(lesson.getParentId());
        List<ChapterKnowledgePointEntity> links = chapterKnowledgePointDao.findByChapterId(lessonId);
        List<KnowledgePointEntity> kps = links.stream()
                .map(link -> knowledgePointDao.findById(link.getKnowledgePointId()))
                .filter(kp -> kp != null)
                .collect(Collectors.toList());

        List<ResourceVO> resources = resourceQueryApi.listResourcesByCourse(courseId, lessonId, 50);
        LessonProgressVO progressVo = null;
        Long studentId = LoginUserResolver.resolveUserId();
        if (studentId != null) {
            LessonProgressEntity progress = lessonProgressDao.findByStudentAndLesson(studentId, lessonId);
            if (progress != null) {
                progressVo = toProgressVO(progress);
            }
        }

        return LessonDetailVO.builder()
                .id(lesson.getId())
                .courseId(courseId)
                .parentChapterId(lesson.getParentId())
                .parentChapterTitle(parent != null ? parent.getTitle() : null)
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .durationMinutes(lesson.getDurationMinutes())
                .lessonType(lesson.getLessonType())
                .contentStatus(lesson.getContentStatus())
                .contentJson(lesson.getContentJson())
                .publishedAt(lesson.getPublishedAt())
                .knowledgePoints(courseConverter.toKnowledgePointVOList(kps))
                .resources(resources.stream().map(this::toResourceSummary).collect(Collectors.toList()))
                .progress(progressVo)
                .build();
    }

    @Override
    public void updateLesson(Long courseId, Long lessonId, LessonUpdateDTO dto) {
        AccessContext access = loadLessonAccess(courseId, lessonId);
        courseAccessService.assertCanEdit(access.course);
        ChapterEntity lesson = access.lesson;
        if (dto.getTitle() != null && StringUtils.hasText(dto.getTitle())) {
            lesson.setTitle(dto.getTitle().trim());
        }
        if (dto.getDescription() != null) {
            lesson.setDescription(dto.getDescription());
        }
        if (dto.getDurationMinutes() != null) {
            lesson.setDurationMinutes(dto.getDurationMinutes());
        }
        if (dto.getLessonType() != null) {
            lesson.setLessonType(normalizeLessonType(dto.getLessonType()));
        }
        if (dto.getContentJson() != null) {
            lesson.setContentJson(dto.getContentJson());
        }
        if (lesson.getContentStatus() == null) {
            lesson.setContentStatus("DRAFT");
        }
        chapterDao.updateById(lesson);
        if (dto.getKnowledgePointIds() != null) {
            chapterKnowledgePointDao.insertBatch(courseId, lessonId, dto.getKnowledgePointIds());
        }
    }

    @Override
    public void publishLesson(Long courseId, Long lessonId) {
        AccessContext access = loadLessonAccess(courseId, lessonId);
        courseAccessService.assertCanEdit(access.course);
        ChapterEntity lesson = access.lesson;
        boolean hasContent = StringUtils.hasText(lesson.getContentJson());
        List<ResourceVO> resources = resourceQueryApi.listResourcesByCourse(courseId, lessonId, 1);
        if (!hasContent && (resources == null || resources.isEmpty())) {
            throw new BusinessException("发布前请至少配置课节正文或绑定教学资料");
        }
        lesson.setContentStatus("PUBLISHED");
        lesson.setPublishedAt(LocalDateTime.now());
        chapterDao.updateById(lesson);
        try {
            lessonContentIndexApi.ingestLesson(courseId, lessonId);
        } catch (Exception ex) {
            log.error("Lesson copilot index failed after publish courseId={} lessonId={}: {}",
                    courseId, lessonId, ex.getMessage(), ex);
        }
    }

    @Override
    public void unpublishLesson(Long courseId, Long lessonId) {
        AccessContext access = loadLessonAccess(courseId, lessonId);
        courseAccessService.assertCanEdit(access.course);
        ChapterEntity lesson = access.lesson;
        lesson.setContentStatus("DRAFT");
        lesson.setPublishedAt(null);
        chapterDao.updateById(lesson);
        try {
            lessonContentIndexApi.removeLessonIndex(courseId, lessonId);
        } catch (Exception ex) {
            log.warn("Remove lesson index failed courseId={} lessonId={}: {}", courseId, lessonId, ex.getMessage());
        }
    }

    @Override
    public void saveContentDraft(Long courseId, Long lessonId, String contentJson) {
        AccessContext access = loadLessonAccess(courseId, lessonId);
        courseAccessService.assertCanEdit(access.course);
        ChapterEntity lesson = access.lesson;
        lesson.setContentJson(contentJson);
        lesson.setContentStatus("DRAFT");
        chapterDao.updateById(lesson);
        try {
            lessonContentIndexApi.removeLessonIndex(courseId, lessonId);
        } catch (Exception ex) {
            log.warn("Remove lesson index on draft save failed courseId={} lessonId={}: {}",
                    courseId, lessonId, ex.getMessage());
        }
    }

    @Override
    public LessonProgressVO getProgress(Long courseId, Long lessonId) {
        loadLessonAccess(courseId, lessonId);
        Long studentId = LoginUserResolver.requireUserId();
        LessonProgressEntity progress = lessonProgressDao.findByStudentAndLesson(studentId, lessonId);
        return progress != null ? toProgressVO(progress) : LessonProgressVO.builder()
                .status("NOT_STARTED")
                .progressPercent(0)
                .build();
    }

    @Override
    public LessonProgressVO updateProgress(Long courseId, Long lessonId, LessonProgressUpdateDTO dto) {
        AccessContext access = loadLessonAccess(courseId, lessonId);
        if (!"PUBLISHED".equalsIgnoreCase(access.lesson.getContentStatus())) {
            throw new BusinessException("课节尚未发布");
        }
        Long studentId = LoginUserResolver.requireUserId();
        LessonProgressEntity progress = lessonProgressDao.findByStudentAndLesson(studentId, lessonId);
        if (progress == null) {
            progress = new LessonProgressEntity();
            progress.setStudentId(studentId);
            progress.setCourseId(courseId);
            progress.setLessonChapterId(lessonId);
            progress.setStatus("IN_PROGRESS");
            progress.setProgressPercent(0);
            progress.setCreateTime(LocalDateTime.now());
            progress.setUpdateTime(LocalDateTime.now());
            lessonProgressDao.insert(progress);
        }
        if (StringUtils.hasText(dto.getStatus())) {
            progress.setStatus(dto.getStatus());
            if ("COMPLETED".equalsIgnoreCase(dto.getStatus())) {
                progress.setProgressPercent(100);
                progress.setCompletedAt(LocalDateTime.now());
            }
        }
        if (dto.getProgressPercent() != null) {
            progress.setProgressPercent(Math.min(100, Math.max(0, dto.getProgressPercent())));
            if (progress.getProgressPercent() >= 100) {
                progress.setStatus("COMPLETED");
                progress.setCompletedAt(LocalDateTime.now());
            } else if (progress.getProgressPercent() > 0) {
                progress.setStatus("IN_PROGRESS");
            }
        }
        if (dto.getLastBlockId() != null) {
            progress.setLastBlockId(dto.getLastBlockId());
        }
        progress.setLastStudyAt(LocalDateTime.now());
        progress.setUpdateTime(LocalDateTime.now());
        lessonProgressDao.updateById(progress);

        int duration = dto.getDurationMinutes() != null ? dto.getDurationMinutes() : 1;
        String action = "COMPLETED".equalsIgnoreCase(progress.getStatus()) ? "LESSON_COMPLETE" : "STUDY";
        eventPublisher.publishEvent(new LearningActivityEvent(
                this, studentId, courseId, action, duration, null, lessonId, null));

        return toProgressVO(progress);
    }

    @Override
    public CourseLessonProgressSummaryVO getProgressSummary(Long courseId) {
        courseAccessService.assertCanViewByCourseId(courseId);
        List<ChapterEntity> chapters = chapterDao.findByCourseId(courseId);
        List<Long> lessonIds = chapters.stream()
                .filter(ChapterEntity::isLessonNode)
                .map(ChapterEntity::getId)
                .collect(Collectors.toList());
        int total = lessonIds.size();
        int completed = 0;
        Long studentId = LoginUserResolver.resolveUserId();
        if (studentId != null && !lessonIds.isEmpty()) {
            completed = (int) lessonProgressDao.countCompletedLessons(courseId, studentId, lessonIds);
        }
        return CourseLessonProgressSummaryVO.builder()
                .totalLessonCount(total)
                .completedLessonCount(completed)
                .build();
    }

    private LessonResourceSummaryVO toResourceSummary(ResourceVO resource) {
        String url = resource.getId() != null ? resourceQueryApi.getResourceDownloadUrl(resource.getId()) : null;
        return LessonResourceSummaryVO.builder()
                .id(resource.getId())
                .resourceId(resource.getId())
                .title(resource.getTitle())
                .resourceType(resource.getResourceType())
                .downloadUrl(url != null ? url : resource.getFileUrl())
                .build();
    }

    private LessonProgressVO toProgressVO(LessonProgressEntity entity) {
        return LessonProgressVO.builder()
                .status(entity.getStatus())
                .progressPercent(entity.getProgressPercent())
                .lastBlockId(entity.getLastBlockId())
                .lastStudyAt(entity.getLastStudyAt())
                .completedAt(entity.getCompletedAt())
                .build();
    }

    private String normalizeLessonType(String type) {
        if (!StringUtils.hasText(type)) {
            return "LECTURE";
        }
        String upper = type.trim().toUpperCase();
        if ("LECTURE".equals(upper) || "PRACTICE".equals(upper) || "QUIZ".equals(upper)) {
            return upper;
        }
        if ("lecture".equalsIgnoreCase(type)) {
            return "LECTURE";
        }
        if ("practice".equalsIgnoreCase(type)) {
            return "PRACTICE";
        }
        if ("quiz".equalsIgnoreCase(type)) {
            return "QUIZ";
        }
        return upper;
    }

    private AccessContext loadLessonAccess(Long courseId, Long lessonId) {
        com.edumind.course.entity.CourseEntity course = courseAccessService.assertCanViewByCourseId(courseId);
        ChapterEntity lesson = chapterDao.findById(lessonId);
        if (lesson == null || !courseId.equals(lesson.getCourseId()) || !lesson.isLessonNode()) {
            throw new BusinessException("微课节不存在");
        }
        return new AccessContext(course, lesson);
    }

    private record AccessContext(com.edumind.course.entity.CourseEntity course, ChapterEntity lesson) {
    }
}
