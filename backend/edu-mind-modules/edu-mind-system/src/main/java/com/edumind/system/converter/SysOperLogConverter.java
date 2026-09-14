package com.edumind.system.converter;

import com.edumind.common.enums.BusinessType;
import com.edumind.system.entity.SysOperLogEntity;
import com.edumind.system.vo.log.SysOperLogVO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 操作日志对象转换器
 */
@Component
public class SysOperLogConverter {

    public SysOperLogVO toVO(SysOperLogEntity entity) {
        if (entity == null) {
            return null;
        }
        BusinessType type = BusinessType.of(entity.getBusinessType());
        return SysOperLogVO.builder()
                .id(entity.getId())
                .tenantId(entity.getTenantId())
                .title(entity.getTitle())
                .businessType(entity.getBusinessType())
                .businessTypeText(type != null ? type.getDescription() : "其它")
                .method(entity.getMethod())
                .requestMethod(entity.getRequestMethod())
                .operUserId(entity.getOperUserId())
                .operName(entity.getOperName())
                .operUrl(entity.getOperUrl())
                .operIp(entity.getOperIp())
                .operParam(entity.getOperParam())
                .jsonResult(entity.getJsonResult())
                .status(entity.getStatus())
                .errorMsg(entity.getErrorMsg())
                .costTime(entity.getCostTime())
                .operTime(entity.getOperTime())
                .build();
    }

    public List<SysOperLogVO> toVOList(List<SysOperLogEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }
}
