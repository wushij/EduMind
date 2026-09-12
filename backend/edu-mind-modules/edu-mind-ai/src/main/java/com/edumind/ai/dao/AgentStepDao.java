package com.edumind.ai.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.entity.AgentStepEntity;
import com.edumind.ai.mapper.AgentStepMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AgentStepDao {

    private final AgentStepMapper agentStepMapper;

    public List<AgentStepEntity> listByRunId(String runId) {
        return agentStepMapper.selectList(new LambdaQueryWrapper<AgentStepEntity>()
                .eq(AgentStepEntity::getRunId, runId)
                .orderByAsc(AgentStepEntity::getStepIndex));
    }

    public int insert(AgentStepEntity entity) {
        return agentStepMapper.insert(entity);
    }

    public int updateById(AgentStepEntity entity) {
        return agentStepMapper.updateById(entity);
    }
}
