package com.edumind.ai.agent.tool.impl;

import com.edumind.ai.agent.tool.AgentTool;
import com.edumind.ai.rag.pipeline.RagPipelineImpl;
import com.edumind.knowledge.api.KnowledgeQueryApi;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SearchKnowledgeTool implements AgentTool {

    private final RagPipelineImpl ragPipeline;
    private final KnowledgeQueryApi knowledgeQueryApi;

    @Override
    public String name() {
        return "search_knowledge";
    }

    @Override
    public String description() {
        return "检索知识库并返回 RAG 回答";
    }

    @Override
    public Object execute(Map<String, Object> params) {
        Long courseId = params.get("courseId") != null ? Long.valueOf(params.get("courseId").toString()) : null;
        String query = params.get("query") != null ? params.get("query").toString() : "";
        List<KnowledgeBaseVO> bases = knowledgeQueryApi.listKnowledgeBasesByCourseId(courseId);
        if (bases.isEmpty()) {
            return "未找到知识库";
        }
        return ragPipeline.execute(query, bases.get(0).getId());
    }
}
