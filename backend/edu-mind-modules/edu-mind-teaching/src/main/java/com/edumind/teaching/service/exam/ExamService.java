package com.edumind.teaching.service.exam;

import com.edumind.common.api.PageResult;
import com.edumind.teaching.dto.exam.ExamCreateDTO;
import com.edumind.teaching.dto.exam.ExamUpdateDTO;
import com.edumind.teaching.vo.exam.ExamExportVO;
import com.edumind.teaching.vo.exam.ExamVO;

public interface ExamService {

    PageResult<ExamVO> pageQuery(Long courseId, String keyword, Long page, Long pageSize);

    ExamVO getById(Long id);

    Long create(ExamCreateDTO dto);

    void update(Long id, ExamUpdateDTO dto);

    void delete(Long id);

    ExamExportVO exportExam(Long id);
}
