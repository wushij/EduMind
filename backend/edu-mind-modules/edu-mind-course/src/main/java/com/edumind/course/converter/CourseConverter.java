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
        entity.setTeacherId(teacherId);
        entity.setStatus(1);
        return entity;
    }

    public CourseVO toVO(CourseEntity entity, String teacherName, Long studentCount) {
        if (entity == null) {
            return null;
        }
        return CourseVO.builder()
                .id(entity.getId())
                .name(entity.getTitle())
                .coverUrl(entity.getCoverImage())
                .teacherId(entity.getTeacherId())
                .teacherName(teacherName)
                .studentCount(studentCount)
                .status(mapStatus(entity.getStatus()))
                .semester(entity.getSemester())
                .build();
    }

    public CourseDetailVO toDetailVO(CourseEntity entity, String teacherName, Long studentCount) {
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
                .studentCount(studentCount)
                .createdAt(entity.getCreateTime())
                .updatedAt(entity.getUpdateTime())
                .build();
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
