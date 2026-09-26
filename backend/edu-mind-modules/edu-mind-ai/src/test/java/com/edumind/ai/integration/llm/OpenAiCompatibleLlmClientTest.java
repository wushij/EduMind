package com.edumind.ai.integration.llm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

class OpenAiCompatibleLlmClientTest {

    @Test
    void testThirdPartyProxyDefendsSystemPromptInUserMessage() throws Exception {
        LlmProperties proxyProps = new LlmProperties();
        proxyProps.setBaseUrl("http://129.204.52.5:7863/v1");
        proxyProps.setModel("deepseek-v4.1-flash");
        proxyProps.setProvider("deepseek");

        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(proxyProps);
        Method method = OpenAiCompatibleLlmClient.class.getDeclaredMethod("buildMessagePayload", String.class, List.class);
        method.setAccessible(true);

        String systemPrompt = "你是智教云 EduMind 全能教学 AI 助手";
        List<LlmChatMessage> messages = List.of(LlmChatMessage.user("智教云现在都有哪些 AI 教学能力？"));

        @SuppressWarnings("unchecked")
        List<Map<String, String>> payload = (List<Map<String, String>>) method.invoke(client, systemPrompt, messages);

        Assertions.assertEquals(2, payload.size());
        Assertions.assertEquals("system", payload.get(0).get("role"));
        Assertions.assertEquals(systemPrompt, payload.get(0).get("content"));

        Assertions.assertEquals("user", payload.get(1).get("role"));
        String userContent = payload.get(1).get("content");
        Assertions.assertTrue(userContent.contains("【系统规则与指引】\n" + systemPrompt));
        Assertions.assertTrue(userContent.contains("【用户提问】\n智教云现在都有哪些 AI 教学能力？"));
    }

    @Test
    void testOfficialDeepSeekKeepsUserMessageClean() throws Exception {
        LlmProperties officialProps = new LlmProperties();
        officialProps.setBaseUrl("https://api.deepseek.com/v1");
        officialProps.setModel("deepseek-flash");
        officialProps.setProvider("deepseek");

        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(officialProps);
        Method method = OpenAiCompatibleLlmClient.class.getDeclaredMethod("buildMessagePayload", String.class, List.class);
        method.setAccessible(true);

        String systemPrompt = "你是智教云 EduMind 全能教学 AI 助手";
        String userQuestion = "智教云现在都有哪些 AI 教学能力？";
        List<LlmChatMessage> messages = List.of(LlmChatMessage.user(userQuestion));

        @SuppressWarnings("unchecked")
        List<Map<String, String>> payload = (List<Map<String, String>>) method.invoke(client, systemPrompt, messages);

        Assertions.assertEquals(2, payload.size());
        Assertions.assertEquals("system", payload.get(0).get("role"));
        Assertions.assertEquals(systemPrompt, payload.get(0).get("content"));

        Assertions.assertEquals("user", payload.get(1).get("role"));
        Assertions.assertEquals(userQuestion, payload.get(1).get("content"));
    }
}
