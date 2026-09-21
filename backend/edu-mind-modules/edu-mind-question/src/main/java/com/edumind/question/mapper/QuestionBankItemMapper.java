package com.edumind.question.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.question.entity.QuestionBankItemEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionBankItemMapper extends BaseMapper<QuestionBankItemEntity> {

    /**
     * 多行 VALUES 批量插入（单条 SQL，参与当前事务），用于替代循环内逐条 insert。
     * tenant_id 不列出，由租户拦截器按当前上下文补列；create_time 走数据库 DEFAULT CURRENT_TIMESTAMP。
     */
    @Insert("<script>"
            + "INSERT INTO question_bank_item (bank_id, question_id) VALUES "
            + "<foreach collection='list' item='item' separator=','>"
            + "(#{item.bankId}, #{item.questionId})"
            + "</foreach>"
            + "</script>")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertBatch(@Param("list") List<QuestionBankItemEntity> list);
}
