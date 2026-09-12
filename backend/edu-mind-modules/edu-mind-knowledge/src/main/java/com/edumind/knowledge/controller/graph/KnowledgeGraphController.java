package com.edumind.knowledge.controller.graph;

import com.edumind.common.api.ApiResult;
import com.edumind.knowledge.dto.graph.KnowledgePointRelationCreateDTO;
import com.edumind.knowledge.service.graph.KnowledgeGraphService;
import com.edumind.knowledge.vo.graph.GraphGapVO;
import com.edumind.knowledge.vo.graph.KnowledgeGraphVO;
import com.edumind.knowledge.vo.graph.KnowledgePointRelationVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    @GetMapping("/knowledge-bases/{id}/graph")
    public ApiResult<KnowledgeGraphVO> getGraph(
            @PathVariable("id") Long knowledgeBaseId,
            @RequestParam(defaultValue = "2") Integer depth,
            @RequestParam(required = false) String types) {
        List<String> typeList = parseTypes(types);
        return ApiResult.success(knowledgeGraphService.buildGraph(knowledgeBaseId, depth, typeList));
    }

    @GetMapping("/knowledge-bases/{id}/graph/gaps")
    public ApiResult<List<GraphGapVO>> getGaps(
            @PathVariable("id") Long knowledgeBaseId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(defaultValue = "0.6") Double masteryThreshold) {
        return ApiResult.success(knowledgeGraphService.findGaps(knowledgeBaseId, studentId, masteryThreshold));
    }

    @PostMapping("/knowledge-bases/{id}/graph/suggest-relations")
    public ApiResult<List<java.util.Map<String, Object>>> suggestRelations(
            @PathVariable("id") Long knowledgeBaseId,
            @RequestBody(required = false) java.util.Map<String, Object> params) {
        Long sourceKpId = null;
        Integer max = 5;
        if (params != null) {
            if (params.get("sourceKnowledgePointId") != null) {
                sourceKpId = Long.valueOf(params.get("sourceKnowledgePointId").toString());
            }
            if (params.get("maxSuggestions") != null) {
                max = Integer.valueOf(params.get("maxSuggestions").toString());
            }
        }
        return ApiResult.success(knowledgeGraphService.suggestRelations(knowledgeBaseId, sourceKpId, max));
    }

    @PostMapping("/knowledge-points/{id}/relations")
    public ApiResult<Void> createRelation(
            @PathVariable("id") Long sourceKnowledgePointId,
            @Valid @RequestBody KnowledgePointRelationCreateDTO dto) {
        knowledgeGraphService.createRelation(sourceKnowledgePointId, dto);
        return ApiResult.success();
    }

    @GetMapping("/knowledge-points/{id}/relations")
    public ApiResult<List<KnowledgePointRelationVO>> listRelations(@PathVariable("id") Long knowledgePointId) {
        return ApiResult.success(knowledgeGraphService.listRelations(knowledgePointId));
    }

    @DeleteMapping("/knowledge-points/relations/{relationId}")
    public ApiResult<Void> deleteRelation(@PathVariable Long relationId) {
        knowledgeGraphService.deleteRelation(relationId);
        return ApiResult.success();
    }

    private List<String> parseTypes(String types) {
        if (types == null || types.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(types.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
