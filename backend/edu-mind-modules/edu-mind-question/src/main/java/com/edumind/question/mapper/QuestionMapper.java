package com.edumind.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.question.entity.QuestionEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionMapper extends BaseMapper<QuestionEntity> {

    /**
     * 多行 VALUES 批量插入（单条 SQL，参与当前事务），用于替代循环内逐条 insert。
     * <p>
     * 说明：
     * 1. id 为雪花 ID（IdType.ASSIGN_ID），必须在实体中已赋值，故显式列出；
     * 2. tenant_id 显式列出（为空时回退列默认值 1），与 {@code QuestionDao#insert} 的租户填充语义一致；
     * 3. difficulty / score / status / deleted 为带默认值的列，插入前做空值兜底；
     * 4. create_time / update_time 走数据库默认值，与单条 insert 行为一致。
     */
    @Insert("<script>"
            + "INSERT INTO edu_question (tenant_id, id, bank_id, course_id, knowledge_point_id, stem, type, options, "
            + "answer, analysis, difficulty, score, status, deleted) VALUES "
            + "<foreach collection='list' item='item' separator=','>"
            + "(IFNULL(#{item.tenantId}, 1), #{item.id}, #{item.bankId}, #{item.courseId}, #{item.knowledgePointId}, "
            + "#{item.stem}, #{item.type}, #{item.options}, #{item.answer}, #{item.analysis}, "
            + "IFNULL(#{item.difficulty}, 3), IFNULL(#{item.score}, 5), IFNULL(#{item.status}, 1), IFNULL(#{item.deleted}, 0))"
            + "</foreach>"
            + "</script>")
    int insertBatch(@Param("list") List<QuestionEntity> list);
}
