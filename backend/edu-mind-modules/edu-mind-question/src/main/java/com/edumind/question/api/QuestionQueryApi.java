package com.edumind.question.api;

import java.util.List;

/**
 * 题库跨模块查询公开 API
 * 供 Teaching（组卷抽题/作业发题）、AI（出题查重/相似题检索）模块调用
 */
public interface QuestionQueryApi {

    /**
     * 根据题目 ID 获取题目简要信息
     */
    Object getQuestionById(Long questionId);

    /**
     * 根据多个题目 ID 批量获取题目
     */
    List<?> listQuestionsByIds(List<Long> questionIds);

    /**
     * 根据课程 ID 获取题目列表（用于组卷抽题）
     */
    List<?> listQuestionsByCourseId(Long courseId);

    /**
     * 根据课程 ID 和题型获取题目列表
     */
    List<?> listQuestionsByCourseAndTypes(Long courseId, List<String> types, Integer limit);

    /**
     * 根据知识点 ID 抽取题目（用于 AI 自动组卷）
     */
    List<?> listQuestionsByKnowledgePointId(Long knowledgePointId, Integer limit);

    long countQuestions();
}
