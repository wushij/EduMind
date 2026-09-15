package com.edumind.course.converter;

import com.edumind.course.dto.course.CourseCreateDTO;
import com.edumind.course.dto.course.CourseUpdateDTO;
import com.edumind.course.entity.ChapterEntity;
import com.edumind.course.entity.CourseEntity;
import com.edumind.course.entity.KnowledgePointEntity;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CourseConverter {

    public void applyUpdate(CourseEntity entity, CourseUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        if (StringUtils.hasText(dto.getName())) {
            entity.setTitle(dto.getName());
        }
        if (dto.getDescription() != null) {
            entity.setDescription(dto.getDescription());
        }
        if (dto.getCoverUrl() != null) {
            entity.setCoverImage(dto.getCoverUrl());
        }
        if (dto.getSemester() != null) {
            entity.setSemester(dto.getSemester());
        }
        if (dto.getCode() != null) {
            entity.setCode(dto.getCode());
        }
        if (dto.getCategory() != null) {
            entity.setCategory(dto.getCategory());
        }
        if (dto.getCredits() != null) {
            entity.setCredits(dto.getCredits());
        }
        if (dto.getPlannedHours() != null) {
            entity.setPlannedHours(dto.getPlannedHours());
        }
        if (dto.getKnowledgeBaseId() != null) {
            entity.setKnowledgeBaseId(dto.getKnowledgeBaseId());
        }
        if (dto.getAiPersona() != null) {
            entity.setAiPersona(dto.getAiPersona());
        }
        if (dto.getWelcomeMessage() != null) {
            entity.setWelcomeMessage(dto.getWelcomeMessage());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            entity.setStatus("ACTIVE".equalsIgnoreCase(dto.getStatus()) ? 1 : 0);
        }
    }

    public CourseEntity toEntity(CourseCreateDTO dto, Long teacherId) {
        if (dto == null) {
            return null;
        }
        CourseEntity entity = new CourseEntity();
        entity.setTitle(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setCoverImage(dto.getCoverUrl());
        entity.setSemester(dto.getSemester());
        entity.setCode(dto.getCode());
        entity.setCategory(dto.getCategory() != null ? dto.getCategory() : "计算机与软件");
        entity.setCredits(dto.getCredits() != null ? dto.getCredits() : java.math.BigDecimal.valueOf(3.0));
        entity.setPlannedHours(dto.getPlannedHours() != null ? dto.getPlannedHours() : 48);
        entity.setKnowledgeBaseId(dto.getKnowledgeBaseId());
        entity.setAiPersona(dto.getAiPersona() != null ? dto.getAiPersona() : "socrates");
        entity.setWelcomeMessage(dto.getWelcomeMessage());
        entity.setTeacherId(teacherId);
        entity.setStatus(1);
        return entity;
    }

    public CourseVO toVO(CourseEntity entity, String teacherName, Long studentCount, Long chapterCount, Long knowledgePointCount, Long resourceCount) {
        if (entity == null) {
            return null;
        }
        return CourseVO.builder()
                .id(entity.getId())
                .name(entity.getTitle())
                .code(entity.getCode())
                .description(entity.getDescription())
                .coverUrl(entity.getCoverImage())
                .teacherId(entity.getTeacherId())
                .teacherName(teacherName)
                .studentCount(studentCount != null ? studentCount : 0L)
                .chapterCount(chapterCount != null ? chapterCount : 0L)
                .knowledgePointCount(knowledgePointCount != null ? knowledgePointCount : 0L)
                .resourceCount(resourceCount != null ? resourceCount : 0L)
                .category(entity.getCategory())
                .credits(entity.getCredits())
                .plannedHours(entity.getPlannedHours())
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                .aiPersona(entity.getAiPersona())
                .welcomeMessage(entity.getWelcomeMessage())
                .status(mapStatus(entity.getStatus()))
                .semester(entity.getSemester())
                .build();
    }

    public CourseVO toVO(CourseEntity entity, String teacherName, Long studentCount) {
        return toVO(entity, teacherName, studentCount, 0L, 0L, 0L);
    }

    public CourseDetailVO toDetailVO(CourseEntity entity, String teacherName, Long studentCount, Long chapterCount, Long knowledgePointCount, Long resourceCount) {
        if (entity == null) {
            return null;
        }
        return CourseDetailVO.builder()
                .id(entity.getId())
                .name(entity.getTitle())
                .code(entity.getCode())
                .description(entity.getDescription())
                .coverUrl(entity.getCoverImage())
                .semester(entity.getSemester())
                .status(mapStatus(entity.getStatus()))
                .teacherId(entity.getTeacherId())
                .teacherName(teacherName)
                .studentCount(studentCount != null ? studentCount : 0L)
                .chapterCount(chapterCount != null ? chapterCount : 0L)
                .knowledgePointCount(knowledgePointCount != null ? knowledgePointCount : 0L)
                .resourceCount(resourceCount != null ? resourceCount : 0L)
                .category(entity.getCategory())
                .credits(entity.getCredits())
                .plannedHours(entity.getPlannedHours())
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                .aiPersona(entity.getAiPersona())
                .welcomeMessage(entity.getWelcomeMessage())
                .createdAt(entity.getCreateTime())
                .updatedAt(entity.getUpdateTime())
                .build();
    }

    public CourseDetailVO toDetailVO(CourseEntity entity, String teacherName, Long studentCount) {
        return toDetailVO(entity, teacherName, studentCount, 0L, 0L, 0L);
    }

    public List<ChapterTreeVO> toChapterTree(List<ChapterEntity> chapters) {
        if (chapters == null || chapters.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, ChapterTreeVO> nodeMap = chapters.stream()
                .collect(Collectors.toMap(ChapterEntity::getId, this::toChapterNode, (a, b) -> a));
        List<ChapterTreeVO> roots = new ArrayList<>();
        for (ChapterTreeVO node : nodeMap.values()) {
            ChapterEntity source = chapters.stream()
                    .filter(item -> item.getId().equals(node.getId()))
                    .findFirst()
                    .orElse(null);
            Long parentId = source != null ? source.getParentId() : 0L;
            if (parentId == null || parentId == 0L || !nodeMap.containsKey(parentId)) {
                roots.add(node);
                continue;
            }
            nodeMap.get(parentId).getChildren().add(node);
        }
        return roots;
    }

    public KnowledgePointVO toKnowledgePointVO(KnowledgePointEntity entity) {
        if (entity == null) {
            return null;
        }
        return KnowledgePointVO.builder()
                .id(entity.getId())
                .courseId(entity.getCourseId())
                .chapterId(entity.getChapterId())
                .title(entity.getTitle())
                .sort(entity.getSortOrder())
                .build();
    }

    public List<KnowledgePointVO> toKnowledgePointVOList(List<KnowledgePointEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toKnowledgePointVO).collect(Collectors.toList());
    }

    private ChapterTreeVO toChapterNode(ChapterEntity entity) {
        return ChapterTreeVO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .sort(entity.getSortOrder())
                .children(new ArrayList<>())
                .build();
    }

    private String mapStatus(Integer status) {
        return status != null && status == 1 ? "ACTIVE" : "INACTIVE";
    }
}
