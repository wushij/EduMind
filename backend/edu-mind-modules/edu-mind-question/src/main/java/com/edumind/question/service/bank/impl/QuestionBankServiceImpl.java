package com.edumind.question.service.bank.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.api.PageResult;
import com.edumind.common.exception.BusinessException;
import com.edumind.question.converter.QuestionBankConverter;
import com.edumind.question.dao.QuestionBankDao;
import com.edumind.question.dao.QuestionBankItemDao;
import com.edumind.question.dao.QuestionDao;
import com.edumind.question.dto.bank.QuestionBankAddQuestionsDTO;
import com.edumind.question.dto.bank.QuestionBankCreateDTO;
import com.edumind.question.dto.bank.QuestionBankUpdateDTO;
import com.edumind.question.entity.QuestionBankEntity;
import com.edumind.question.entity.QuestionBankItemEntity;
import com.edumind.question.entity.QuestionEntity;
import com.edumind.question.service.bank.QuestionBankService;
import com.edumind.question.converter.QuestionConverter;
import com.edumind.question.vo.bank.QuestionBankVO;
import com.edumind.question.vo.question.QuestionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionBankServiceImpl implements QuestionBankService {

    private final QuestionBankDao questionBankDao;
    private final QuestionBankItemDao questionBankItemDao;
    private final QuestionDao questionDao;
    private final QuestionBankConverter questionBankConverter;
    private final QuestionConverter questionConverter;

    @Override
    public PageResult<QuestionBankVO> pageQuery(Long courseId, String keyword, Long page, Long pageSize) {
        long pageNum = page != null && page > 0 ? page : 1L;
        long size = pageSize != null && pageSize > 0 ? pageSize : 10L;
        Page<QuestionBankEntity> result = questionBankDao.pageQuery(courseId, keyword, pageNum, size);
        List<QuestionBankVO> list = result.getRecords().stream()
                .map(questionBankConverter::toVO)
                .collect(Collectors.toList());
        return PageResult.<QuestionBankVO>builder()
                .total(result.getTotal())
                .pageNum(result.getCurrent())
                .pageSize(result.getSize())
                .list(list)
                .build();
    }

    @Override
    public QuestionBankVO getById(Long id, boolean includeQuestions) {
        QuestionBankEntity entity = questionBankDao.getById(id);
        if (entity == null) {
            throw new BusinessException("题库不存在");
        }
        QuestionBankVO vo = questionBankConverter.toVO(entity);
        if (includeQuestions) {
            List<Long> questionIds = questionBankItemDao.listQuestionIdsByBankId(id);
            List<QuestionVO> questions = questionDao.listByIds(questionIds).stream()
                    .map(questionConverter::toVO)
                    .collect(Collectors.toList());
            vo.setQuestions(questions);
        }
        return vo;
    }

    @Override
    public Long create(QuestionBankCreateDTO dto) {
        QuestionBankEntity entity = questionBankConverter.toEntity(dto);
        questionBankDao.insert(entity);
        return entity.getId();
    }

    @Override
    public void update(Long id, QuestionBankUpdateDTO dto) {
        QuestionBankEntity entity = questionBankDao.getById(id);
        if (entity == null) {
            throw new BusinessException("题库不存在");
        }
        questionBankConverter.applyUpdate(entity, dto);
        questionBankDao.updateById(entity);
    }

    @Override
    public void delete(Long id) {
        QuestionBankEntity entity = questionBankDao.getById(id);
        if (entity == null) {
            throw new BusinessException("题库不存在");
        }
        questionBankDao.softDeleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addQuestions(Long id, QuestionBankAddQuestionsDTO dto) {
        QuestionBankEntity bank = questionBankDao.getById(id);
        if (bank == null) {
            throw new BusinessException("题库不存在");
        }
        List<Long> requestedQuestionIds = dto.getQuestionIds() == null
                ? List.of()
                : dto.getQuestionIds().stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
        if (requestedQuestionIds.isEmpty()) {
            refreshQuestionCount(id);
            return;
        }
        // 1) 一次批量校验题目存在性（替代循环内逐个 getById）
        Set<Long> existingQuestionIds = questionDao.listByIds(requestedQuestionIds).stream()
                .map(QuestionEntity::getId)
                .collect(Collectors.toSet());
        for (Long questionId : requestedQuestionIds) {
            if (!existingQuestionIds.contains(questionId)) {
                throw new BusinessException("题目不存在: " + questionId);
            }
        }
        // 2) 一次取出题库已有题目做内存去重（替代循环内逐个 exists 计数查询）
        Set<Long> alreadyInBankQuestionIds = new HashSet<>(questionBankItemDao.listQuestionIdsByBankId(id));
        List<QuestionBankItemEntity> items = new ArrayList<>();
        for (Long questionId : requestedQuestionIds) {
            if (alreadyInBankQuestionIds.contains(questionId)) {
                continue;
            }
            QuestionBankItemEntity item = new QuestionBankItemEntity();
            item.setBankId(id);
            item.setQuestionId(questionId);
            items.add(item);
        }
        // 3) 单条 SQL 批量入库（替代循环内逐条 insert）
        questionBankItemDao.insertBatch(items);
        refreshQuestionCount(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeQuestion(Long bankId, Long questionId) {
        QuestionBankEntity bank = questionBankDao.getById(bankId);
        if (bank == null) {
            throw new BusinessException("题库不存在");
        }
        questionBankItemDao.deleteByBankIdAndQuestionId(bankId, questionId);
        refreshQuestionCount(bankId);
    }

    private void refreshQuestionCount(Long bankId) {
        QuestionBankEntity bank = questionBankDao.getById(bankId);
        if (bank != null) {
            bank.setQuestionCount(questionBankItemDao.countByBankId(bankId));
            questionBankDao.updateById(bank);
        }
    }
}
