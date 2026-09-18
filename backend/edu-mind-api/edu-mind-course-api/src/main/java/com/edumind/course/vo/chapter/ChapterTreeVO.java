package com.edumind.course.vo.chapter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterTreeVO implements Serializable {

    private Long id;
    private String title;
    private String description;
    private Integer sort;
    private Long parentId;
    private LessonMetaVO lessonMeta;

    @Builder.Default
    private List<ChapterTreeVO> children = new ArrayList<>();
}
