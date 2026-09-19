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
public class KnowledgePointBriefVO implements Serializable {

    private Long id;
    private String title;
}
