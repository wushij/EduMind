package com.edumind.teaching.converter;

import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.teaching.dto.exam.ExamCreateDTO;
import com.edumind.teaching.entity.AssignmentEntity;
import com.edumind.teaching.entity.ExamEntity;
import com.edumind.teaching.entity.ExamQuestionEntity;
import com.edumind.teaching.vo.assignment.AssignmentVO;
import com.edumind.teaching.vo.exam.ExamQuestionVO;
import com.edumind.teaching.vo.exam.ExamVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExamConverter {

    private final QuestionQueryApi questionQueryApi;

    public ExamEntity toEntity(ExamCreateDTO dto) {
        ExamEntity entity = new ExamEntity();
        BeanUtils.copyProperties(dto, entity);
        entity.setStatus(1);
        if (entity.getPassScore() == null) {
            entity.setPassScore(60);
        }
        return entity;
    }

    public ExamVO toVO(ExamEntity entity, List<ExamQuestionEntity> examQuestions) {
        ExamVO vo = new ExamVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setQuestions(toExamQuestionVOList(examQuestions));
        return vo;
    }

    public List<ExamQuestionVO> toExamQuestionVOList(List<ExamQuestionEntity> examQuestions) {
        if (examQuestions == null || examQuestions.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> questionIds = examQuestions.stream()
                .map(ExamQuestionEntity::getQuestionId)
                .collect(Collectors.toList());
        List<QuestionVO> rawQuestions = questionQueryApi.listQuestionsByIds(questionIds);
        Map<Long, QuestionVO> questionMap = rawQuestions.stream()
                .collect(Collectors.toMap(QuestionVO::getId, q -> q, (a, b) -> a));

        List<ExamQuestionVO> result = new ArrayList<>();
        for (ExamQuestionEntity item : examQuestions) {
            ExamQuestionVO vo = new ExamQuestionVO();
            vo.setQuestionId(item.getQuestionId());
            vo.setScore(item.getScore());
            vo.setSortOrder(item.getSortOrder());
            vo.setQuestion(questionMap.get(item.getQuestionId()));
            result.add(vo);
        }
        return result;
    }

    public AssignmentVO toAssignmentVO(AssignmentEntity entity) {
        AssignmentVO vo = new AssignmentVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
