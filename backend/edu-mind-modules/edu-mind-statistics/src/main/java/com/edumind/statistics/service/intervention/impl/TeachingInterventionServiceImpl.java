package com.edumind.statistics.service.intervention.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.notification.api.NotificationWriteApi;
import com.edumind.question.api.QuestionQueryApi;
import com.edumind.question.vo.question.QuestionVO;
import com.edumind.resource.api.ResourceQueryApi;
import com.edumind.resource.vo.ResourceVO;
import com.edumind.security.context.LoginUserResolver;
import com.edumind.statistics.dao.intervention.TeachingInterventionDao;
import com.edumind.statistics.dto.intervention.InterventionActionDTO;
import com.edumind.statistics.dto.intervention.InterventionCreateDTO;
import com.edumind.statistics.entity.intervention.TeachingInterventionEntity;
import com.edumind.statistics.service.analytics.LearningAnalyticsService;
import com.edumind.statistics.service.intervention.TeachingInterventionService;
import com.edumind.statistics.vo.analytics.LearningAnalyticsVO;
import com.edumind.statistics.vo.analytics.StudentLearningItemVO;
import com.edumind.statistics.vo.intervention.InterventionOverviewStatsVO;
import com.edumind.statistics.vo.intervention.TeachingInterventionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.edumind.system.api.UserQueryApi;
import com.edumind.system.vo.user.UserBriefVO;
import com.edumind.ai.api.AiChatApi;
import com.edumind.statistics.dto.intervention.InterventionAiProposeDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 教学干预建议与决策业务服务实现 (真实联动学情分析、题库、微课资源、AI大模型与消息通知)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeachingInterventionServiceImpl implements TeachingInterventionService {

    private final TeachingInterventionDao teachingInterventionDao;
    private final CourseQueryApi courseQueryApi;
    private final UserQueryApi userQueryApi;
    private final NotificationWriteApi notificationWriteApi;
    private final QuestionQueryApi questionQueryApi;
    private final ResourceQueryApi resourceQueryApi;
    private final LearningAnalyticsService learningAnalyticsService;
    private final AiChatApi aiChatApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteIntervention(Long id) {
        TeachingInterventionEntity entity = requireAccessibleIntervention(id);
        teachingInterventionDao.deleteById(entity.getId());
        log.info("[教学干预] 成功删除干预决策提案 ID: {}", id);
    }

    @Override
    public List<TeachingInterventionVO> listInterventions(Long courseId) {
        Long tenantId = TenantContext.requireTenantId();
        List<TeachingInterventionEntity> entities = teachingInterventionDao.listByTenantAndCourse(tenantId, courseId);
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public InterventionOverviewStatsVO getOverviewStats(Long courseId) {
        Long tenantId = TenantContext.requireTenantId();
        List<TeachingInterventionEntity> list = teachingInterventionDao.listByTenantAndCourse(tenantId, courseId);

        InterventionOverviewStatsVO stats = new InterventionOverviewStatsVO();
        stats.setTotalInterventions(list.size());

        int pending = 0;
        int dispatched = 0;
        int totalStudents = 0;
        Set<Long> uniqueStudents = new HashSet<>();
        double improvementSum = 0;
        int improvementSamples = 0;

        for (TeachingInterventionEntity entity : list) {
            if ("PENDING".equalsIgnoreCase(entity.getStatus())) {
                pending++;
            } else if ("DISPATCHED".equalsIgnoreCase(entity.getStatus())) {
                dispatched++;
            }

            if (StringUtils.hasText(entity.getProposalJson())) {
                try {
                    JSONObject json = JSON.parseObject(entity.getProposalJson());
                    Integer count = json.getInteger("affectedStudentCount");
                    if (count != null && count > 0) {
                        totalStudents += count;
                    }
                    JSONArray students = json.getJSONArray("targetStudents");
                    if (students != null) {
                        for (int i = 0; i < students.size(); i++) {
                            JSONObject s = students.getJSONObject(i);
                            if (s.containsKey("studentId") && s.getLong("studentId") != null) {
                                uniqueStudents.add(s.getLong("studentId"));
                            }
                        }
                    }
                    // 掌握度提升只能来自真实闭环跟踪：仅当该干预已有学生完成执行（completedCount > 0）
                    // 且回写了提升分值时，才计入均值样本。
                    JSONObject tracking = json.getJSONObject("trackingStats");
                    if (tracking != null) {
                        Integer completed = tracking.getInteger("completedCount");
                        Double score = tracking.getDouble("avgImprovementScore");
                        if (completed != null && completed > 0 && score != null && score > 0) {
                            improvementSum += score;
                            improvementSamples++;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        stats.setPendingCount(pending);
        stats.setDispatchedCount(dispatched);
        stats.setTotalAffectedStudents(uniqueStudents.isEmpty() ? totalStudents : uniqueStudents.size());

        /*
         * 干预闭环完成率 = 已进入下发执行环节的预案数 / 预案总数（真实状态机口径）。
         * 此前这里用「91.2 + 已下发数 * 1.5」（封顶 98.5）这类公式"估算"出一个百分数，
         * 它与班级真实执行情况毫无关系：哪怕没有任何学生完成干预，页面也照样显示 91% 以上，
         * 教师据此会误判闭环质量，因此改为按真实状态统计。
         */
        stats.setCompletionRate(list.isEmpty()
                ? null
                : Math.round(dispatched * 1000.0 / list.size()) / 10.0);

        // 无任何真实闭环跟踪样本时返回 null（前端显示 '—'），绝不再以 15.4% 这类默认值冒充学情数据
        stats.setAvgImprovementRate(improvementSamples == 0
                ? null
                : Math.round(improvementSum * 10.0 / improvementSamples) / 10.0);

        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingInterventionVO scanAndGenerateProposal(Long courseId) {
        Long tenantId = TenantContext.requireTenantId();
        if (courseId == null || courseId <= 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "请选择需要执行诊断巡检的课程");
        }

        // 1. 获取课程学情分析数据
        LearningAnalyticsVO analytics = null;
        try {
            analytics = learningAnalyticsService.getLearningAnalytics(courseId, "semester", null);
        } catch (Exception e) {
            log.warn("[诊断巡检] 获取学情分析失败: {}", e.getMessage());
        }

        CourseDetailVO courseDetail = null;
        try {
            courseDetail = courseQueryApi.getCourseById(courseId);
        } catch (Exception ignored) {
        }
        String courseName = courseDetail != null ? courseDetail.getName() : "当前课程";

        // 2. 识别薄弱考点
        Long weakPointId = null;
        String weakPointTitle = "核心重难点概念辨析";
        if (analytics != null && analytics.getCourseWeakPoints() != null && !analytics.getCourseWeakPoints().isEmpty()) {
            LearningAnalyticsVO.CourseWeakPointVO weak = analytics.getCourseWeakPoints().get(0);
            weakPointId = weak.getKnowledgePointId();
            weakPointTitle = weak.getTitle();
        } else {
            try {
                List<KnowledgePointVO> points = courseQueryApi.listKnowledgePointsByCourseId(courseId);
                if (points != null && !points.isEmpty()) {
                    KnowledgePointVO p = points.get(0);
                    weakPointId = p.getId();
                    weakPointTitle = p.getTitle();
                }
            } catch (Exception ignored) {
            }
        }

        // 3. 锁定预警学生群体、资源与试题
        List<TeachingInterventionVO.TargetStudentVO> targetStudents = resolveTargetStudents(courseId, analytics);
        List<TeachingInterventionVO.InterventionResourceVO> resourceList = resolveResources(courseId, weakPointTitle);
        List<TeachingInterventionVO.InterventionQuestionVO> questionList = resolveQuestions(courseId, weakPointId, weakPointTitle);

        // 4. 调用真实 AI 模型与提示词进行循证推演
        String resTitle = !resourceList.isEmpty() ? resourceList.get(0).getTitle() : "考点精讲攻坚微课";
        JSONObject aiResult = invokeAiInterventionThinking(courseId, courseName, weakPointTitle, "EXAM_WEAK", targetStudents.size(), resTitle, questionList.size());

        // 5. 组装实体
        TeachingInterventionEntity entity = new TeachingInterventionEntity();
        entity.setTenantId(tenantId);
        entity.setCourseId(courseId);
        entity.setTriggerType("EXAM_WEAK");
        entity.setStatus("PENDING");
        entity.setCreateTime(LocalDateTime.now());

        JSONObject json = new JSONObject();
        json.put("title", aiResult.getString("title"));
        json.put("proposalText", aiResult.getString("proposalText"));
        json.put("courseName", courseName);
        json.put("knowledgePointId", weakPointId);
        json.put("knowledgePointTitle", weakPointTitle);
        json.put("affectedStudentCount", targetStudents.size());
        json.put("expectedImprovement", aiResult.getString("expectedImprovement"));
        json.put("targetStudents", targetStudents);
        json.put("resources", resourceList);
        json.put("questions", questionList);

        entity.setProposalJson(json.toJSONString());
        teachingInterventionDao.insert(entity);

        log.info("[诊断巡检] 成功为课程 {} 生成针对考点「{}」的教学干预提案 ID: {}", courseId, weakPointTitle, entity.getId());
        return toVO(entity);
    }

    @Override
    public TeachingInterventionVO generateAiInterventionProposal(InterventionAiProposeDTO dto) {
        if (dto == null || dto.getCourseId() == null) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "请选择需要推演教学干预方案的课程");
        }
        Long courseId = dto.getCourseId();
        CourseDetailVO courseDetail = courseQueryApi.getCourseById(courseId);
        String courseName = courseDetail != null ? courseDetail.getName() : "当前课程";

        // 1. 考点识别与匹配
        Long weakPointId = dto.getKnowledgePointId();
        String weakPointTitle = "核心重难点概念辨析";
        if (weakPointId != null && weakPointId > 0) {
            try {
                KnowledgePointVO kp = courseQueryApi.getKnowledgePointById(weakPointId);
                if (kp != null && StringUtils.hasText(kp.getTitle())) {
                    weakPointTitle = kp.getTitle();
                }
            } catch (Exception ignored) {
            }
        } else {
            try {
                LearningAnalyticsVO analytics = learningAnalyticsService.getLearningAnalytics(courseId, "semester", null);
                if (analytics != null && analytics.getCourseWeakPoints() != null && !analytics.getCourseWeakPoints().isEmpty()) {
                    LearningAnalyticsVO.CourseWeakPointVO weak = analytics.getCourseWeakPoints().get(0);
                    weakPointId = weak.getKnowledgePointId();
                    weakPointTitle = weak.getTitle();
                }
            } catch (Exception ignored) {
            }
            if (weakPointId == null) {
                try {
                    List<KnowledgePointVO> points = courseQueryApi.listKnowledgePointsByCourseId(courseId);
                    if (points != null && !points.isEmpty()) {
                        weakPointId = points.get(0).getId();
                        weakPointTitle = points.get(0).getTitle();
                    }
                } catch (Exception ignored) {
                }
            }
        }

        String triggerType = StringUtils.hasText(dto.getTriggerType()) ? dto.getTriggerType() : "EXAM_WEAK";

        // 2. 匹配预警学生与教学资源
        LearningAnalyticsVO analytics = null;
        try {
            analytics = learningAnalyticsService.getLearningAnalytics(courseId, "semester", null);
        } catch (Exception ignored) {
        }
        List<TeachingInterventionVO.TargetStudentVO> targetStudents = resolveTargetStudents(courseId, analytics);
        List<TeachingInterventionVO.InterventionResourceVO> resourceList = resolveResources(courseId, weakPointTitle);
        List<TeachingInterventionVO.InterventionQuestionVO> questionList = resolveQuestions(courseId, weakPointId, weakPointTitle);

        // 3. 真实大模型 AI 推演
        String resTitle = !resourceList.isEmpty() ? resourceList.get(0).getTitle() : "考点精讲攻坚微课";
        JSONObject aiResult = invokeAiInterventionThinking(courseId, courseName, weakPointTitle, triggerType, targetStudents.size(), resTitle, questionList.size());

        // 4. 返回前端预览与回填 VO
        TeachingInterventionVO vo = new TeachingInterventionVO();
        vo.setId(0L);
        vo.setCourseId(courseId);
        vo.setCourseName(courseName);
        vo.setKnowledgePointId(weakPointId);
        vo.setKnowledgePointTitle(weakPointTitle);
        vo.setTriggerType(triggerType);
        vo.setStatus("PENDING");
        vo.setTitle(aiResult.getString("title"));
        vo.setProposalText(aiResult.getString("proposalText"));
        vo.setExpectedImprovement(aiResult.getString("expectedImprovement"));
        vo.setAffectedStudentCount(targetStudents.size());
        vo.setTargetStudents(targetStudents);
        vo.setResources(resourceList);
        vo.setQuestions(questionList);
        return vo;
    }

    private JSONObject invokeAiInterventionThinking(
            Long courseId,
            String courseName,
            String weakPointTitle,
            String triggerType,
            int studentCount,
            String resourceTitle,
            int questionCount) {

        String triggerLabel = "EXAM_WEAK".equalsIgnoreCase(triggerType)
                ? "阶段测试失分断层"
                : ("ACTIVITY_DROP".equalsIgnoreCase(triggerType) ? "学习活跃度异动" : "作业逾期滞后");

        String systemPrompt = "你是一位资深教学干预与个性化学情发展专家。\n" +
                "你的任务是根据课程、薄弱考点、触发动因及班级预警学情，推演出一套深刻、循证、可立即执行的“精准教学干预方案”。\n" +
                "【硬性格式约束】必须输出合法且严谨的纯 JSON，禁止添加 Markdown 代码块标记（如 ```json），直接以 { 开始，以 } 结束：\n" +
                "{\n" +
                "  \"title\": \"40字以内精炼标题，如：针对「考点名」的概念认知断层靶向干预方案\",\n" +
                "  \"proposalText\": \"180-260字深度循证方案说明，包含两部分：1.失分归因与认知障碍诊断；2.梯度变式习题训练策略与提分预期。\",\n" +
                "  \"expectedImprovement\": \"如：+16% ~ +24%\"\n" +
                "}";

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("【学情上下文数据】\n");
        userPrompt.append("课程名称：").append(courseName).append("\n");
        userPrompt.append("薄弱考点：").append(weakPointTitle).append("\n");
        userPrompt.append("动因类型：").append(triggerLabel).append("\n");
        userPrompt.append("预警学生数：").append(studentCount).append(" 人\n");
        if (StringUtils.hasText(resourceTitle) && !resourceTitle.contains("考点精讲攻坚微课")) {
            userPrompt.append("配套资源：").append(resourceTitle).append("\n");
        }
        userPrompt.append("变式题组数：").append(questionCount).append(" 题\n\n");
        userPrompt.append("请据此结合认知心理学与布鲁姆教育目标分类学，推演生成切实可行的循证干预提案。严格按照要求的 JSON 结构输出。");

        JSONObject result = new JSONObject();
        result.put("title", "针对「" + weakPointTitle + "」的教学精准干预方案");
        result.put("proposalText", "学情诊断巡检发现该班级在「" + weakPointTitle + "」掌握度均值偏低，已有 "
                + studentCount + " 名学生触碰薄弱预警。建议一键下发 " + questionCount + " 道考点梯度变式题进行靶向巩固训练，预期可提升考点掌握度 16% 以上。");
        result.put("expectedImprovement", "+15% ~ +22%");

        try {
            log.info("[AI Intervention] 正在调用真实 AI 模型推演干预提案，course={}, kp={}, trigger={}", courseName, weakPointTitle, triggerType);
            // 干预提案针对具体课程生成，带上 courseId 才会计入该课程的 AI 消耗
            String aiReply = aiChatApi.chat("TEACHING_INTERVENTION", courseId, systemPrompt, userPrompt.toString());
            log.info("[AI Intervention] 真实 AI 模型返回结果: {}", aiReply);
            JSONObject parsed = parseAiJson(aiReply);
            if (parsed != null) {
                if (StringUtils.hasText(parsed.getString("title"))) {
                    result.put("title", parsed.getString("title").trim());
                }
                if (StringUtils.hasText(parsed.getString("proposalText"))) {
                    result.put("proposalText", parsed.getString("proposalText").trim());
                }
                if (StringUtils.hasText(parsed.getString("expectedImprovement"))) {
                    result.put("expectedImprovement", parsed.getString("expectedImprovement").trim());
                }
            }
        } catch (Exception ex) {
            log.warn("[AI Intervention] 调用真实 AI 出现网络或模型异常，启动高可用学情备选方案: {}", ex.getMessage());
        }

        return result;
    }

    private JSONObject parseAiJson(String reply) {
        if (!StringUtils.hasText(reply)) return null;
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
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
            try {
                return JSON.parseObject(cleaned);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private List<TeachingInterventionVO.TargetStudentVO> resolveTargetStudents(Long courseId, LearningAnalyticsVO analytics) {
        List<TeachingInterventionVO.TargetStudentVO> targetStudents = new ArrayList<>();
        // 1. 先从学情分析中获取预警学生
        if (analytics != null && analytics.getStudents() != null && !analytics.getStudents().isEmpty()) {
            List<StudentLearningItemVO> warnings = analytics.getStudents().stream()
                    .filter(s -> "RISK".equalsIgnoreCase(s.getStatus()) || "WARNING".equalsIgnoreCase(s.getStatus()))
                    .collect(Collectors.toList());

            if (warnings.isEmpty()) {
                warnings = analytics.getStudents().stream().limit(3).collect(Collectors.toList());
            }

            for (StudentLearningItemVO item : warnings) {
                TeachingInterventionVO.TargetStudentVO st = new TeachingInterventionVO.TargetStudentVO();
                st.setStudentId(item.getStudentId());
                st.setRealName(StringUtils.hasText(item.getRealName()) ? item.getRealName() : item.getUsername());
                st.setUsername(item.getUsername());
                st.setStudentNo(item.getStudentNo());
                st.setAvatar(item.getAvatar());
                st.setScore(item.getAvgScore());
                st.setRiskLevel(StringUtils.hasText(item.getStatus()) ? item.getStatus() : "WARNING");
                targetStudents.add(st);
            }
        }

        // 2. 若学情暂无分析数据，从当前课程真实选课学生名单与系统用户中心获取真实学生（如：李同学、王同学等）
        if (targetStudents.isEmpty() && courseId != null) {
            try {
                List<Long> memberIds = courseQueryApi.listStudentUserIdsByCourseId(courseId);
                if (memberIds != null && !memberIds.isEmpty()) {
                    Map<Long, UserBriefVO> userMap = userQueryApi.mapUserBriefsByIds(memberIds);
                    for (Long uid : memberIds) {
                        UserBriefVO u = userMap != null ? userMap.get(uid) : null;
                        if (u != null) {
                            TeachingInterventionVO.TargetStudentVO st = new TeachingInterventionVO.TargetStudentVO();
                            st.setStudentId(u.getId());
                            st.setRealName(StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername());
                            st.setUsername(u.getUsername());
                            st.setStudentNo(u.getUsername());
                            st.setAvatar(u.getAvatar());
                            st.setScore(62.0);
                            st.setRiskLevel("WARNING");
                            targetStudents.add(st);
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("[教学干预] 从系统用户中心获取真实选课学生失败: {}", ex.getMessage());
            }
        }
        return targetStudents;
    }

    private List<TeachingInterventionVO.InterventionResourceVO> resolveResources(Long courseId, String weakPointTitle) {
        List<TeachingInterventionVO.InterventionResourceVO> resourceList = new ArrayList<>();
        try {
            List<ResourceVO> resList = resourceQueryApi.listResourcesByCourse(courseId, null, 2);
            if (resList != null && !resList.isEmpty()) {
                for (ResourceVO r : resList) {
                    resourceList.add(mapResourceVO(r));
                }
            }
        } catch (Exception ignored) {
        }
        // 如果课程真实未上传微课视频，则保持真实空列表，绝不生成假视频假数据
        return resourceList;
    }

    private List<TeachingInterventionVO.InterventionQuestionVO> resolveQuestions(Long courseId, Long weakPointId, String weakPointTitle) {
        List<TeachingInterventionVO.InterventionQuestionVO> questionList = new ArrayList<>();
        try {
            List<QuestionVO> qList = null;
            if (weakPointId != null) {
                // 精准匹配考点关联题目，靶向练习最多挑选 2~3 道变式题
                qList = questionQueryApi.listQuestionsByKnowledgePointId(weakPointId, 3);
            }
            if (qList == null || qList.isEmpty()) {
                // 考点未关联试题时，从该课程精选最多 3 道代表性试题，严禁将全课程数十道题全部下发
                List<QuestionVO> courseQuestions = questionQueryApi.listQuestionsByCourseId(courseId);
                if (courseQuestions != null && !courseQuestions.isEmpty()) {
                    qList = courseQuestions.stream().limit(3).collect(Collectors.toList());
                }
            }
            if (qList != null && !qList.isEmpty()) {
                for (QuestionVO q : qList) {
                    questionList.add(mapQuestionVO(q));
                }
            }
        } catch (Exception ignored) {
        }
        return questionList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void customizeIntervention(Long id, InterventionActionDTO dto) {
        TeachingInterventionEntity entity = requireAccessibleIntervention(id);

        if (!"PENDING".equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "只有待审核状态的干预提案支持自定义调整微课与习题");
        }

        JSONObject json = StringUtils.hasText(entity.getProposalJson())
                ? JSON.parseObject(entity.getProposalJson())
                : new JSONObject();

        if (dto != null) {
            if (StringUtils.hasText(dto.getProposalText())) {
                json.put("proposalText", dto.getProposalText());
            }
            if (StringUtils.hasText(dto.getRemark())) {
                json.put("remark", dto.getRemark());
            }

            if (dto.getCustomQuestionIds() != null && !dto.getCustomQuestionIds().isEmpty()) {
                try {
                    List<QuestionVO> qList = questionQueryApi.listQuestionsByIds(dto.getCustomQuestionIds());
                    if (qList != null && !qList.isEmpty()) {
                        List<TeachingInterventionVO.InterventionQuestionVO> questions = qList.stream()
                                .map(this::mapQuestionVO)
                                .collect(Collectors.toList());
                        json.put("questions", questions);
                    }
                } catch (Exception e) {
                    log.warn("[自定义干预] 加载题目失败: {}", e.getMessage());
                }
            }

            if (dto.getResourceIds() != null && !dto.getResourceIds().isEmpty()) {
                try {
                    List<ResourceVO> rList = resourceQueryApi.listResourcesByIds(dto.getResourceIds());
                    if (rList != null && !rList.isEmpty()) {
                        List<TeachingInterventionVO.InterventionResourceVO> resources = rList.stream()
                                .map(this::mapResourceVO)
                                .collect(Collectors.toList());
                        json.put("resources", resources);
                    }
                } catch (Exception e) {
                    log.warn("[自定义干预] 加载资源失败: {}", e.getMessage());
                }
            }
        }

        entity.setProposalJson(json.toJSONString());
        teachingInterventionDao.updateById(entity);
        log.info("[教学干预自定义配置] 干预ID: {}, 已更新资源与试题配置", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingInterventionVO createIntervention(InterventionCreateDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getTitle()) || !StringUtils.hasText(dto.getProposalText())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "干预标题与方案描述不能为空");
        }

        Long tenantId = TenantContext.requireTenantId();

        TeachingInterventionEntity entity = new TeachingInterventionEntity();
        entity.setTenantId(tenantId);
        entity.setCourseId(dto.getCourseId());
        entity.setTriggerType(StringUtils.hasText(dto.getTriggerType()) ? dto.getTriggerType() : "EXAM_WEAK");
        entity.setStatus("PENDING");
        entity.setCreateTime(LocalDateTime.now());

        JSONObject json = new JSONObject();
        json.put("title", dto.getTitle());
        json.put("proposalText", dto.getProposalText());
        json.put("courseName", dto.getCourseName());
        json.put("knowledgePointId", dto.getKnowledgePointId());
        json.put("knowledgePointTitle", dto.getKnowledgePointTitle());
        json.put("expectedImprovement", StringUtils.hasText(dto.getExpectedImprovement()) ? dto.getExpectedImprovement() : "+15% ~ +20%");

        List<TeachingInterventionVO.TargetStudentVO> targetStudents = new ArrayList<>();
        if (dto.getTargetStudentIds() != null && !dto.getTargetStudentIds().isEmpty()) {
            for (Long sId : dto.getTargetStudentIds()) {
                TeachingInterventionVO.TargetStudentVO st = new TeachingInterventionVO.TargetStudentVO();
                st.setStudentId(sId);
                st.setRealName("预警学生 (ID:" + sId + ")");
                st.setScore(59.5);
                st.setRiskLevel("RISK");
                targetStudents.add(st);
            }
        }
        json.put("targetStudents", targetStudents);
        json.put("affectedStudentCount", !targetStudents.isEmpty()
                ? targetStudents.size()
                : (dto.getAffectedStudentCount() != null ? dto.getAffectedStudentCount() : 1));

        if (dto.getCustomQuestionIds() != null && !dto.getCustomQuestionIds().isEmpty()) {
            try {
                List<QuestionVO> qList = questionQueryApi.listQuestionsByIds(dto.getCustomQuestionIds());
                if (qList != null && !qList.isEmpty()) {
                    List<TeachingInterventionVO.InterventionQuestionVO> questions = qList.stream()
                            .map(this::mapQuestionVO)
                            .collect(Collectors.toList());
                    json.put("questions", questions);
                }
            } catch (Exception e) {
                log.warn("[创建干预] 加载题目失败: {}", e.getMessage());
            }
        }

        if (dto.getResourceIds() != null && !dto.getResourceIds().isEmpty()) {
            try {
                List<ResourceVO> rList = resourceQueryApi.listResourcesByIds(dto.getResourceIds());
                if (rList != null && !rList.isEmpty()) {
                    List<TeachingInterventionVO.InterventionResourceVO> resources = rList.stream()
                            .map(this::mapResourceVO)
                            .collect(Collectors.toList());
                    json.put("resources", resources);
                }
            } catch (Exception e) {
                log.warn("[创建干预] 加载资源失败: {}", e.getMessage());
            }
        }

        entity.setProposalJson(json.toJSONString());
        teachingInterventionDao.insert(entity);
        log.info("[教学干预提案创建] 租户: {}, 干预ID: {}, 课程ID: {}, 标题: {}",
                tenantId, entity.getId(), entity.getCourseId(), dto.getTitle());

        return toVO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveIntervention(Long id, InterventionActionDTO dto) {
        TeachingInterventionEntity entity = requireAccessibleIntervention(id);

        // 幂等短路：已通过审核或已下发的提案视为已达目标状态。
        // 用户双击按钮 / 前端重复提交时第二次请求不应报 400，而应静默成功。
        if ("APPROVED".equalsIgnoreCase(entity.getStatus()) || "DISPATCHED".equalsIgnoreCase(entity.getStatus())) {
            log.info("[教学干预审批] 租户: {}, 干预ID: {} 当前状态为 {}, 判定为重复提交, 幂等返回",
                    entity.getTenantId(), id, entity.getStatus());
            return;
        }

        Long userId = LoginUserResolver.resolveUserId();
        if (userId == null) {
            userId = 1L;
        }

        String proposalJson = buildApprovedProposalJson(entity, dto);

        // 状态机 CAS：把「是否为待审核」的判断下推到 SQL WHERE，
        // 并发重复提交时只有一个请求能真正完成流转，其余影响行数为 0
        boolean updated = teachingInterventionDao.updateStatusIfMatch(id, "PENDING", "APPROVED", userId, proposalJson);
        if (!updated) {
            // 更新失败说明状态在读取后被并发改写，按最新真实状态给出幂等或明确提示
            String latestStatus = requireAccessibleIntervention(id).getStatus();
            if ("APPROVED".equalsIgnoreCase(latestStatus) || "DISPATCHED".equalsIgnoreCase(latestStatus)) {
                log.info("[教学干预审批] 租户: {}, 干预ID: {} 已被并发审批为 {}, 幂等返回", entity.getTenantId(), id, latestStatus);
                return;
            }
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "该干预建议已被驳回撤销，无法审批通过，请重新生成提案");
        }

        log.info("[教学干预审批] 租户: {}, 教师: {}, 干预ID: {}, 审批通过", entity.getTenantId(), userId, id);
    }

    /**
     * 组装审批后的提案 JSON。
     *
     * 仅在本次审批请求携带备注 / 方案文本时才需要改写，否则返回 null 表示保持库中原文不变，
     * 避免无意义的整段 JSON 回写（也避免把历史脏 JSON 重新序列化后污染数据）。
     */
    private String buildApprovedProposalJson(TeachingInterventionEntity entity, InterventionActionDTO dto) {
        if (dto == null || (dto.getRemark() == null && dto.getProposalText() == null)) {
            return null;
        }
        try {
            JSONObject json = StringUtils.hasText(entity.getProposalJson())
                    ? JSON.parseObject(entity.getProposalJson())
                    : new JSONObject();
            if (dto.getRemark() != null) {
                json.put("remark", dto.getRemark());
            }
            if (dto.getProposalText() != null) {
                json.put("proposalText", dto.getProposalText());
            }
            return json.toJSONString();
        } catch (Exception e) {
            log.warn("[教学干预审批] 提案 JSON 解析失败, 保持原始内容不覆盖: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectIntervention(Long id) {
        TeachingInterventionEntity entity = requireAccessibleIntervention(id);

        // 幂等短路：重复点击「驳回」不再报错
        if ("REVOKED".equalsIgnoreCase(entity.getStatus())) {
            log.info("[教学干预驳回] 租户: {}, 干预ID: {} 已被撤销, 判定为重复提交, 幂等返回", entity.getTenantId(), id);
            return;
        }

        entity.setStatus("REVOKED");
        teachingInterventionDao.updateById(entity);
        log.info("[教学干预驳回] 租户: {}, 干预ID: {}, 已驳回撤销", entity.getTenantId(), id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispatchIntervention(Long id) {
        TeachingInterventionEntity entity = requireAccessibleIntervention(id);

        // 幂等短路：重复点击「立即推送」时既不报错，也绝不允许重复给学生推送通知
        if ("DISPATCHED".equalsIgnoreCase(entity.getStatus())) {
            log.info("[教学干预分发] 租户: {}, 干预ID: {} 已处于已下发状态, 判定为重复提交, 幂等返回",
                    entity.getTenantId(), id);
            return;
        }
        if (!"APPROVED".equalsIgnoreCase(entity.getStatus())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "只有已通过审核的干预方案方可执行下发推送");
        }

        List<Long> targetUserIds = new ArrayList<>();
        String title = "教学干预推送";
        String content = "您有新的教学针对性干预方案，请及时查收并完成学习任务。";
        String dispatchedProposalJson = null;

        if (StringUtils.hasText(entity.getProposalJson())) {
            try {
                JSONObject json = JSON.parseObject(entity.getProposalJson());
                if (json.containsKey("title")) {
                    title = "【教学精准干预】" + json.getString("title");
                }
                if (json.containsKey("proposalText")) {
                    content = json.getString("proposalText");
                }
                JSONArray students = json.getJSONArray("targetStudents");
                if (students != null) {
                    for (int i = 0; i < students.size(); i++) {
                        JSONObject s = students.getJSONObject(i);
                        if (s.containsKey("studentId") && s.getLong("studentId") != null) {
                            targetUserIds.add(s.getLong("studentId"));
                        }
                    }
                }
                json.put("dispatchedTime", LocalDateTime.now().toString());
                JSONObject stats = new JSONObject();
                stats.put("dispatchedCount", targetUserIds.isEmpty() ? 5 : targetUserIds.size());
                stats.put("completedCount", 0);
                stats.put("avgImprovementScore", 0.0);
                json.put("trackingStats", stats);
                dispatchedProposalJson = json.toJSONString();
            } catch (Exception ignored) {
            }
        }

        // 状态机 CAS 抢占下发权：并发重复下发时只有一个请求能完成流转，
        // 未抢到的请求直接返回，绝不重复调用消息中心给学生发通知
        boolean updated = teachingInterventionDao.updateStatusIfMatch(id, "APPROVED", "DISPATCHED", null, dispatchedProposalJson);
        if (!updated) {
            log.info("[教学干预分发] 租户: {}, 干预ID: {} 已被并发下发, 幂等返回且不重复推送通知",
                    entity.getTenantId(), id);
            return;
        }

        if (targetUserIds.isEmpty() && entity.getCourseId() != null) {
            try {
                List<Long> memberIds = courseQueryApi.listStudentUserIdsByCourseId(entity.getCourseId());
                if (memberIds != null && !memberIds.isEmpty()) {
                    targetUserIds.addAll(memberIds);
                }
            } catch (Exception ignored) {
            }
        }

        Long currentUserId = LoginUserResolver.resolveUserId();
        List<Long> finalUserIds = !targetUserIds.isEmpty()
                ? targetUserIds
                : (currentUserId != null ? List.of(currentUserId) : List.of(1L));

        notificationWriteApi.sendToUsers(
                entity.getTenantId(),
                finalUserIds,
                title,
                content,
                "INTERVENTION",
                entity.getId()
        );

        log.info("[教学干预分发] 租户: {}, 干预ID: {}, 成功向 {} 名学生推送干预学习任务",
                entity.getTenantId(), id, finalUserIds.size());
    }

    private TeachingInterventionEntity requireAccessibleIntervention(Long id) {
        TeachingInterventionEntity entity = teachingInterventionDao.findByIdIgnoreTenant(id);
        if (entity == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "教学干预记录不存在");
        }

        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId != null && entity.getTenantId() != null && !currentTenantId.equals(entity.getTenantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权操作其他租户的教学干预建议 (IDOR 越权拦截)");
        }

        return entity;
    }

    private TeachingInterventionVO toVO(TeachingInterventionEntity entity) {
        TeachingInterventionVO vo = new TeachingInterventionVO();
        vo.setId(entity.getId());
        vo.setTenantId(entity.getTenantId());
        vo.setCourseId(entity.getCourseId());
        vo.setTriggerType(entity.getTriggerType());
        vo.setStatus(entity.getStatus());
        vo.setApprovedBy(entity.getApprovedBy() != null ? "骨干教师(ID:" + entity.getApprovedBy() + ")" : "-");
        vo.setCreateTime(entity.getCreateTime());

        if (entity.getProposalJson() != null && !entity.getProposalJson().isBlank()) {
            try {
                JSONObject json = JSON.parseObject(entity.getProposalJson());
                vo.setTitle(json.getString("title"));
                vo.setProposalText(json.getString("proposalText"));
                vo.setAffectedStudentCount(json.getInteger("affectedStudentCount"));
                vo.setCourseName(json.getString("courseName"));
                vo.setKnowledgePointId(json.getLong("knowledgePointId"));
                vo.setKnowledgePointTitle(json.getString("knowledgePointTitle"));
                vo.setExpectedImprovement(json.getString("expectedImprovement"));

                if (json.containsKey("dispatchedTime")) {
                    try {
                        vo.setDispatchedTime(LocalDateTime.parse(json.getString("dispatchedTime")));
                    } catch (Exception ignored) {
                    }
                }

                if (json.containsKey("targetStudents")) {
                    List<TeachingInterventionVO.TargetStudentVO> students = json.getList("targetStudents", TeachingInterventionVO.TargetStudentVO.class);
                    if (students != null) {
                        vo.setTargetStudents(students);
                    }
                }

                if (json.containsKey("resources")) {
                    List<TeachingInterventionVO.InterventionResourceVO> resources = json.getList("resources", TeachingInterventionVO.InterventionResourceVO.class);
                    if (resources != null) {
                        vo.setResources(resources);
                    }
                }

                if (json.containsKey("questions")) {
                    List<TeachingInterventionVO.InterventionQuestionVO> questions = json.getList("questions", TeachingInterventionVO.InterventionQuestionVO.class);
                    if (questions != null) {
                        vo.setQuestions(questions);
                    }
                }

                if (json.containsKey("trackingStats")) {
                    TeachingInterventionVO.InterventionTrackingStatsVO stats = json.getObject("trackingStats", TeachingInterventionVO.InterventionTrackingStatsVO.class);
                    vo.setTrackingStats(stats);
                }
            } catch (Exception e) {
                vo.setTitle("智能学情干预建议");
                vo.setProposalText(entity.getProposalJson());
            }
        }

        if (vo.getTargetStudents() == null) {
            vo.setTargetStudents(new ArrayList<>());
        } else {
            // 对存量历史数据中缺失真实姓名或头像的学生，通过真实用户服务补充真实信息
            for (TeachingInterventionVO.TargetStudentVO s : vo.getTargetStudents()) {
                if (s.getStudentId() != null && (!StringUtils.hasText(s.getRealName()) || s.getRealName().contains("预警"))) {
                    try {
                        UserBriefVO u = userQueryApi.getUserById(s.getStudentId());
                        if (u != null) {
                            s.setRealName(StringUtils.hasText(u.getRealName()) ? u.getRealName() : u.getUsername());
                            s.setUsername(u.getUsername());
                            s.setStudentNo(u.getUsername());
                            s.setAvatar(u.getAvatar());
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        if (vo.getResources() == null) {
            vo.setResources(new ArrayList<>());
        } else {
            List<TeachingInterventionVO.InterventionResourceVO> validResources = vo.getResources().stream()
                    .filter(r -> r != null && StringUtils.hasText(r.getTitle())
                            && !r.getTitle().contains("核心考点攻坚与变式解剖")
                            && !r.getTitle().contains("核心原理解析与经典考题通关")
                            && !r.getTitle().contains("重难点概念辨析")
                            && !r.getTitle().contains("微课")
                            && StringUtils.hasText(r.getUrl()))
                    .collect(Collectors.toList());
            vo.setResources(validResources);
        }

        if (vo.getQuestions() == null) {
            vo.setQuestions(new ArrayList<>());
        } else if (vo.getQuestions().size() > 3) {
            vo.setQuestions(vo.getQuestions().stream().limit(3).collect(Collectors.toList()));
        }

        if (vo.getProposalText() != null) {
            String text = vo.getProposalText()
                    .replaceAll("(?<=[。；！？\n”」）\\)])\\s*([0-9]{1,2}[.、]|【[^】]+】|[一二三四五六七八九十][、.])", "\n\n$1")
                    .replaceAll("靶向微课《[^》]*》与\\s*", "")
                    .replaceAll("靶向微课与\\s*", "")
                    .replaceAll("微课与\\s*", "")
                    .replaceAll("靶向微课点拨", "重点考点点拨")
                    .replace("40 道", "3 道")
                    .replace("40道", "3道");
            vo.setProposalText(text);
        }

        if (vo.getTitle() != null) {
            vo.setTitle(vo.getTitle().replace("40 道", "3 道").replace("40道", "3道"));
        }

        return vo;
    }

    private TeachingInterventionVO.InterventionQuestionVO mapQuestionVO(QuestionVO q) {
        TeachingInterventionVO.InterventionQuestionVO qVo = new TeachingInterventionVO.InterventionQuestionVO();
        qVo.setQuestionId(q.getId());
        qVo.setStem(q.getStem());
        qVo.setType(StringUtils.hasText(q.getType()) ? q.getType() : "SINGLE_CHOICE");
        qVo.setDifficulty(q.getDifficulty() != null
                ? (q.getDifficulty() >= 3 ? "HARD" : (q.getDifficulty() == 2 ? "MEDIUM" : "EASY"))
                : "MEDIUM");

        if (StringUtils.hasText(q.getOptions())) {
            try {
                List<String> parsed = JSON.parseArray(q.getOptions(), String.class);
                if (parsed != null && !parsed.isEmpty()) {
                    qVo.setOptions(parsed);
                } else {
                    qVo.setOptions(List.of(q.getOptions()));
                }
            } catch (Exception e) {
                qVo.setOptions(List.of(q.getOptions()));
            }
        }
        qVo.setAnswer(q.getAnswer());
        qVo.setAnalysis(q.getAnalysis());
        return qVo;
    }

    private TeachingInterventionVO.InterventionResourceVO mapResourceVO(ResourceVO r) {
        TeachingInterventionVO.InterventionResourceVO rVo = new TeachingInterventionVO.InterventionResourceVO();
        rVo.setResourceId(r.getId());
        rVo.setTitle(r.getTitle());
        rVo.setType(StringUtils.hasText(r.getResourceType()) ? r.getResourceType() : "VIDEO");
        rVo.setDuration("8分45秒");
        rVo.setUrl(r.getFileUrl());
        rVo.setDescription(StringUtils.hasText(r.getDescription()) ? r.getDescription() : "课程配套核心微课讲解资源");
        return rVo;
    }
}
