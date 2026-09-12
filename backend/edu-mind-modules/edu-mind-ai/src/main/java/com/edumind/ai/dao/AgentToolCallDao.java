package com.edumind.ai.dao;

import com.edumind.ai.entity.AgentToolCallEntity;
import com.edumind.ai.mapper.AgentToolCallMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AgentToolCallDao {

    private final AgentToolCallMapper agentToolCallMapper;

    public int insert(AgentToolCallEntity entity) {
        return agentToolCallMapper.insert(entity);
    }

    public int updateById(AgentToolCallEntity entity) {
        return agentToolCallMapper.updateById(entity);
    }
}
