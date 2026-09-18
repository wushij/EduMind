package com.edumind.course.service.chapter;

import com.edumind.course.dao.ChapterKnowledgePointDao;
import com.edumind.course.dao.LessonProgressDao;
import com.edumind.course.entity.ChapterEntity;
import com.edumind.course.entity.LessonProgressEntity;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.chapter.LessonMetaVO;
import com.edumind.security.context.LoginUserResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChapterTreeEnrichService {

    private final ChapterKnowledgePointDao chapterKnowledgePointDao;
    private final LessonProgressDao lessonProgressDao;

    public void enrichTree(List<ChapterTreeVO> roots, List<ChapterEntity> flatEntities) {
        if (roots == null || roots.isEmpty()) {
            return;
        }
        Map<Long, ChapterEntity> entityMap = flatEntities.stream()
                .collect(Collectors.toMap(ChapterEntity::getId, e -> e, (a, b) -> a));
        List<Long> lessonIds = flatEntities.stream()
                .filter(ChapterEntity::isLessonNode)
                .map(ChapterEntity::getId)
                .collect(Collectors.toList());
        Map<Long, Long> kpCountMap = chapterKnowledgePointDao.countByChapterIds(lessonIds);
        Long studentId = LoginUserResolver.resolveUserId();
        Map<Long, LessonProgressEntity> progressMap = studentId != null
                ? lessonProgressDao.mapByStudentAndLessons(studentId, lessonIds)
                : Map.of();

        for (ChapterTreeVO root : roots) {
            enrichNode(root, entityMap, kpCountMap, progressMap);
        }
    }

    private void enrichNode(ChapterTreeVO node,
                            Map<Long, ChapterEntity> entityMap,
                            Map<Long, Long> kpCountMap,
                            Map<Long, LessonProgressEntity> progressMap) {
        ChapterEntity entity = entityMap.get(node.getId());
        if (entity != null && entity.isLessonNode()) {
            LessonProgressEntity progress = progressMap.get(entity.getId());
            boolean completed = progress != null && "COMPLETED".equalsIgnoreCase(progress.getStatus());
            boolean hasContent = StringUtils.hasText(entity.getContentJson());
            node.setLessonMeta(LessonMetaVO.builder()
                    .durationMinutes(entity.getDurationMinutes())
                    .lessonType(entity.getLessonType())
                    .contentStatus(entity.getContentStatus())
                    .knowledgePointCount(kpCountMap.getOrDefault(entity.getId(), 0L).intValue())
                    .completed(completed)
                    .hasContent(hasContent)
                    .build());
        }
        if (node.getChildren() != null) {
            for (ChapterTreeVO child : node.getChildren()) {
                enrichNode(child, entityMap, kpCountMap, progressMap);
            }
        }
    }
}
