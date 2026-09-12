package com.edumind.ai.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumind.ai.entity.AiGatewayRouteEntity;
import com.edumind.ai.mapper.AiGatewayRouteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AiGatewayRouteDao {

    private final AiGatewayRouteMapper aiGatewayRouteMapper;

    public AiGatewayRouteEntity findByScene(String scene) {
        return aiGatewayRouteMapper.selectOne(new LambdaQueryWrapper<AiGatewayRouteEntity>()
                .eq(AiGatewayRouteEntity::getScene, scene));
    }

    public List<AiGatewayRouteEntity> listAll() {
        return aiGatewayRouteMapper.selectList(new LambdaQueryWrapper<>());
    }

    public int updateById(AiGatewayRouteEntity entity) {
        return aiGatewayRouteMapper.updateById(entity);
    }
}
