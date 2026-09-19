package com.edumind.teaching.api;

import com.edumind.teaching.vo.assignment.StudentAssignmentVO;

import java.util.List;

/**
 * 学生作业只读查询（跨模块）
 */
public interface StudentAssignmentQueryApi {

    /**
     * 当前登录学生的作业列表；courseId 为空则返回全部选课课程作业
     */
    List<StudentAssignmentVO> listMine(Long courseId);
}
