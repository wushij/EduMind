package com.edumind.statistics.service.analytics.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.ai.api.AiChatApi;
import com.edumind.common.markdown.LatexTextNormalizer;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.statistics.dto.analytics.TeachingAdviceRequestDTO;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.service.analytics.TeachingAdviceService;
import com.edumind.statistics.service.teaching.TeachingReportService;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import com.edumind.statistics.vo.analytics.TeachingAdviceVO;
import com.edumind.statistics.vo.teaching.TeachingReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeachingAdviceServiceImpl implements TeachingAdviceService {

    private final AiChatApi aiChatApi;
    private final KnowledgeMasteryService knowledgeMasteryService;
    private final CourseQueryApi courseQueryApi;
    private final TeachingReportService teachingReportService;
    private final java.util.Map<String, TeachingAdviceVO> adviceCache = new java.util.concurrent.ConcurrentHashMap<>();

    /** 送入 Prompt 的考点数量上限：过多会稀释模型注意力，过少则覆盖不全 */
    private static final int MAX_FOCUS_TITLES = 5;

    /** 单条 AI 归因摘要送入 Prompt 的字数上限，避免长诊断挤占上下文 */
    private static final int MAX_DIAGNOSIS_CHARS = 160;

    /**
     * 公式书写纪律（追加到 system prompt）。
     *
     * <p>模型默认输出纯文本数学（如 {@code lim(1+a/x)^(bx)=e^(ab)}、{@code sin2x}），
     * 前端 KaTeX 无法识别，建议卡片会退化成一串粗糙的纯文本。与错题变式题生成
     * （{@code WrongQuestionDiagnosisService}）保持同一套源头约束，
     * 读取侧再由 {@link LatexTextNormalizer} 兜底归一化。</p>
     */
    private static final String LATEX_DISCIPLINE =
            "\n【公式书写规范】涉及数学表达式时必须使用 LaTeX 并用 $ 包裹（行内公式如 $\\lim_{x \\to 0}\\frac{\\sin x}{x}$）；"
                    + "禁止使用纯文本或 Unicode 数学符号（如 →、×、∞），请改用 \\to、\\times、\\infty。";

    private String getCacheKey(Long courseId, Long studentId) {
        return (courseId != null ? courseId : 0L) + "_" + (studentId != null ? studentId : "overall");
    }

    @Override
    public TeachingAdviceVO getLatestAdvice(Long courseId, Long studentId) {
        return adviceCache.get(getCacheKey(courseId, studentId));
    }

    @Override
    public void clearAdvice(Long courseId, Long studentId) {
        adviceCache.remove(getCacheKey(courseId, studentId));
    }

    @Override
    public TeachingAdviceVO generateAdvice(TeachingAdviceRequestDTO request) {
        Long courseId = request.getCourseId();
        Long studentId = request.getStudentId();

        // 1. 获取课程与知识点掌握度真实上下文数据
        CourseDetailVO course = courseId != null ? courseQueryApi.getCourseById(courseId) : null;
        String courseName = course != null && StringUtils.hasText(course.getName()) ? course.getName() : "当前课程";

        KnowledgeMasteryVO mastery = knowledgeMasteryService.getMastery(courseId, studentId);

        /*
         * 班级诊断（studentId == null）必须以教师端「核心考点掌握度热力排行榜」为唯一引用来源。
         * 掌握度画像的维度分对无实测记录的学生使用作业均分推算，与榜单只统计真实测评记录的口径不同源，
         * 直接送进 Prompt 会出现「同一页面上榜单 0%、AI 结论 36 分」的自相矛盾。
         */
        List<TeachingReportVO.WeakPointVO> reportWeakPoints = studentId == null
                ? loadReportWeakPoints(courseId)
                : List.of();

        List<String> weakTitles = reportWeakPoints.isEmpty()
                ? collectMasteryWeakTitles(mastery)
                : reportWeakPoints.stream()
                        .map(TeachingAdviceServiceImpl::displayTitle)
                        .filter(StringUtils::hasText)
                        .limit(MAX_FOCUS_TITLES)
                        .collect(Collectors.toList());

        // 聚焦考点必须使用真实知识点 ID：校验失败的 ID 一律丢弃。
        // 历史实现直接把传入 ID 兜底成「考点#1001」，而调用方曾传入题目 ID，
        // 导致 Prompt 里全是无意义编号，模型只能照抄，诊断建议随之失真。
        List<String> focusTitles = resolveFocusTitles(request.getFocusKnowledgePointIds());
        if (!focusTitles.isEmpty()) {
            weakTitles = focusTitles;
        }

        // 2. 组装深度学情诊断 Prompt
        String systemPrompt = buildSystemPrompt(studentId != null);
        String userPrompt = buildUserPrompt(courseName, studentId, mastery, weakTitles,
                reportWeakPoints, focusTitles);

        // 3. 调用真实已接入的 AI 模型 API（如 DeepSeek 等）进行认知推演
        try {
            log.info("[AI Teaching Advice] 正在调用真实 AI 模型 API 生成教学诊断建议，courseId={}, studentId={}, weakCount={}",
                    courseId, studentId, weakTitles.size());
            // 必须带上 courseId：本次诊断是「某门课的学情诊断」，course_id 为空会被课程维度的
            // AI 消耗流水过滤掉，用户点了诊断却在课程视图里看不到这次调用
            String aiReply = aiChatApi.chat("TEACHING_ADVICE", courseId, systemPrompt, userPrompt);
            log.info("[AI Teaching Advice] 真实 AI 接口返回结果：{}", aiReply);

            TeachingAdviceVO vo = parseAdviceResponse(aiReply);
            if (vo != null && StringUtils.hasText(vo.getSummary()) && !vo.getActions().isEmpty()) {
                adviceCache.put(getCacheKey(courseId, studentId), vo);
                return vo;
            }
        } catch (Exception ex) {
            log.warn("[AI Teaching Advice] 调用真实 AI 模型出现异常，启动智能上下文备选方案：{}", ex.getMessage());
        }

        // 4. 容错备选方案 (仅在 AI 外部网络超时等不可抗力异常时兜底保证系统高可用)
        TeachingAdviceVO fallback = buildFallbackAdvice(courseName, studentId, weakTitles);
        adviceCache.put(getCacheKey(courseId, studentId), fallback);
        return fallback;
    }

    /** 掌握度榜单中的薄弱考点标题（真实知识点评测口径，最多 {@link #MAX_FOCUS_TITLES} 条） */
    private static List<String> collectMasteryWeakTitles(KnowledgeMasteryVO mastery) {
        if (mastery == null || mastery.getWeakPoints() == null) {
            return new ArrayList<>();
        }
        return mastery.getWeakPoints().stream()
                .map(KnowledgeMasteryVO.WeakPointVO::getTitle)
                .filter(StringUtils::hasText)
                .limit(MAX_FOCUS_TITLES)
                .collect(Collectors.toList());
    }

    /**
     * 聚焦考点 ID → 考点标题。
     *
     * <p>只接受真实存在的知识点 ID：查不到的 ID 直接跳过并记警告，
     * 全部无效时返回空列表，由调用方回退到掌握度榜单的真实薄弱考点，
     * 避免把「考点#1001」这类占位编号送进 Prompt 让模型照抄。</p>
     */
    private List<String> resolveFocusTitles(List<Long> focusIds) {
        List<String> titles = new ArrayList<>();
        if (focusIds == null || focusIds.isEmpty()) {
            return titles;
        }
        for (Long kpId : focusIds) {
            if (kpId == null) {
                continue;
            }
            KnowledgePointVO kp = courseQueryApi.getKnowledgePointById(kpId);
            if (kp == null || !StringUtils.hasText(kp.getTitle())) {
                log.warn("[AI Teaching Advice] 聚焦考点 ID {} 未匹配到知识点，已忽略（疑似误传题目 ID）", kpId);
                continue;
            }
            if (!titles.contains(kp.getTitle())) {
                titles.add(kp.getTitle());
            }
            if (titles.size() >= MAX_FOCUS_TITLES) {
                break;
            }
        }
        return titles;
    }

    private String buildSystemPrompt(boolean isPersonal) {
        if (isPersonal) {
            return "你是一位拥有丰富教学诊断经验的资深智能学情分析专家。\n" +
                   "你的任务是根据学生各知识维度的真实掌握度、薄弱考点以及班级对比数据，推演出深刻、具体、有针对性的个性化提分诊断结论与行动方案。\n" +
                   "【硬性格式约束】必须输出合法且严谨的纯 JSON，禁止添加 Markdown 代码块（如 ```json），直接以 { 开始，以 } 结束：\n" +
                   "{\n" +
                   "  \"summary\": \"120-220字针对该学员核心卡点、思维认知盲区与提分潜力的深度诊断评语\",\n" +
                   "  \"actions\": [\n" +
                   "    \"具体、可操作的靶向行动项1（如包含关联考点训练建议）\",\n" +
                   "    \"具体、可操作的靶向行动项2\",\n" +
                   "    \"具体、可操作的靶向行动项3\"\n" +
                   "  ]\n" +
                   "}" + LATEX_DISCIPLINE;
        }
        return "你是一位国家级教学名师与大数据教学质量诊断专家。\n" +
               "你的任务是根据全班知识图谱掌握度、知识维度得分与共性薄弱考点，推演生成班级宏观教学诊断策略与切实可行的教学干预方案。\n" +
               "【硬性格式约束】必须输出合法且严谨的纯 JSON，禁止添加 Markdown 代码块（如 ```json），直接以 { 开始，以 } 结束：\n" +
               "{\n" +
               "  \"summary\": \"120-220字针对全班知识达成度、教学进度节奏调优及集中薄弱环节的全局指导性总结\",\n" +
               "  \"actions\": [\n" +
               "    \"切实可行的课堂强化教学行动项1\",\n" +
               "    \"切实可行的课后作业或分层任务行动项2\",\n" +
               "    \"切实可行的互助或自适应干预行动项3\"\n" +
               "  ]\n" +
               "}" + LATEX_DISCIPLINE;
    }

    private String buildUserPrompt(String courseName, Long studentId, KnowledgeMasteryVO mastery,
                                   List<String> weakTitles,
                                   List<TeachingReportVO.WeakPointVO> reportWeakPoints,
                                   List<String> focusTitles) {
        StringBuilder sb = new StringBuilder();
        sb.append("【学情上下文数据】\n");
        sb.append("课程名称：").append(courseName).append("\n");
        if (studentId != null) {
            sb.append("诊断对象：学员编号 #").append(studentId).append("\n");
        } else {
            sb.append("诊断对象：全班学生宏观教学学情\n");
        }

        // 口径与样本量必须显式告知模型：否则它会把 0 分读成"全班完全未掌握"，
        // 也会在 1~2 人的极小样本上下绝对结论
        Integer studentCount = mastery != null ? mastery.getStudentCount() : null;
        sb.append("参与统计学生数：").append(studentCount != null ? studentCount : 0).append(" 人\n");

        if (!reportWeakPoints.isEmpty()) {
            sb.append("\n【核心薄弱考点（与教师端「核心考点掌握度热力排行榜」同一份数据，数值不得改写或推算）】\n");
            for (TeachingReportVO.WeakPointVO weak : orderedByFocus(reportWeakPoints, focusTitles)) {
                sb.append("  - ").append(displayTitle(weak))
                        .append("：").append(describeMastery(weak))
                        .append("；累计答错 ").append(weak.getWrongCount() != null ? weak.getWrongCount() : 0).append(" 人次")
                        .append("；主要错因 ").append(StringUtils.hasText(weak.getErrorTypeName())
                                ? weak.getErrorTypeName() : "待归因")
                        .append("\n");
                String diagnosis = abbreviate(weak.getErrorReason(), MAX_DIAGNOSIS_CHARS);
                if (diagnosis != null) {
                    sb.append("     最近一次 AI 归因摘要：").append(diagnosis).append("\n");
                }
            }
        } else if (weakTitles != null && !weakTitles.isEmpty()) {
            sb.append("重点薄弱考点清单：").append(String.join("、", weakTitles)).append("\n");
        } else {
            sb.append("考点情况：该课程各核心知识模块暂无显著低于 70 分的预警考点，整体处于良性掌握状态。\n");
        }

        if (focusTitles != null && !focusTitles.isEmpty()) {
            sb.append("本次教师重点关注：").append(String.join("、", focusTitles)).append("\n");
        }

        // 维度分含规则推算，必须显式标注来源，避免模型复述成实测成绩
        if (mastery != null && mastery.getDimensions() != null && !mastery.getDimensions().isEmpty()) {
            sb.append("\n【知识维度达成度（含规则推算，非实测成绩，仅可用于趋势参考，禁止作为实测结论引用）】\n");
            for (int i = 0; i < mastery.getDimensions().size(); i++) {
                String dim = mastery.getDimensions().get(i);
                Object pScore = (mastery.getPersonal() != null && i < mastery.getPersonal().size()) ? mastery.getPersonal().get(i) : "-";
                Object cScore = (mastery.getClassAvg() != null && i < mastery.getClassAvg().size()) ? mastery.getClassAvg().get(i) : "-";
                if (studentId != null) {
                    sb.append("  - ").append(dim).append("：个人得分 ").append(pScore).append("分，班级均值 ").append(cScore).append("分\n");
                } else {
                    sb.append("  - ").append(dim).append("：班级平均达成 ").append(cScore).append(" 分（含推算）\n");
                }
            }
        }

        sb.append("\n口径说明：掌握度仅统计真实测评记录，无实测记录的考点不给数值，")
                .append("不得推断为 0 分或满分；覆盖学生数少于 3 人时请避免绝对化结论，建议以课堂观察复核。\n");

        sb.append("\n请综合上述真实学情大数据，给出专业精准的诊断结论与 3-4 条具有实操价值的行动项；")
                .append("行动项必须点名上述具体考点名称，禁止使用「考点#编号」这类占位表述；")
                .append("行动对象只能取自【核心薄弱考点】清单或「本次教师重点关注」，")
                .append("【知识维度达成度】里的维度名仅用于判断整体趋势，不得据此新增薄弱考点或指派任务；")
                .append("不得编造未出现在上下文中的分数、人数、错因或知识点名称。")
                .append("严格按上述 JSON 结构输出。");
        return sb.toString();
    }

    /**
     * 教师端「核心考点掌握度热力排行榜」的薄弱考点快照。
     *
     * <p>AI 建议与教师正在看的榜单必须引用同一份数据。历史实现从掌握度画像
     * （{@code getMastery} 的维度分）取材，而那份数据对无实测记录的学生用作业均分推算，
     * 于是出现「榜单显示 0%、AI 结论却说 36 分」的自相矛盾；报告侧只聚合真实测评记录，
     * 故以它作为唯一引用来源。报告构建失败时返回空列表，由调用方回退到掌握度画像口径。</p>
     */
    private List<TeachingReportVO.WeakPointVO> loadReportWeakPoints(Long courseId) {
        if (courseId == null) {
            return List.of();
        }
        try {
            TeachingReportVO report = teachingReportService.buildReport(courseId, null);
            if (report == null || report.getWeakPoints() == null) {
                return List.of();
            }
            return report.getWeakPoints().stream().filter(Objects::nonNull).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[AI Teaching Advice] 加载教学报告薄弱考点失败，回退掌握度画像口径：{}", e.getMessage());
            return List.of();
        }
    }

    /** 聚焦考点优先展示，其余按原顺序跟随，保证模型先看到教师本次关心的考点 */
    private static List<TeachingReportVO.WeakPointVO> orderedByFocus(List<TeachingReportVO.WeakPointVO> weakPoints,
                                                                     List<String> focusTitles) {
        if (focusTitles == null || focusTitles.isEmpty()) {
            return weakPoints;
        }
        List<TeachingReportVO.WeakPointVO> ordered = new ArrayList<>();
        for (String title : focusTitles) {
            weakPoints.stream()
                    .filter(weak -> title.equals(displayTitle(weak)))
                    .findFirst()
                    .ifPresent(ordered::add);
        }
        weakPoints.stream().filter(weak -> !ordered.contains(weak)).forEach(ordered::add);
        return ordered;
    }

    private static String displayTitle(TeachingReportVO.WeakPointVO weak) {
        if (StringUtils.hasText(weak.getKnowledgePointName())) {
            return weak.getKnowledgePointName();
        }
        return StringUtils.hasText(weak.getTitle()) ? weak.getTitle() : "未关联考点";
    }

    /** 掌握度描述：无实测记录时明确写"暂无"，不得让模型把它读成 0 分 */
    private static String describeMastery(TeachingReportVO.WeakPointVO weak) {
        if (weak.getMasteryRate() == null) {
            return "暂无实测掌握度记录";
        }
        StringBuilder desc = new StringBuilder("实测掌握度 ").append(weak.getMasteryRate()).append("%");
        if (weak.getMasterySampleCount() != null) {
            desc.append("（覆盖 ").append(weak.getMasterySampleCount()).append(" 名学生");
            if (weak.getMasteryAssessmentCount() != null) {
                desc.append("、").append(weak.getMasteryAssessmentCount()).append(" 次测评");
            }
            desc.append("）");
        }
        return desc.toString();
    }

    /** 诊断摘要：折叠空白并截断，超长时明确以省略号收尾 */
    private static String abbreviate(String text, int maxChars) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxChars ? normalized : normalized.substring(0, maxChars) + "…";
    }

    /**
     * 建议正文下发前的数学公式归一化。
     *
     * <p>system prompt 已要求模型用 $ 包裹公式，但模型输出并不稳定：仍可能出现
     * {@code lim(1+a/x)^(bx)=e^(ab)}、{@code sin2x} 这类纯文本数学。
     * 这里与题干/解析/归因结论共用 {@link LatexTextNormalizer}，补全 $ 定界符后再交给前端 KaTeX，
     * 保证「模型偶尔不听话」时教师看到的仍是排版好的公式。</p>
     */
    private static String normalizeMath(String text) {
        return LatexTextNormalizer.wrapBareMath(text);
    }

    private TeachingAdviceVO parseAdviceResponse(String reply) {
        if (!StringUtils.hasText(reply)) {
            return null;
        }
        String cleaned = reply.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }

        try {
            JSONObject json = JSON.parseObject(cleaned);
            if (json != null) {
                TeachingAdviceVO vo = new TeachingAdviceVO();
                vo.setSummary(normalizeMath(json.getString("summary")));
                JSONArray arr = json.getJSONArray("actions");
                if (arr != null) {
                    List<String> actions = new ArrayList<>();
                    for (int i = 0; i < arr.size(); i++) {
                        String a = arr.getString(i);
                        if (StringUtils.hasText(a)) {
                            actions.add(normalizeMath(a.trim()));
                        }
                    }
                    vo.setActions(actions);
                }
                return vo;
            }
        } catch (Exception e) {
            log.debug("[AI Teaching Advice] JSON 解析失败，尝试按行提取: {}", e.getMessage());
        }

        TeachingAdviceVO fallback = new TeachingAdviceVO();
        String[] lines = cleaned.split("\n");
        StringBuilder summarySb = new StringBuilder();
        List<String> actions = new ArrayList<>();
        for (String line : lines) {
            String l = line.trim();
            if (l.matches("^[0-9一二三四1-9][.、\\s].+")) {
                actions.add(l.replaceFirst("^[0-9一二三四1-9][.、\\s]+", ""));
            } else if (!l.startsWith("{") && !l.startsWith("}") && StringUtils.hasText(l)) {
                if (summarySb.length() < 300) {
                    summarySb.append(l).append(" ");
                }
            }
        }
        fallback.setSummary(normalizeMath(summarySb.length() > 0 ? summarySb.toString().trim() : cleaned));
        fallback.setActions(actions.isEmpty()
                ? List.of("根据诊断建议进行针对性专题巩固", "安排课后答疑与错因回溯")
                : actions.stream().map(TeachingAdviceServiceImpl::normalizeMath).collect(Collectors.toList()));
        return fallback;
    }

    private TeachingAdviceVO buildFallbackAdvice(String courseName, Long studentId, List<String> weakTitles) {
        TeachingAdviceVO vo = new TeachingAdviceVO();
        if (studentId != null) {
            if (weakTitles.isEmpty()) {
                vo.setSummary("根据《" + courseName + "》实时学情，该生各知识维度掌握均衡且表现优异。建议拓展压轴变式题型与高阶工程实践，持续向卓越进阶。");
                vo.setActions(List.of(
                        "推送前沿学科拓展与综合性拔高题库",
                        "保持当前在线微课打卡与自主探究节奏",
                        "鼓励参与课堂难题思辨并担任互助学长"
                ));
            } else {
                String weakStr = String.join("、", weakTitles);
                vo.setSummary("诊断显示该生在《" + courseName + "》中对「" + weakStr + "」考点的理解存在阶段性短板，建议针对性查漏补缺。");
                vo.setActions(List.of(
                        "针对「" + weakTitles.get(0) + "」定向推送 5 道自适应变式微练习",
                        "重温关键公式推导与微课精讲视频",
                        "进入 AI 专属答疑模块进行错因追溯与启发式答疑"
                ));
            }
        } else {
            if (weakTitles.isEmpty()) {
                vo.setSummary("当前《" + courseName + "》全班知识达成度良好，各章节知识结构推进平稳。建议适度增加跨考点综合应用大题，提升学生解决复杂问题的能力。");
                vo.setActions(List.of(
                        "组织阶段性拔高综合测验以检验长效留存",
                        "推送学科前沿研读与典型行业案例分析资源",
                        "安排优秀学生经验交流与专题研讨"
                ));
            } else {
                String weakStr = String.join("、", weakTitles);
                vo.setSummary("班级整体学情态势良好，但「" + weakStr + "」考点为全班共性易错薄弱项，达成度显著低于班级均线。");
                vo.setActions(List.of(
                        "下节课堂重点安排 15 分钟集中精讲「" + weakTitles.get(0) + "」难点",
                        "在作业库中批量下发该专题的针对性强化练习题",
                        "通过知识图谱查缺补漏工具建立薄弱生跟踪预警"
                ));
            }
        }
        return vo;
    }
}
