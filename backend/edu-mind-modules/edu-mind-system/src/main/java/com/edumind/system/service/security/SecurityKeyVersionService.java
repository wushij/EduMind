package com.edumind.system.service.security;

import com.edumind.system.dto.security.SecurityKeyRotateDTO;
import com.edumind.system.vo.security.SecurityKeyVersionVO;

import java.util.List;

/**
 * 国密密钥版本管理服务接口
 */
public interface SecurityKeyVersionService {

    /**
     * 获取指定别名（或当前租户全量）的密钥版本流水列表
     */
    List<SecurityKeyVersionVO> listKeyVersions(String keyAlias);

    /**
     * 获取当前租户有效活跃的密钥版本 (ACTIVE)
     */
    SecurityKeyVersionVO getActiveKey(String keyAlias);

    /**
     * 按 ID 获取密钥版本详情（含 IDOR 校验）
     */
    SecurityKeyVersionVO getKeyVersion(Long id);

    /**
     * 执行密钥版本轮换：将原有 ACTIVE 置为 DEPRECATED，生成版本递增的全新 ACTIVE 密钥
     */
    SecurityKeyVersionVO rotateKey(SecurityKeyRotateDTO dto);
}
