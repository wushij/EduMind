package com.edumind.teaching.api.impl;

import com.edumind.teaching.api.StudentAssignmentQueryApi;
import com.edumind.teaching.service.assignment.AssignmentService;
import com.edumind.teaching.vo.assignment.StudentAssignmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentAssignmentQueryApiImpl implements StudentAssignmentQueryApi {

    private final AssignmentService assignmentService;

    @Override
    public List<StudentAssignmentVO> listMine(Long courseId) {
        return assignmentService.listMine(courseId);
    }
}
