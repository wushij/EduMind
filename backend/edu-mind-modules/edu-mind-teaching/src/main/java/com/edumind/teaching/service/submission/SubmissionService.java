package com.edumind.teaching.service.submission;

import com.edumind.teaching.dto.submission.SubmissionCreateDTO;
import com.edumind.teaching.vo.submission.SubmissionVO;

import java.util.List;

public interface SubmissionService {

    SubmissionVO submit(Long assignmentId, SubmissionCreateDTO dto);

    SubmissionVO getById(Long id);

    List<SubmissionVO> listByAssignmentId(Long assignmentId);
}
