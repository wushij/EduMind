package com.edumind.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.edumind.knowledge.entity.KnowledgeIndexTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface KnowledgeIndexTaskMapper extends BaseMapper<KnowledgeIndexTaskEntity> {

    /**
     * 把指定任务直接置为终态（SUPERSEDED / INDEX_FAILED 等）。
     *
     * <p>用于「任务被同一知识库更新的任务取代」场景：老任务的异步执行会被守卫提前 return，
     * 若不给它写终态，这条记录将永久停留在 INDEXING，污染大盘进度展示。</p>
     */
    @Update("""
            UPDATE knowledge_index_task
            SET status = #{status}, error_message = #{errorMessage}, finished_at = NOW()
            WHERE id = #{taskId}
            """)
    int updateTerminalStatus(@Param("taskId") Long taskId,
                             @Param("status") String status,
                             @Param("errorMessage") String errorMessage);

    /**
     * 查询「超时仍无进展」的僵死任务所归属的知识库 ID（去重），供启动恢复复位使用。
     *
     * <p>以 update_time 作为「最后一次进展」时间：索引过程中会周期性刷新进度，
     * 处于 INDEXING 且 update_time 长时间不动的行即为中断任务。</p>
     */
    @Select("""
            SELECT DISTINCT knowledge_base_id
            FROM knowledge_index_task
            WHERE status = 'INDEXING' AND COALESCE(update_time, started_at, create_time) < #{before}
            """)
    List<Long> selectStaleIndexingKnowledgeBaseIds(@Param("before") LocalDateTime before);

    /**
     * 批量把「超时仍无进展」的僵死 INDEXING 任务复位为失败。
     */
    @Update("""
            UPDATE knowledge_index_task
            SET status = 'INDEX_FAILED', error_message = #{errorMessage}, finished_at = NOW()
            WHERE status = 'INDEXING' AND COALESCE(update_time, started_at, create_time) < #{before}
            """)
    int failStaleIndexing(@Param("before") LocalDateTime before,
                          @Param("errorMessage") String errorMessage);
}
