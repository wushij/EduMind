package com.edumind.course.vo.knowledge;

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
public class KnowledgePointVO implements Serializable {

    private Long id;
    private Long courseId;
    private Long chapterId;
    private String title;
    private String code;
    private String description;
    private String cognitiveDimension;
    private Integer importance;
    private String examFocus;
    private Integer sort;
    private List<Long> prerequisiteIds;
    private List<KnowledgePointBriefVO> prerequisites;
}
