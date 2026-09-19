package com.edumind.teaching.service.assignment;

import com.edumind.common.api.PageResult;
import com.edumind.teaching.dto.assignment.AssignmentCreateDTO;
import com.edumind.teaching.vo.assignment.AssignmentPaperVO;
import com.edumind.teaching.vo.assignment.AssignmentStatsVO;
import com.edumind.teaching.vo.assignment.AssignmentVO;
import com.edumind.teaching.vo.assignment.StudentAssignmentVO;

import java.util.List;

public interface AssignmentService {

    PageResult<AssignmentVO> pageQuery(Long courseId, String status, String keyword, Long page, Long pageSize);

    AssignmentStatsVO getStats(Long courseId);

    AssignmentVO getById(Long id);

    Long create(AssignmentCreateDTO dto);

    void publish(Long id);

    void close(Long id);

    void delete(Long id);

    List<StudentAssignmentVO> listMine(Long courseId);

    AssignmentPaperVO getPaper(Long id);

    int remindUnsubmitted(Long id);
}
