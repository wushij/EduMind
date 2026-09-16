package com.edumind.course.vo.knowledge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePointVO implements Serializable {

    private Long id;
    private Long courseId;
    private Long chapterId;
    private String title;
    private Integer sort;
}
