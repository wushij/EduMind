package com.edumind.question.converter;

import com.edumind.question.dto.question.QuestionCreateDTO;
import com.edumind.question.dto.question.QuestionUpdateDTO;
import com.edumind.question.entity.QuestionEntity;
import com.edumind.question.vo.question.QuestionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class QuestionConverter {

    public QuestionEntity toEntity(QuestionCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        QuestionEntity entity = new QuestionEntity();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(1);
        if (entity.getScore() == null) {
            entity.setScore(5);
        }
        return entity;
    }

    public void applyUpdate(QuestionEntity entity, QuestionUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        entity.setBankId(dto.getBankId());
        entity.setCourseId(dto.getCourseId());
        entity.setKnowledgePointId(dto.getKnowledgePointId());
        entity.setStem(dto.getStem());
        entity.setType(dto.getType());
        entity.setOptions(dto.getOptions());
        entity.setAnswer(dto.getAnswer());
        entity.setAnalysis(dto.getAnalysis());
        entity.setDifficulty(dto.getDifficulty());
        entity.setScore(dto.getScore());
        if (dto.getStatus() != null) {
            entity.setStatus(dto.getStatus());
        }
    }

    public QuestionVO toVO(QuestionEntity entity) {
        if (entity == null) {
            return null;
        }
        QuestionVO vo = new QuestionVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
