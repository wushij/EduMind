package com.edumind.question.converter;

import com.edumind.question.dto.bank.QuestionBankCreateDTO;
import com.edumind.question.dto.bank.QuestionBankUpdateDTO;
import com.edumind.question.entity.QuestionBankEntity;
import com.edumind.question.vo.bank.QuestionBankVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class QuestionBankConverter {

    public QuestionBankEntity toEntity(QuestionBankCreateDTO dto) {
        QuestionBankEntity entity = new QuestionBankEntity();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(1);
        entity.setQuestionCount(0);
        return entity;
    }

    public void applyUpdate(QuestionBankEntity entity, QuestionBankUpdateDTO dto) {
        entity.setName(dto.getName());
        entity.setCourseId(dto.getCourseId());
        entity.setDescription(dto.getDescription());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    public QuestionBankVO toVO(QuestionBankEntity entity) {
        QuestionBankVO vo = new QuestionBankVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
