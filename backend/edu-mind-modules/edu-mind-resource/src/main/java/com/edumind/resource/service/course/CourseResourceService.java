package com.edumind.resource.service.course;

import com.edumind.resource.vo.CourseResourceVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CourseResourceService {

    List<CourseResourceVO> listByCourseId(Long courseId);

    List<CourseResourceVO> listByCourseId(Long courseId, Long chapterId);

    Long addResource(Long courseId, com.edumind.resource.dto.course.CourseResourceCreateDTO dto);

    Long uploadResource(Long courseId, MultipartFile file, String title, String resourceType, Long chapterId,
                        boolean syncToKnowledgeBase);

    void deleteResource(Long resourceId);
}
