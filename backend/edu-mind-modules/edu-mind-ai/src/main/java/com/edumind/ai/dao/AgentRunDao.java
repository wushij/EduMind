package com.edumind.ai.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.entity.AgentRunEntity;
import com.edumind.ai.mapper.AgentRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AgentRunDao {

    private final AgentRunMapper agentRunMapper;

    public AgentRunEntity findByRunId(String runId) {
        return agentRunMapper.selectOne(new LambdaQueryWrapper<AgentRunEntity>()
                .eq(AgentRunEntity::getRunId, runId));
    }

    public List<AgentRunEntity> listByAgentCode(String agentCode) {
        return agentRunMapper.selectList(new LambdaQueryWrapper<AgentRunEntity>()
                .eq(AgentRunEntity::getAgentCode, agentCode));
    }

    public int insert(AgentRunEntity entity) {
        return agentRunMapper.insert(entity);
    }

    public int updateById(AgentRunEntity entity) {
        return agentRunMapper.updateById(entity);
    }

    public long countByAgentCode(String agentCode) {
        return agentRunMapper.selectCount(new LambdaQueryWrapper<AgentRunEntity>()
                .eq(AgentRunEntity::getAgentCode, agentCode));
    }

    public long countSucceededByAgentCode(String agentCode) {
        return agentRunMapper.selectCount(new LambdaQueryWrapper<AgentRunEntity>()
                .eq(AgentRunEntity::getAgentCode, agentCode)
                .eq(AgentRunEntity::getStatus, "SUCCEEDED"));
    }
}
