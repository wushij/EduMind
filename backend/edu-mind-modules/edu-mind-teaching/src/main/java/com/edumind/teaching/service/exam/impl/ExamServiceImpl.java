package com.edumind.teaching.service.exam.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.PageResult;
import com.edumind.common.exception.BusinessException;
import com.edumind.teaching.converter.ExamConverter;
import com.edumind.teaching.dao.ExamDao;
import com.edumind.teaching.dao.ExamQuestionDao;
import com.edumind.teaching.dto.exam.ExamCreateDTO;
import com.edumind.teaching.dto.exam.ExamQuestionItemDTO;
import com.edumind.teaching.dto.exam.ExamUpdateDTO;
import com.edumind.teaching.entity.ExamEntity;
import com.edumind.teaching.entity.ExamQuestionEntity;
import com.edumind.teaching.service.exam.ExamService;
import com.edumind.teaching.vo.exam.ExamExportVO;
import com.edumind.teaching.vo.exam.ExamVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamDao examDao;
    private final ExamQuestionDao examQuestionDao;
    private final ExamConverter examConverter;

    @Override
    public PageResult<ExamVO> pageQuery(Long courseId, String keyword, Long page, Long pageSize) {
        long pageNum = page != null && page > 0 ? page : 1L;
        long size = pageSize != null && pageSize > 0 ? pageSize : 10L;
        Page<ExamEntity> result = examDao.pageQuery(courseId, keyword, pageNum, size);
        List<ExamVO> list = result.getRecords().stream()
                .map(entity -> examConverter.toVO(entity, examQuestionDao.listByExamId(entity.getId())))
                .collect(Collectors.toList());
        return PageResult.<ExamVO>builder()
                .total(result.getTotal())
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .list(list)
                .build();
    }

    @Override
    public ExamVO getById(Long id) {
        ExamEntity entity = examDao.findById(id);
        if (entity == null) {
            throw new BusinessException("试卷不存在");
        }
        return examConverter.toVO(entity, examQuestionDao.listByExamId(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ExamCreateDTO dto) {
        ExamEntity entity = examConverter.toEntity(dto);
        examDao.insert(entity);
        saveExamQuestions(entity.getId(), dto.getQuestions());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ExamUpdateDTO dto) {
        ExamEntity entity = examDao.findById(id);
        if (entity == null) {
            throw new BusinessException("试卷不存在");
        }
        if (dto.getTitle() != null) {
            entity.setTitle(dto.getTitle());
        }
        if (dto.getTotalScore() != null) {
            entity.setTotalScore(dto.getTotalScore());
        }
        if (dto.getPassScore() != null) {
            entity.setPassScore(dto.getPassScore());
        }
        if (dto.getDurationMinutes() != null) {
            entity.setDurationMinutes(dto.getDurationMinutes());
        }
        if (dto.getStartTime() != null) {
            entity.setStartTime(dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            entity.setEndTime(dto.getEndTime());
        }
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
        examDao.updateById(entity);
        if (dto.getQuestions() != null) {
            examQuestionDao.deleteByExamId(id);
            saveExamQuestions(id, dto.getQuestions());
        }
    }

    @Override
    public void delete(Long id) {
        ExamEntity entity = examDao.findById(id);
        if (entity == null) {
            throw new BusinessException("试卷不存在");
        }
        examDao.softDeleteById(id);
    }

    @Override
    public ExamExportVO exportExam(Long id) {
        ExamVO exam = getById(id);
        ExamExportVO exportVO = new ExamExportVO();
        exportVO.setExportFormat("JSON");
        exportVO.setExam(exam);
        return exportVO;
    }

    private void saveExamQuestions(Long examId, List<ExamQuestionItemDTO> questions) {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        // exam_question 对 (exam_id, question_id) 建有唯一键，同一题被重复加入时若不剔除，
        // 整份试卷会因 DuplicateKeyException 回滚（对外表现为 400「数据已存在，请勿重复提交」）。
        int sort = 1;
        Set<Long> usedQuestionIds = new HashSet<>(questions.size());
        List<ExamQuestionEntity> entities = new ArrayList<>(questions.size());
        for (ExamQuestionItemDTO item : questions) {
            if (item.getQuestionId() == null || !usedQuestionIds.add(item.getQuestionId())) {
                continue;
            }
            ExamQuestionEntity entity = new ExamQuestionEntity();
            entity.setExamId(examId);
            entity.setQuestionId(item.getQuestionId());
            entity.setScore(item.getScore());
            entity.setSortOrder(item.getSortOrder() != null ? item.getSortOrder() : sort++);
            entities.add(entity);
        }
        examQuestionDao.insertBatch(entities);
    }
}
