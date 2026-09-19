package com.edumind.question.integration.export.support;

import com.edumind.question.vo.question.QuestionVO;
import com.edumind.teaching.vo.exam.ExamQuestionVO;
import com.edumind.teaching.vo.exam.ExamVO;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PaperExportSectionGrouper {

    private static final Map<String, String> TYPE_LABELS = Map.of(
            "SINGLE_CHOICE", "单项选择题",
            "MULTIPLE_CHOICE", "多项选择题",
            "TRUE_FALSE", "判断题",
            "FILL_BLANK", "填空题",
            "SHORT_ANSWER", "综合应用与简答题"
    );

    private PaperExportSectionGrouper() {
    }

    public static List<ExportSection> group(ExamVO exam) {
        Map<String, ExportSection> map = new LinkedHashMap<>();
        if (exam == null || exam.getQuestions() == null) {
            return List.of();
        }
        List<ExamQuestionVO> sorted = new ArrayList<>(exam.getQuestions());
        sorted.sort((a, b) -> {
            int sa = a.getSortOrder() != null ? a.getSortOrder() : 0;
            int sb = b.getSortOrder() != null ? b.getSortOrder() : 0;
            return Integer.compare(sa, sb);
        });
        int globalIndex = 1;
        for (ExamQuestionVO row : sorted) {
            QuestionVO q = row.getQuestion();
            if (q == null) {
                continue;
            }
            String type = q.getType() != null ? q.getType() : "SHORT_ANSWER";
            ExportSection section = map.computeIfAbsent(type, t -> new ExportSection(t, TYPE_LABELS.getOrDefault(t, "综合试题")));
            int score = row.getScore() != null ? row.getScore() : (q.getScore() != null ? q.getScore() : 5);
            section.questions.add(new ExportQuestion(globalIndex++, q, score));
            section.totalScore += score;
        }
        return new ArrayList<>(map.values());
    }

    @Getter
    public static class ExportSection {
        private final String type;
        private final String title;
        private int totalScore;
        private final List<ExportQuestion> questions = new ArrayList<>();

        ExportSection(String type, String title) {
            this.type = type;
            this.title = title;
        }
    }

    @Getter
    public static class ExportQuestion {
        private final int index;
        private final QuestionVO question;
        private final int score;

        ExportQuestion(int index, QuestionVO question, int score) {
            this.index = index;
            this.question = question;
            this.score = score;
        }
    }
}
