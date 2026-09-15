package com.edumind;

import com.edumind.ai.integration.llm.LlmClient;
import com.edumind.ai.integration.llm.LlmClientRegistry;
import com.edumind.ai.integration.llm.MockLlmClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 可选 GA LLM 真引擎冒烟：mvn test -Dga.llm.integration=true -Dtest=GaLlmIntegrationSmokeTest
 */
@SpringBootTest(classes = EduMindApplication.class)
@ActiveProfiles("test")
@EnabledIfSystemProperty(named = "ga.llm.integration", matches = "true")
public class GaLlmIntegrationSmokeTest {

    @Autowired
    private LlmClientRegistry llmClientRegistry;

    @Test
    @DisplayName("GA LLM 集成冒烟：非 Mock 客户端可实例化")
    void realLlmClientNotMock() {
        LlmClient client = llmClientRegistry.get("deepseek-chat");
        Assertions.assertNotNull(client);
        Assertions.assertFalse(client instanceof MockLlmClient, "ga.llm.integration 模式禁止 Mock 客户端");
    }
}
