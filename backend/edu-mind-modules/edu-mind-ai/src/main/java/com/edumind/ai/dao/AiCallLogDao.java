package com.edumind.ai.dao;

import com.edumind.ai.entity.AiCallLogEntity;
import com.edumind.ai.mapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AiCallLogDao {

    private final AiCallLogMapper aiCallLogMapper;

    public int insert(AiCallLogEntity entity) {
        return aiCallLogMapper.insert(entity);
    }
}
