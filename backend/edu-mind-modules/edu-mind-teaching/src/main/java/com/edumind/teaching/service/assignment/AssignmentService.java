package com.edumind.teaching.service.assignment;

import com.edumind.common.api.PageResult;
import com.edumind.teaching.dto.assignment.AssignmentCreateDTO;
import com.edumind.teaching.vo.assignment.AssignmentVO;

public interface AssignmentService {

    PageResult<AssignmentVO> pageQuery(Long courseId, String status, Long page, Long pageSize);

    AssignmentVO getById(Long id);

    Long create(AssignmentCreateDTO dto);

    void publish(Long id);

    void delete(Long id);
}
