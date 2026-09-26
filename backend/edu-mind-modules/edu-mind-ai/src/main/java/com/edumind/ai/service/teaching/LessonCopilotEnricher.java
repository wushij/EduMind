package com.edumind.ai.service.teaching;

import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.api.LessonQueryApi;
import com.edumind.course.vo.chapter.ChapterTreeVO;
import com.edumind.course.vo.lesson.LessonCopilotContextVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonCopilotEnricher {

    private final LessonQueryApi lessonQueryApi;
    private final CourseQueryApi courseQueryApi;

    public String buildEnrichmentBlock(Long courseId, Long lessonChapterId, boolean previewDraft, String userQuestion) {
        if (courseId == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();

        // 1. 课程全局章节目录体系（让 AI 拥有完整的宏观课程大纲视野）
        appendCourseCurriculumTree(sb, courseId, lessonChapterId);

        // 2. 当前具体微课时讲义正文与内部文章结构（让 AI 深入掌握当前课节的实际内容）
        if (lessonChapterId != null) {
            appendLessonDetails(sb, courseId, lessonChapterId, previewDraft, userQuestion);
        }

        sb.append("\n【助教回答准则】\n")
                .append("1. 涉及课程结构、大纲脉络、章节列表、课时数量等宏观提问，严格依据上述【本课程全局章节目录体系】如实陈述各章与小节名称，严禁臆测；\n")
                .append("2. 涉及课文内容、知识点解析、代码原理与概念辨析，优先依据【当前课节文章结构与讲义正文】及核心目标进行深度讲解，若文中有代码或分级标题请参照解答；\n")
                .append("3. 涉及例题讲解（如用户提问‘例题1’、‘例1’、‘例题’等），【参考资料】或【当前课节讲义】中的‘例1’等对应题目即为用户所指的目标题目，必须直接提取其完整题干并展开详细推导与解题步骤教学，严禁以‘看不到题干’、‘请上传原题’为由拒答或推脱；\n")
                .append("4. 保持严谨、专业且富有启发性的教学风格，不编造课文与大纲中不存在的内容。\n");

        return sb.toString();
    }

    private void appendCourseCurriculumTree(StringBuilder sb, Long courseId, Long currentLessonChapterId) {
        try {
            List<ChapterTreeVO> tree = courseQueryApi.listChaptersByCourseId(courseId);
            if (tree == null || tree.isEmpty()) {
                return;
            }
            sb.append("【本课程全局章节目录体系】\n");
            int chapIdx = 1;
            for (ChapterTreeVO chap : tree) {
                String chapTitle = StringUtils.hasText(chap.getTitle()) ? chap.getTitle().trim() : ("第" + chapIdx + "章");
                sb.append("- ").append(chapTitle);
                if (chap.getId() != null) {
                    sb.append(" (章ID: ").append(chap.getId()).append(")");
                }
                sb.append("\n");

                List<ChapterTreeVO> sections = chap.getChildren();
                if (sections != null && !sections.isEmpty()) {
                    int secIdx = 1;
                    for (ChapterTreeVO sec : sections) {
                        String secTitle = StringUtils.hasText(sec.getTitle()) ? sec.getTitle().trim() : ("第" + secIdx + "节");
                        boolean isCurrent = currentLessonChapterId != null && currentLessonChapterId.equals(sec.getId());
                        sb.append("  * [课时 ").append(sec.getId() != null ? sec.getId() : secIdx).append("] ")
                                .append(secTitle);
                        if (isCurrent) {
                            sb.append("  <-- (用户当前正在研读该课节)");
                        }
                        sb.append("\n");
                        secIdx++;
                    }
                }
                chapIdx++;
            }
            sb.append("\n");
        } catch (Exception ex) {
            log.warn("Failed to append course curriculum tree courseId={}: {}", courseId, ex.getMessage());
        }
    }

    private void appendLessonDetails(StringBuilder sb, Long courseId, Long lessonChapterId, boolean previewDraft, String userQuestion) {
        try {
            LessonCopilotContextVO ctx = null;
            try {
                ctx = lessonQueryApi.getCopilotContext(courseId, lessonChapterId, previewDraft, userQuestion);
            } catch (Exception ex) {
                if (!previewDraft) {
                    try {
                        ctx = lessonQueryApi.getCopilotContext(courseId, lessonChapterId, true, userQuestion);
                    } catch (Exception ignored) {
                    }
                }
            }
            if (ctx == null) {
                return;
            }
            sb.append("【当前选定课节详细信息】\n");
            if (StringUtils.hasText(ctx.getTitle())) {
                sb.append("- 课节名称：").append(ctx.getTitle().trim()).append("\n");
            }
            if (StringUtils.hasText(ctx.getDescription())) {
                sb.append("- 课节导读：").append(ctx.getDescription().trim()).append("\n");
            }
            if (StringUtils.hasText(ctx.getObjectivesText())) {
                sb.append("\n【课节核心学习目标】\n").append(ctx.getObjectivesText().trim()).append("\n");
            }
            if (StringUtils.hasText(ctx.getRelevantBodyMarkdown())) {
                sb.append("\n【当前课节文章结构与讲义正文】\n");
                if (ctx.isBodyTruncated()) {
                    sb.append("（注：讲义正文篇幅较长，以下为与提问最相关的核心章节段落与文章结构）\n");
                }
                sb.append(ctx.getRelevantBodyMarkdown().trim()).append("\n");
            }
        } catch (Exception ex) {
            log.warn("Failed to append lesson details courseId={} lessonChapterId={}: {}", courseId, lessonChapterId, ex.getMessage());
        }
    }
}
