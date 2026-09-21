package com.edumind.teaching.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.teaching.entity.SubmissionAnswerEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SubmissionAnswerMapper extends BaseMapper<SubmissionAnswerEntity> {

    /**
     * 多行 VALUES 批量插入（单条 SQL，参与当前事务），用于替代循环内逐条 insert。
     * tenant_id 不列出，由租户拦截器按当前上下文补列；create_time 走数据库 DEFAULT CURRENT_TIMESTAMP。
     */
    @Insert("<script>"
            + "INSERT INTO submission_answer (submission_id, question_id, answer) VALUES "
            + "<foreach collection='list' item='item' separator=','>"
            + "(#{item.submissionId}, #{item.questionId}, #{item.answer})"
            + "</foreach>"
            + "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertBatch(@Param("list") List<SubmissionAnswerEntity> list);
}
