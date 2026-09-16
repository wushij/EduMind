package com.edumind.statistics.service.analytics.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.ai.api.AiChatApi;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.statistics.dto.analytics.TeachingAdviceRequestDTO;
import com.edumind.statistics.service.analytics.KnowledgeMasteryService;
import com.edumind.statistics.service.analytics.TeachingAdviceService;
import com.edumind.statistics.vo.analytics.KnowledgeMasteryVO;
import com.edumind.statistics.vo.analytics.TeachingAdviceVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeachingAdviceServiceImpl implements TeachingAdviceService {

    private final AiChatApi aiChatApi;
    private final KnowledgeMasteryService knowledgeMasteryService;
    private final CourseQueryApi courseQueryApi;
    private final java.util.Map<String, TeachingAdviceVO> adviceCache = new java.util.concurrent.ConcurrentHashMap<>();

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
        List<String> weakTitles = new ArrayList<>();
        if (mastery != null && mastery.getWeakPoints() != null) {
            weakTitles = mastery.getWeakPoints().stream()
                    .map(KnowledgeMasteryVO.WeakPointVO::getTitle)
                    .filter(StringUtils::hasText)
                    .limit(5)
                    .collect(Collectors.toList());
        }
        if (request.getFocusKnowledgePointIds() != null && !request.getFocusKnowledgePointIds().isEmpty()) {
            weakTitles = request.getFocusKnowledgePointIds().stream()
                    .map(id -> {
                        KnowledgePointVO kp = courseQueryApi.getKnowledgePointById(id);
                        return kp != null ? kp.getTitle() : "考点#" + id;
                    })
                    .collect(Collectors.toList());
        }

        // 2. 组装深度学情诊断 Prompt
        String systemPrompt = buildSystemPrompt(studentId != null);
        String userPrompt = buildUserPrompt(courseName, studentId, mastery, weakTitles);

        // 3. 调用真实已接入的 AI 模型 API（如 DeepSeek 等）进行认知推演
        try {
            log.info("[AI Teaching Advice] 正在调用真实 AI 模型 API 生成教学诊断建议，courseId={}, studentId={}, weakCount={}",
                    courseId, studentId, weakTitles.size());
            String aiReply = aiChatApi.chat("TEACHING_ADVICE", systemPrompt, userPrompt);
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
                   "}";
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
               "}";
    }

    private String buildUserPrompt(String courseName, Long studentId, KnowledgeMasteryVO mastery, List<String> weakTitles) {
        StringBuilder sb = new StringBuilder();
        sb.append("【学情上下文数据】\n");
        sb.append("课程名称：").append(courseName).append("\n");
        if (studentId != null) {
            sb.append("诊断对象：学员编号 #").append(studentId).append("\n");
        } else {
            sb.append("诊断对象：全班学生宏观教学学情\n");
        }

        if (mastery != null && mastery.getDimensions() != null && !mastery.getDimensions().isEmpty()) {
            sb.append("知识维度与掌握情况：\n");
            for (int i = 0; i < mastery.getDimensions().size(); i++) {
                String dim = mastery.getDimensions().get(i);
                Object pScore = (mastery.getPersonal() != null && i < mastery.getPersonal().size()) ? mastery.getPersonal().get(i) : "-";
                Object cScore = (mastery.getClassAvg() != null && i < mastery.getClassAvg().size()) ? mastery.getClassAvg().get(i) : "-";
                if (studentId != null) {
                    sb.append("  - ").append(dim).append("：个人得分 ").append(pScore).append("分，班级均值 ").append(cScore).append("分\n");
                } else {
                    sb.append("  - ").append(dim).append("：班级平均达成 ").append(cScore).append("分\n");
                }
            }
        }

        if (weakTitles != null && !weakTitles.isEmpty()) {
            sb.append("重点薄弱考点清单：").append(String.join("、", weakTitles)).append("\n");
        } else {
            sb.append("考点情况：该课程各核心知识模块暂无显著低于 70 分的预警考点，整体处于良性掌握状态。\n");
        }

        sb.append("\n请综合上述真实学情大数据，给出专业精准的诊断结论与 3-4 条具有实操价值的行动项。严格按上述 JSON 结构输出。");
        return sb.toString();
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
                vo.setSummary(json.getString("summary"));
                JSONArray arr = json.getJSONArray("actions");
                if (arr != null) {
                    List<String> actions = new ArrayList<>();
                    for (int i = 0; i < arr.size(); i++) {
                        String a = arr.getString(i);
                        if (StringUtils.hasText(a)) {
                            actions.add(a.trim());
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
        fallback.setSummary(summarySb.length() > 0 ? summarySb.toString().trim() : cleaned);
        fallback.setActions(actions.isEmpty() ? List.of("根据诊断建议进行针对性专题巩固", "安排课后答疑与错因回溯") : actions);
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
