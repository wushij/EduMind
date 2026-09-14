package com.edumind.system.converter.security;

import com.edumind.system.entity.security.SecurityKeyVersionEntity;
import com.edumind.system.vo.security.SecurityKeyVersionVO;
import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 国密密钥版本对象转换器
 */
public final class SecurityKeyVersionConverter {

    private SecurityKeyVersionConverter() {
    }

    public static SecurityKeyVersionVO toVO(SecurityKeyVersionEntity entity) {
        if (entity == null) {
            return null;
        }
        SecurityKeyVersionVO vo = new SecurityKeyVersionVO();
        BeanUtils.copyProperties(entity, vo);
        vo.setAlgorithm(formatAlgorithm(entity.getAlgorithm()));
        return vo;
    }

    /**
     * 统一算法展示口径：运行时实现为 SM4-GCM，兼容历史元数据 SM4_CBC
     */
    private static String formatAlgorithm(String algorithm) {
        if (algorithm == null || algorithm.isBlank()
                || "SM4_CBC".equalsIgnoreCase(algorithm)
                || "SM4_GCM".equalsIgnoreCase(algorithm)) {
            return "SM4-GCM";
        }
        return algorithm;
    }

    public static List<SecurityKeyVersionVO> toVOList(List<SecurityKeyVersionEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(SecurityKeyVersionConverter::toVO).collect(Collectors.toList());
    }
}
