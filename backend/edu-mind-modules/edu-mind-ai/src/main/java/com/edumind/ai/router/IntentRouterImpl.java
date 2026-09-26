package com.edumind.ai.router;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class IntentRouterImpl implements IntentRouter {

    @Override
    public String routeIntent(String input) {
        IntentResult res = route(input, null);
        return res.type();
    }

    @Override
    public IntentResult route(String input, Long courseId) {
        if (input == null || input.isBlank()) {
            return new IntentResult("chat", "general", 1.0, Map.of());
        }
        String lower = input.toLowerCase();
        Map<String, Object> slots = new HashMap<>();
        if (courseId != null) {
            slots.put("courseId", courseId);
        }

        // 0. 平台自身能力与使用咨询（如「智教云现在都有哪些 AI 教学能力」）。
        //    必须早于下面的关键词匹配：否则「教学」二字会命中备课关键词，被误判成「备课教学建议」。
        if (isPlatformCapabilityQuery(lower)) {
            return new IntentResult("chat", "platform", 0.92, slots);
        }

        // 1. 题目答疑与解题思路辅导、概念辨析（问怎么解、解题步骤、思路讲解、题目辅导、区别与本质等）
        if (lower.contains("思路") || lower.contains("步骤") || lower.contains("讲解") || lower.contains("辅导")
                || lower.contains("解题") || lower.contains("怎么解") || lower.contains("怎么做") || lower.contains("讲讲")
                || lower.contains("为什么选") || lower.contains("题意") || lower.contains("分析选项")
                || lower.contains("区别") || lower.contains("本质") || lower.contains("概念") || lower.contains("定义")) {
            return new IntentResult("chat", "tutor", 0.95, slots);
        }

        // 2. 组卷与出题 Agent（明确要出试卷、组卷、出题）
        if (lower.contains("组卷") || lower.contains("试卷") || lower.contains("出卷") || lower.contains("出一套")
                || lower.contains("出题") || lower.contains("生成试卷") || lower.contains("生成一套")
                || (lower.contains("生成") && lower.contains("题")) || lower.contains("选题")) {
            return new IntentResult("agent", "exam", 0.95, slots);
        }
        if (lower.contains("批改") || lower.contains("评分") || lower.contains("阅卷")) {
            return new IntentResult("agent", "grading", 0.90, slots);
        }
        // 导航意图只在用户明确表达「要去某个页面/找系统入口」时触发。
        // 严禁将「区别在哪」「错在哪」「问题在哪」等学术辨析或归因问句误判为导航！
        if (isNavigationQuery(lower)) {
            return new IntentResult("navigate", "/analytics", 0.90, slots);
        }
        if (lower.contains("学情") || lower.contains("薄弱") || lower.contains("错题") || lower.contains("掌握度")) {
            return new IntentResult("agent", "learning", 0.91, slots);
        }
        if (lower.contains("检索") || lower.contains("资料") || lower.contains("知识库") || lower.contains("查找") || lower.contains("切片")) {
            return new IntentResult("rag", "kb_retrieval", 0.88, slots);
        }
        // 不要用宽泛的「教学」做关键词：「教学能力」「教学平台」「教学系统」都会被误命中
        if (lower.contains("教案") || lower.contains("备课") || lower.contains("教学设计")
                || lower.contains("课堂设计") || lower.contains("教学建议")) {
            return new IntentResult("agent", "teaching", 0.85, slots);
        }
        return new IntentResult("chat", "general", 0.80, slots);
    }

    /**
     * 是否为「平台自身能力 / 使用方式」咨询。
     *
     * <p>这类问句（如「智教云现在都有哪些 AI 教学能力」）中的「教学」二字会命中备课关键词，
     * 因此必须在关键词匹配之前单独识别，否则会被错判成「备课教学建议」。</p>
     */
    private boolean isPlatformCapabilityQuery(String lower) {
        boolean asksCapability = lower.contains("能力") || lower.contains("功能") || lower.contains("模块")
                || lower.contains("有哪些") || lower.contains("有什么") || lower.contains("能做什么")
                || lower.contains("支持哪些") || lower.contains("支持什么") || lower.contains("怎么用")
                || lower.contains("如何使用") || lower.contains("分别适合");
        if (!asksCapability) {
            return false;
        }
        boolean mentionsPlatform = lower.contains("智教云") || lower.contains("edumind") || lower.contains("平台");
        if (mentionsPlatform) {
            return true;
        }
        // 未提平台名，但明确在问「教学能力 / 平台功能」时同样按平台咨询处理
        return lower.contains("教学能力") || lower.contains("教学功能") || lower.contains("平台功能")
                || lower.contains("ai 能力") || lower.contains("ai能力");
    }

    /**
     * 是否为明确的页面导航 / 系统入口寻址意图。
     *
     * <p>严禁将「区别在哪」「错在哪」「难点在哪」「本质在哪」「差异在哪」等概念辨析或学科归因问句误判为导航！</p>
     */
    private boolean isNavigationQuery(String lower) {
        // 1. 概念辨析、学科推导与归因问句保护：绝不进入导航
        if (lower.contains("区别") || lower.contains("本质") || lower.contains("差异") || lower.contains("不同")
                || lower.contains("错在哪") || lower.contains("差在哪") || lower.contains("问题在哪")
                || lower.contains("难点在哪") || lower.contains("原因在哪") || lower.contains("体现在哪")
                || lower.contains("关键在哪") || lower.contains("怎么解") || lower.contains("极限")
                || lower.contains("函数") || lower.contains("导数") || lower.contains("为什么")) {
            return false;
        }

        // 2. 带有明确报表查看意图
        if (lower.contains("报表")) {
            return true;
        }

        // 3. 明确的系统页面跳转动词
        if (lower.contains("前往") || lower.contains("跳转") || lower.contains("怎么进") || lower.contains("从哪进")
                || lower.contains("从哪进入") || lower.contains("入口")) {
            return true;
        }

        if (lower.contains("导航") && !lower.contains("惯性导航") && !lower.contains("卫星导航")) {
            return true;
        }

        // 4. 包含“在哪”的系统寻址问句（必须明确伴随系统模块、菜单、功能等上下文）
        if (lower.contains("在哪") || lower.contains("哪里")) {
            return lower.contains("页面") || lower.contains("菜单") || lower.contains("功能")
                    || lower.contains("模块") || lower.contains("看板") || lower.contains("设置")
                    || lower.contains("在哪看") || lower.contains("哪里看") || lower.contains("在哪查");
        }

        return false;
    }
}
