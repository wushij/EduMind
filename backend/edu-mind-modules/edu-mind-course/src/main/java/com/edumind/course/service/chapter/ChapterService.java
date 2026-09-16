package com.edumind.course.service.chapter;

import com.edumind.course.vo.chapter.ChapterTreeVO;

import java.util.List;

public interface ChapterService {

    List<ChapterTreeVO> getChapterTree(Long courseId);

    Long createChapter(Long courseId, String title, Long parentId, Integer sortOrder);

    void updateChapter(Long courseId, Long chapterId, String title, Integer sortOrder);

    void deleteChapter(Long courseId, Long chapterId);
}
