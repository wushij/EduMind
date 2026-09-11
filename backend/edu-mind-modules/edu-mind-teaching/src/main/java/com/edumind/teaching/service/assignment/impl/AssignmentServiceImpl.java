package com.edumind.teaching.service.assignment.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.PageResult;
import com.edumind.common.exception.BusinessException;
import com.edumind.teaching.converter.ExamConverter;
import com.edumind.teaching.dao.AssignmentDao;
import com.edumind.teaching.dto.assignment.AssignmentCreateDTO;
import com.edumind.teaching.entity.AssignmentEntity;
import com.edumind.teaching.service.assignment.AssignmentService;
import com.edumind.teaching.vo.assignment.AssignmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentDao assignmentDao;
    private final ExamConverter examConverter;

    @Override
    public PageResult<AssignmentVO> pageQuery(Long courseId, String status, Long page, Long pageSize) {
        long pageNum = page != null && page > 0 ? page : 1L;
        long size = pageSize != null && pageSize > 0 ? pageSize : 10L;
        Page<AssignmentEntity> result = assignmentDao.pageQuery(courseId, status, pageNum, size);
        List<AssignmentVO> list = result.getRecords().stream()
                .map(examConverter::toAssignmentVO)
                .collect(Collectors.toList());
        return PageResult.<AssignmentVO>builder()
                .total(result.getTotal())
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .list(list)
                .build();
    }

    @Override
    public AssignmentVO getById(Long id) {
        AssignmentEntity entity = assignmentDao.findById(id);
        if (entity == null) {
            throw new BusinessException("作业不存在");
        }
        return examConverter.toAssignmentVO(entity);
    }

    @Override
    public Long create(AssignmentCreateDTO dto) {
        AssignmentEntity entity = new AssignmentEntity();
        entity.setCourseId(dto.getCourseId());
        entity.setExamId(dto.getExamId());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setDeadline(dto.getDeadline());
        entity.setStatus("DRAFT");
        assignmentDao.insert(entity);
        return entity.getId();
    }

    @Override
    public void publish(Long id) {
        AssignmentEntity entity = assignmentDao.findById(id);
        if (entity == null) {
            throw new BusinessException("作业不存在");
        }
        entity.setStatus("PUBLISHED");
        assignmentDao.updateById(entity);
    }
}
