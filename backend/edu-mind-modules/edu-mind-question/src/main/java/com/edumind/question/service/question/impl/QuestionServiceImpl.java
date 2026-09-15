package com.edumind.question.service.question.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.PageResult;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.converter.QuestionConverter;
import com.edumind.question.dao.QuestionDao;
import com.edumind.question.dto.question.QuestionBatchCreateDTO;
import com.edumind.question.dto.question.QuestionCreateDTO;
import com.edumind.question.dto.question.QuestionQueryDTO;
import com.edumind.question.dto.question.QuestionUpdateDTO;
import com.edumind.question.entity.QuestionEntity;
import com.edumind.question.service.question.QuestionService;
import com.edumind.question.vo.question.QuestionBatchSaveVO;
import com.edumind.question.vo.question.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionDao questionDao;
    private final QuestionConverter questionConverter;
    private final QuestionQueryApi questionQueryApi;

    @Override
    public Long createQuestion(QuestionCreateDTO dto) {
        QuestionEntity entity = questionConverter.toEntity(dto);
        if (entity.getTenantId() == null) {
            entity.setTenantId(TenantContext.requireTenantId());
        }
        questionDao.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuestionBatchSaveVO batchSave(QuestionBatchCreateDTO dto) {
        List<Long> questionIds = new ArrayList<>();
        Long currentTenantId = TenantContext.requireTenantId();
        for (QuestionCreateDTO item : dto.getQuestions()) {
            if (item.getCourseId() == null && dto.getCourseId() != null) {
                item.setCourseId(dto.getCourseId());
            }
            QuestionEntity entity = questionConverter.toEntity(item);
            if (entity.getTenantId() == null) {
                entity.setTenantId(currentTenantId);
            }
            questionDao.insert(entity);
            questionIds.add(entity.getId());
        }
        return QuestionBatchSaveVO.builder()
                .savedCount(questionIds.size())
                .questionIds(questionIds)
                .build();
    }

    @Override
    public QuestionVO getQuestionById(Long id) {
        return questionQueryApi.getQuestionById(id);
    }

    @Override
    public PageResult<QuestionVO> pageQuery(QuestionQueryDTO query) {
        Page<QuestionEntity> page = questionDao.pageQuery(query);
        List<QuestionVO> list = page.getRecords().stream()
                .map(questionConverter::toVO)
                .collect(Collectors.toList());
        return PageResult.<QuestionVO>builder()
                .total(page.getTotal())
                .pageNum(page.getCurrent())
                .pageSize(page.getSize())
                .list(list)
                .build();
    }

    @Override
    public void updateQuestion(Long id, QuestionUpdateDTO dto) {
        QuestionEntity entity = questionDao.getById(id);
        if (entity == null) {
            throw new BusinessException("题目不存在");
        }
        questionConverter.applyUpdate(entity, dto);
        questionDao.updateById(entity);
    }

    @Override
    public void deleteQuestion(Long id) {
        QuestionEntity entity = questionDao.getById(id);
        if (entity == null) {
            throw new BusinessException("题目不存在");
        }
        questionDao.softDeleteById(id);
    }

    @Override
    public List<QuestionVO> listQuestionsByIds(List<Long> ids) {
        return questionQueryApi.listQuestionsByIds(ids);
    }

    @Override
    public List<QuestionVO> listQuestionsByKnowledgePointId(Long pointId, Integer limit) {
        return questionQueryApi.listQuestionsByKnowledgePointId(pointId, limit);
    }

    @Override
    public List<QuestionVO> listQuestionsByCourseId(Long courseId) {
        return questionQueryApi.listQuestionsByCourseId(courseId);
    }

    @Override
    public List<QuestionVO> listQuestionsByCourseAndTypes(Long courseId, List<String> types, Integer limit) {
        return questionQueryApi.listQuestionsByCourseAndTypes(courseId, types, limit);
    }
}
