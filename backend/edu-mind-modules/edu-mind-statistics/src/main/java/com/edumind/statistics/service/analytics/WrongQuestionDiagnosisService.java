package com.edumind.statistics.service.analytics;

import com.edumind.ai.api.QuestionGenerateApi;
import com.edumind.ai.dto.QuestionGenerateDTO;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.common.exception.BusinessException;
import com.edumind.question.api.QuestionCommandApi;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.dto.question.QuestionBatchCreateDTO;
import com.edumind.question.dto.question.QuestionCreateDTO;
import com.edumind.question.vo.question.QuestionBatchSaveVO;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.statistics.dao.WrongQuestionRecordDao;
import com.edumind.statistics.entity.WrongQuestionRecordEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WrongQuestionDiagnosisService {

    private final AiGatewayFacade aiGatewayFacade;
    private final WrongQuestionRecordDao wrongQuestionRecordDao;
    private final QuestionQueryApi questionQueryApi;
    private final QuestionGenerateApi questionGenerateApi;
    private final QuestionCommandApi questionCommandApi;

    public String diagnose(String questionStem, String studentAnswer, String correctAnswer) {
        String prompt = "题目：" + questionStem + "\n学生答案：" + studentAnswer + "\n正确答案：" + correctAnswer
                + "\n请用一句话诊断错因，并标注类型 CONCEPT/LOGIC/CALC 之一。";
        try {
            return aiGatewayFacade.chat("GRADING", "你是错题诊断助手。", prompt);
        } catch (Exception ex) {
            return "CONCEPT: 概念理解不完整";
        }
    }

    public void recordWrong(Long studentId, Long courseId, Long questionId, Long knowledgePointId,
                            String diagnosis) {
        WrongQuestionRecordEntity entity = new WrongQuestionRecordEntity();
        entity.setStudentId(studentId);
        entity.setCourseId(courseId);
        entity.setQuestionId(questionId);
        entity.setKnowledgePointId(knowledgePointId);
        entity.setDiagnosis(diagnosis);
        entity.setErrorTypes(extractTypes(diagnosis));
        entity.setWrongCount(1);
        wrongQuestionRecordDao.insert(entity);
    }

    public WrongQuestionRecordEntity diagnoseRecord(Long recordId) {
        WrongQuestionRecordEntity entity = wrongQuestionRecordDao.findById(recordId);
        if (entity == null) {
            throw new BusinessException("错题记录不存在");
        }
        Object raw = questionQueryApi.getQuestionById(entity.getQuestionId());
        if (!(raw instanceof QuestionVO question)) {
            throw new BusinessException("题目不存在");
        }
        String diagnosis = diagnose(question.getStem(), "", question.getAnswer());
        entity.setDiagnosis(diagnosis);
        entity.setErrorTypes(extractTypes(diagnosis));

        QuestionGenerateDTO generateDto = new QuestionGenerateDTO();
        generateDto.setCourseId(entity.getCourseId());
        if (entity.getKnowledgePointId() != null) {
            generateDto.setKnowledgePointIds(List.of(entity.getKnowledgePointId()));
        }
        generateDto.setCount(2);
        generateDto.setDifficulty("MEDIUM");
        generateDto.setQuestionTypes(List.of(
                question.getType() != null ? question.getType() : "SINGLE_CHOICE"));

        List<QuestionVO> generated = questionGenerateApi.generate(generateDto);
        if (!CollectionUtils.isEmpty(generated)) {
            QuestionBatchCreateDTO batchDto = new QuestionBatchCreateDTO();
            batchDto.setCourseId(entity.getCourseId());
            batchDto.setQuestions(generated.stream()
                    .map(q -> toCreateDto(q, entity.getCourseId(), entity.getKnowledgePointId()))
                    .collect(Collectors.toList()));
            QuestionBatchSaveVO saved = questionCommandApi.batchSave(batchDto);
            if (saved.getQuestionIds() != null && !saved.getQuestionIds().isEmpty()) {
                entity.setVariantQuestionIds(saved.getQuestionIds().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")));
            }
        }
        wrongQuestionRecordDao.updateById(entity);
        return entity;
    }

    private QuestionCreateDTO toCreateDto(QuestionVO q, Long courseId, Long knowledgePointId) {
        QuestionCreateDTO dto = new QuestionCreateDTO();
        dto.setCourseId(courseId);
        dto.setKnowledgePointId(knowledgePointId);
        dto.setStem(q.getStem());
        dto.setType(q.getType() != null ? q.getType() : "SINGLE_CHOICE");
        dto.setOptions(q.getOptions());
        dto.setAnswer(q.getAnswer());
        dto.setAnalysis(q.getAnalysis());
        dto.setDifficulty(q.getDifficulty() != null ? q.getDifficulty() : 3);
        dto.setScore(q.getScore() != null ? q.getScore() : 5);
        return dto;
    }

    private String extractTypes(String diagnosis) {
        List<String> types = Arrays.asList("CONCEPT", "LOGIC", "CALC");
        for (String t : types) {
            if (diagnosis != null && diagnosis.contains(t)) {
                return t;
            }
        }
        return "CONCEPT";
    }
}
