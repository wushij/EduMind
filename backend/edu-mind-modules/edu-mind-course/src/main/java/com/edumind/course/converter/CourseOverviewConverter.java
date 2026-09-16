package com.edumind.course.converter;

import com.edumind.course.entity.CourseAnnouncementEntity;
import com.edumind.course.entity.CourseLearningObjectiveEntity;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.overview.CourseAnnouncementVO;
import com.edumind.course.vo.overview.CourseCapabilityTagVO;
import com.edumind.course.vo.overview.CourseObjectiveVO;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class CourseOverviewConverter {

    public CourseObjectiveVO toObjectiveVO(CourseLearningObjectiveEntity entity) {
        if (entity == null) {
            return null;
        }
        return CourseObjectiveVO.builder()
                .id(entity.getId())
                .sortOrder(entity.getSortOrder())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .build();
    }

    public CourseAnnouncementVO toAnnouncementVO(CourseAnnouncementEntity entity) {
        if (entity == null) {
            return null;
        }
        return CourseAnnouncementVO.builder()
                .id(entity.getId())
                .courseId(entity.getCourseId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .pinned(entity.getPinned())
                .status(entity.getStatus())
                .publishTime(entity.getPublishTime())
                .publisherId(entity.getPublisherId())
                .publisherName(entity.getPublisherName())
                .build();
    }

    public List<CourseCapabilityTagVO> buildCapabilityTags(CourseDetailVO course) {
        List<CourseCapabilityTagVO> tags = new ArrayList<>();
        if (course == null) {
            return tags;
        }
        if (course.getKnowledgeBaseId() != null && course.getKnowledgeBaseId() > 0) {
            tags.add(tag("knowledge_base", "知识库已挂载", "kb"));
        }
        if (StringUtils.hasText(course.getAiPersona()) || StringUtils.hasText(course.getWelcomeMessage())) {
            tags.add(tag("ai_assistant", "AI 助教已配置", "ai"));
        }
        if (course.getChapterCount() != null && course.getChapterCount() > 0) {
            tags.add(tag("syllabus", "教学大纲已发布", "primary"));
        }
        if (course.getResourceCount() != null && course.getResourceCount() > 0) {
            tags.add(tag("resources", "课件已入库", "default"));
        }
        if (course.getKnowledgePointCount() != null && course.getKnowledgePointCount() > 0) {
            tags.add(tag("knowledge_points", "知识点已建设", "success"));
        }
        return tags;
    }

    private CourseCapabilityTagVO tag(String code, String label, String tone) {
        return CourseCapabilityTagVO.builder().code(code).label(label).tone(tone).build();
    }
}
