package com.edumind.statistics.service.teaching.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 诊断正文 → 教学建议的抽取规则单测。
 *
 * <p>这些断言锁的是「报告建议与错题分析同源」这件事的边界：能抽到就复用 AI 结论，
 * 抽不到必须回落（返回 null 交给模板），绝不能把公式源码或半截句塞进建议卡片。</p>
 */
class DiagnosisAdviceExtractorTest {

    /** 结构参照真实模型输出：偏差本质（含公式）+ 教学诊断与补救 + 失分诱因代码尾标 */
    private static final String FULL_DIAGNOSIS =
            "一、偏差本质标准解的关键是把混合增量恒等拆分为两项，并分别配凑成 \\(x=0\\) 处的导数定义："
                    + "\\[\\frac{f(2x)-f(x)}{x}=2\\cdot \\frac{f(2x)-f(0)}{2x}+\\frac{f(-x)-f(0)}{-x}\\] "
                    + "因此极限应为 \\(2f'(0)+f'(0)=3f'(0)=6\\)。学生选 B，说明其未能完整识别两个增量项都要归约到同一个导数定义，"
                    + "或对负增量配凑符号判断错误。"
                    + " 二、教学诊断与补救根本原因是\u201c增量与分母同构\u201d的导数定义变形训练不足，"
                    + "学生未形成将 \\(f(kx)-f(0)\\)与 \\(kx\\)成对配凑的意识。"
                    + "补救时应专项训练 \\(\\lim_{x\\to0}\\frac{f(ax)-f(bx)}{x}=(a-b)f'(0)\\) 的推导，"
                    + "强调正负增量、系数拆分与极限线性运算，并提醒不可在未确认邻域可导时贸然使用洛必达法则。"
                    + " [类型: CONCEPT]";

    @Test
    void 抽取教学补救小节并剥离公式与类型标记() {
        String advice = DiagnosisAdviceExtractor.extract(FULL_DIAGNOSIS);

        assertNotNull(advice);
        assertTrue(advice.startsWith("根本原因是"), "应从小节正文开头，不带小节标题与序号：" + advice);
        assertTrue(advice.contains("导数定义变形训练不足"));
        assertTrue(advice.contains("强调正负增量、系数拆分与极限线性运算"), "顿号枚举不能被误判成小节边界");
        assertFalse(advice.contains("偏差本质"), "不得把「偏差本质」小节一起带出来");
        assertFalse(advice.contains("\\"), "LaTeX 源码不得进入建议：" + advice);
        assertFalse(advice.contains("$"));
        assertFalse(advice.contains("类型"), "失分诱因机器标记不得进入建议");
        assertFalse(advice.contains("  "), "公式被抹掉后不应残留连续空格");
    }

    @Test
    void 只有诊断没有补救小节时返回空由模板兜底() {
        String onlyDiagnosis = "一、偏差本质标准解的关键是配凑导数定义，学生选 B，说明其对导数定义的结构化理解不清。";

        assertNull(DiagnosisAdviceExtractor.extract(onlyDiagnosis));
    }

    @Test
    void 诊断为空或无小节标题时返回空() {
        assertNull(DiagnosisAdviceExtractor.extract(null));
        assertNull(DiagnosisAdviceExtractor.extract("   "));
        assertNull(DiagnosisAdviceExtractor.extract("学生在本题失分明显，需要加强训练。"));
    }

    @Test
    void 补救小节内容过短时视为抽取失败() {
        assertNull(DiagnosisAdviceExtractor.extract("二、教学诊断与补救：见解析。"));
    }

    @Test
    void 超长建议按整句收口且不出现半截句() {
        String longTail = "补救时应先重建增量与分母同构的结构化意识，再用梯度变式巩固配凑能力。"
                + "同时需要在课堂上反复演示正负增量的拆分过程，让学生亲手完成从定义到结论的完整推导，"
                + "并配合限时训练检验迁移效果，最后通过错因复盘强化符号判断的严谨性。";
        String advice = DiagnosisAdviceExtractor.extract("二、教学诊断与补救" + longTail);

        assertNotNull(advice);
        assertTrue(advice.length() <= 161, "超出上限应被裁剪：" + advice.length());
        assertTrue(advice.endsWith("。") || advice.endsWith("；") || advice.endsWith("，") || advice.endsWith("…"),
                "裁剪后必须以句读收口：" + advice);
    }

    @Test
    void 空白作答的固定结论不会产出建议() {
        String unanswered = "本次作答为空白（未提交任何答案），无法定位概念理解或计算环节的具体偏差，系统不作认知归因。建议先完成作答，再查看归因分析。";

        assertNull(DiagnosisAdviceExtractor.extract(unanswered));
    }
}
