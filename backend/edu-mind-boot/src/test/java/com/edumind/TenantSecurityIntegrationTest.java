package com.edumind;

import com.edumind.ai.dto.memory.MemoryItemCreateDTO;
import com.edumind.ai.service.memory.AgentMemoryService;
import com.edumind.ai.vo.memory.MemoryNamespaceVO;
import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.system.service.SysOrganizationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * V20-P0 安全加固回归 · 租户 Fail-Closed / Memory Consent
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
public class TenantSecurityIntegrationTest {

    @Autowired
    private SysOrganizationService sysOrganizationService;

    @Autowired
    private AgentMemoryService agentMemoryService;

    @BeforeEach
    void setupUser() {
        UserContext.set(LoginUser.builder()
                .id(9001L)
                .username("ga_security_tester")
                .realName("GA Security Tester")
                .build());
    }

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        UserContext.clear();
    }

    @Test
    @DisplayName("P0-03: 无租户上下文访问组织架构 Fail-Closed")
    void orgApiRejectsMissingTenantContext() {
        TenantContext.clear();
        BusinessException ex = Assertions.assertThrows(BusinessException.class,
                () -> sysOrganizationService.getTree(null));
        Assertions.assertEquals(ResultCode.UNAUTHORIZED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("P0-05: Memory 默认 consent=0，未授权不可写入")
    void memoryDefaultConsentIsClosed() {
        TenantContext.setTenantId(3001L);
        MemoryNamespaceVO ns = agentMemoryService.getNamespace(null);
        Assertions.assertFalse(Boolean.TRUE.equals(ns.getConsentGranted()));
        Assertions.assertFalse(Boolean.TRUE.equals(ns.getConsentStatus()));

        MemoryItemCreateDTO dto = new MemoryItemCreateDTO();
        dto.setSummary("secret preference");
        dto.setMemoryType("PREFERENCE");
        BusinessException ex = Assertions.assertThrows(BusinessException.class,
                () -> agentMemoryService.createMemoryItem(dto));
        Assertions.assertEquals(ResultCode.FORBIDDEN.getCode(), ex.getCode());
    }
}
