package com.edumind.knowledge.controller.graph;

import com.edumind.common.api.ApiResult;
import com.edumind.knowledge.service.graph.KnowledgeGraphService;
import com.edumind.knowledge.vo.graph.KnowledgeGraphVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    @GetMapping("/{id}/graph")
    public ApiResult<KnowledgeGraphVO> getGraph(@PathVariable("id") Long knowledgeBaseId) {
        return ApiResult.success(knowledgeGraphService.buildGraph(knowledgeBaseId));
    }
}
