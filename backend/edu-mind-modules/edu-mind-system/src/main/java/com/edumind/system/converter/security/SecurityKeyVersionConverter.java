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
        vo.setCreateTime(entity.getActivatedTime());
        vo.setUsageScope(resolveUsageScope(entity.getKeyAlias()));
        vo.setKeyFingerprint(generateFingerprint(entity.getTenantId(), entity.getKeyAlias(), entity.getKeyVersion()));
        return vo;
    }

    private static String resolveUsageScope(String keyAlias) {
        if (keyAlias != null && keyAlias.toLowerCase().contains("model")) {
            return "AI 大模型中枢凭据 (DeepSeek / OpenAI / Qwen API-Key)";
        }
        return "AI 长期记忆沙箱与高敏学情档案 (学生错题 / 认知画像 / 对话切片)";
    }

    private static String generateFingerprint(Long tenantId, String keyAlias, Integer version) {
        try {
            String raw = (tenantId != null ? tenantId : 1) + ":" + (keyAlias != null ? keyAlias : "key") + ":v" + (version != null ? version : 1);
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("SM4-GCM#");
            for (int i = 0; i < 6; i++) {
                sb.append(String.format("%02X", digest[i]));
                if (i == 1 || i == 3) sb.append("-");
            }
            return sb.toString();
        } catch (Exception e) {
            return "SM4-GCM#7F8A-3C2B-91E4";
        }
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

