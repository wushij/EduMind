package com.edumind.teaching.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.teaching.entity.ExamQuestionEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExamQuestionMapper extends BaseMapper<ExamQuestionEntity> {

    /**
     * 多行 VALUES 批量插入（单条 SQL，参与当前事务），用于替代循环内逐条 insert。
     * tenant_id 不列出，由租户拦截器按当前上下文补列；
     * score / sort_order 为带默认值的列，插入前做空值兜底，保持与单条 insert 相同的落库结果。
     */
    @Insert("<script>"
            + "INSERT INTO exam_question (exam_id, question_id, score, sort_order) VALUES "
            + "<foreach collection='list' item='item' separator=','>"
            + "(#{item.examId}, #{item.questionId}, IFNULL(#{item.score}, 5), IFNULL(#{item.sortOrder}, 0))"
            + "</foreach>"
            + "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertBatch(@Param("list") List<ExamQuestionEntity> list);
}
