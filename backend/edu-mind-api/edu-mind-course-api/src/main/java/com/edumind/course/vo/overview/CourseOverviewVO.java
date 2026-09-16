package com.edumind.course.vo.overview;

import com.edumind.course.vo.course.CourseDetailVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseOverviewVO implements Serializable {
    private CourseDetailVO course;
    private List<CourseObjectiveVO> objectives;
    private List<CourseAnnouncementVO> announcementsPreview;
    private Long announcementTotal;
    private List<CourseInstructorCardVO> instructors;
    private List<CourseCapabilityTagVO> capabilityTags;
    private Boolean editable;
}
