package com.edumind.ai.rag.query;

import lombok.Builder;
import lombok.Value;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Value
@Builder
public class QueryRewriteContext {

    String question;
    String courseName;
    String conversationHistory;

    public static QueryRewriteContext ofQuestion(String question) {
        return QueryRewriteContext.builder().question(question).build();
    }

    public Map<String, String> toVariables() {
        Map<String, String> vars = new HashMap<>(3);
        vars.put("question", question != null ? question.trim() : "");
        vars.put("course_name", courseName != null ? courseName.trim() : "");
        vars.put("conversation_history", conversationHistory != null ? conversationHistory.trim() : "");
        return vars;
    }

    public boolean hasQuestion() {
        return StringUtils.hasText(question);
    }
}
