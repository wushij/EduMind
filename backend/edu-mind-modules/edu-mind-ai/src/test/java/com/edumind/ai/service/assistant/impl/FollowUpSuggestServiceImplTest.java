package com.edumind.ai.service.assistant.impl;

import com.edumind.ai.dto.assistant.FollowUpSuggestDTO;
import com.edumind.ai.gateway.AiGatewayFacade;
import com.edumind.ai.service.prompt.PromptService;
import com.edumind.ai.vo.assistant.FollowUpSuggestVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 追问生成测试：重点是「模型输出净化」与「模型失败时不污染聊天主流程」。
 */
@ExtendWith(MockitoExtension.class)
class FollowUpSuggestServiceImplTest {

    @Mock
    private AiGatewayFacade aiGatewayFacade;

    @Mock
    private PromptService promptService;

    @InjectMocks
    private FollowUpSuggestServiceImpl followUpSuggestService;

    @Test
    void suggest_shouldStripNumberingAndKeepContentAnchoredPrompts() {
        when(promptService.renderTemplate(eq("chat_follow_up"), anyMap())).thenReturn("渲染后的提示词");
        when(aiGatewayFacade.chat(anyString(), any(), anyString(), anyString())).thenReturn("""
                1. git worktree 和主分支共享对象库吗？
                2. 删除 worktree 时未提交的改动会丢吗
                3. CI 里同时跑多个 worktree 会冲突吗？
                """);

        FollowUpSuggestVO vo = followUpSuggestService.suggest(request());

        assertTrue(vo.isAiGenerated());
        assertEquals(3, vo.getPrompts().size());
        assertEquals("git worktree 和主分支共享对象库吗？", vo.getPrompts().get(0));
        assertEquals("删除 worktree 时未提交的改动会丢吗", vo.getPrompts().get(1));
    }

    @Test
    void suggest_shouldFallbackSilentlyWhenModelUnavailable() {
        when(promptService.renderTemplate(eq("chat_follow_up"), anyMap())).thenReturn("渲染后的提示词");
        when(aiGatewayFacade.chat(anyString(), any(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("gateway down"));

        FollowUpSuggestVO vo = followUpSuggestService.suggest(request());

        assertFalse(vo.isAiGenerated());
        assertTrue(vo.getPrompts().isEmpty());
    }

    @Test
    void suggest_shouldReturnEmptyWhenTemplateMissing() {
        when(promptService.renderTemplate(eq("chat_follow_up"), anyMap())).thenReturn("");

        FollowUpSuggestVO vo = followUpSuggestService.suggest(request());

        assertFalse(vo.isAiGenerated());
        assertTrue(vo.getPrompts().isEmpty());
    }

    @Test
    void parsePrompts_shouldDropGenericDuplicatedAndOverlongLines() {
        String raw = """
                - 还有什么想了解的？
                - 用 git worktree 时子目录的 ignore 怎么算？
                * 用 git worktree 时子目录的 ignore 怎么算
                2. 用 git worktree 时子目录的 ignore 怎么算？
                这条追问会因为超过四十个字而被丢弃因为它实在是太长了根本不像学生会问出来的问题（真的）
                - git worktree 会共享 .git 目录吗？
                """;

        List<String> prompts = FollowUpSuggestServiceImpl.parsePrompts(
                raw, 5, "git worktree 怎么用");

        assertEquals(2, prompts.size());
        assertEquals("用 git worktree 时子目录的 ignore 怎么算？", prompts.get(0));
        assertEquals("git worktree 会共享 .git 目录吗？", prompts.get(1));
    }

    @Test
    void parsePrompts_shouldSplitMergedSingleLineOutput() {
        // 实测模型会把三条追问挤在同一行（一次只产出约 60 字符），必须拆开而不是整行丢弃
        String raw = "1. 极限唯一是怎么推导出来的？ 2. 收敛必有界有反例吗？ 3. 保号性做题第一步判断什么？";

        List<String> prompts = FollowUpSuggestServiceImpl.parsePrompts(raw, 3, "请讲透 1.1 节极限");

        assertEquals(3, prompts.size());
        assertEquals("极限唯一是怎么推导出来的？", prompts.get(0));
        assertEquals("收敛必有界有反例吗？", prompts.get(1));
        assertEquals("保号性做题第一步判断什么？", prompts.get(2));
    }

    @Test
    void parsePrompts_shouldSplitLongLineWithoutNumberingByQuestionMarks() {
        String raw = "极限唯一怎么推导出来的呢？收敛必有界在做题时有没有反例可以看？保号性判定要先确认什么条件？";

        List<String> prompts = FollowUpSuggestServiceImpl.parsePrompts(raw, 3, "请讲透 1.1 节极限");

        assertEquals(3, prompts.size());
        assertEquals("极限唯一怎么推导出来的呢？", prompts.get(0));
    }

    @Test
    void suggest_shouldRepairWhenFirstReplyIsUnusable() {
        when(promptService.renderTemplate(eq("chat_follow_up"), anyMap())).thenReturn("渲染后的提示词");
        when(aiGatewayFacade.chat(anyString(), any(), anyString(), anyString()))
                .thenReturn("好的，以下是一些建议追问。")
                .thenReturn("""
                        极限唯一为什么成立？
                        收敛必有界做题时怎么用？
                        保号性判定的前提是什么？
                        """);

        FollowUpSuggestVO vo = followUpSuggestService.suggest(request());

        assertTrue(vo.isAiGenerated());
        assertEquals(3, vo.getPrompts().size());
        assertEquals("极限唯一为什么成立？", vo.getPrompts().get(0));
    }

    @Test
    void parsePrompts_shouldKeepSectionNumbersIntact() {
        // 回归：小节号「1.1」曾被当成列表编号剥掉，追问变成「1算法复杂度与渐近表示法…」
        String raw = """
                1.1 算法复杂度与渐近表示法为什么是衡量尺度？
                2. 渐进表示法里最坏情况怎么算？
                """;

        List<String> prompts = FollowUpSuggestServiceImpl.parsePrompts(raw, 2, "算法复杂度");

        assertEquals(2, prompts.size());
        assertEquals("1.1 算法复杂度与渐近表示法为什么是衡量尺度？", prompts.get(0));
        assertEquals("渐进表示法里最坏情况怎么算？", prompts.get(1));
    }

    @Test
    void parsePrompts_shouldDropEchoOfUserQuestion() {
        List<String> prompts = FollowUpSuggestServiceImpl.parsePrompts(
                "git worktree 怎么用？\ngit worktree 怎么用\nworktree 与 clone 有什么差别？", 3,
                "git worktree 怎么用？");

        assertEquals(1, prompts.size());
        assertEquals("worktree 与 clone 有什么差别？", prompts.get(0));
    }

    @Test
    void clipAnswer_shouldKeepHeadAndTailWhenAnswerTooLong() {
        String answer = "头".repeat(4500) + "中".repeat(3000) + "尾".repeat(2500);

        String clipped = FollowUpSuggestServiceImpl.clipAnswer(answer);

        assertTrue(clipped.startsWith("头头头"));
        assertTrue(clipped.endsWith("尾尾尾"));
        assertTrue(clipped.contains("（中间内容已省略）"));
        assertTrue(clipped.length() < answer.length());
    }

    private FollowUpSuggestDTO request() {
        FollowUpSuggestDTO dto = new FollowUpSuggestDTO();
        dto.setQuestion("git worktree 怎么用");
        dto.setAnswer("worktree 让同一个仓库可以同时检出多个分支……");
        return dto;
    }
}
