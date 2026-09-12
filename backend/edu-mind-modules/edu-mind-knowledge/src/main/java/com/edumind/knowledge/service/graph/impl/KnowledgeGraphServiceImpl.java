package com.edumind.knowledge.service.graph.impl;

import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.knowledge.api.KnowledgePointQueryApi;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.dao.KnowledgeDocumentChunkDao;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentChunkEntity;
import com.edumind.knowledge.service.graph.KnowledgeGraphService;
import com.edumind.knowledge.service.knowledge.KnowledgeAccessService;
import com.edumind.knowledge.vo.graph.KnowledgeGraphVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private final KnowledgeAccessService knowledgeAccessService;
    private final KnowledgeBaseDao knowledgeBaseDao;
    private final KnowledgeDocumentChunkDao knowledgeDocumentChunkDao;
    private final KnowledgePointQueryApi knowledgePointQueryApi;

    @Override
    public KnowledgeGraphVO buildGraph(Long knowledgeBaseId) {
        knowledgeAccessService.assertAccessible(knowledgeBaseId);
        KnowledgeGraphVO graph = new KnowledgeGraphVO();
        KnowledgeBaseEntity kb = knowledgeBaseDao.findById(knowledgeBaseId);
        if (kb == null) {
            return graph;
        }

        Set<String> nodeIds = new HashSet<>();
        if (kb.getCourseId() != null) {
            List<KnowledgePointVO> points = knowledgePointQueryApi.listByCourseId(kb.getCourseId());
            Map<Long, String> chapterNodeMap = new HashMap<>();
            for (KnowledgePointVO point : points) {
                String nodeId = "kp_" + point.getId();
                addNode(graph, nodeIds, nodeId, point.getTitle(), "KNOWLEDGE_POINT", point.getId());
                if (point.getChapterId() != null) {
                    String chapterNodeId = "ch_" + point.getChapterId();
                    if (!chapterNodeMap.containsKey(point.getChapterId())) {
                        chapterNodeMap.put(point.getChapterId(), chapterNodeId);
                        addNode(graph, nodeIds, chapterNodeId, "章节 " + point.getChapterId(), "CHAPTER", point.getChapterId());
                    }
                    addEdge(graph, chapterNodeId, nodeId, "contains");
                }
            }
        }

        List<KnowledgeDocumentChunkEntity> chunks = knowledgeDocumentChunkDao.findByKnowledgeBaseId(knowledgeBaseId);
        String prevNodeId = null;
        Long prevDocumentId = null;
        for (KnowledgeDocumentChunkEntity chunk : chunks) {
            if (!StringUtils.hasText(chunk.getHeading())) {
                continue;
            }
            String nodeId = "chunk_" + chunk.getId();
            addNode(graph, nodeIds, nodeId, chunk.getHeading(), "CHUNK", chunk.getId());
            if (prevNodeId != null && prevDocumentId != null && prevDocumentId.equals(chunk.getDocumentId())) {
                addEdge(graph, prevNodeId, nodeId, "next");
            }
            prevNodeId = nodeId;
            prevDocumentId = chunk.getDocumentId();
        }
        return graph;
    }

    private void addNode(KnowledgeGraphVO graph, Set<String> nodeIds, String id, String label, String type, Long refId) {
        if (nodeIds.contains(id)) {
            return;
        }
        KnowledgeGraphVO.GraphNodeVO node = new KnowledgeGraphVO.GraphNodeVO();
        node.setId(id);
        node.setLabel(label);
        node.setType(type);
        node.setRefId(refId);
        graph.getNodes().add(node);
        nodeIds.add(id);
    }

    private void addEdge(KnowledgeGraphVO graph, String source, String target, String relation) {
        KnowledgeGraphVO.GraphEdgeVO edge = new KnowledgeGraphVO.GraphEdgeVO();
        edge.setSource(source);
        edge.setTarget(target);
        edge.setRelation(relation);
        graph.getEdges().add(edge);
    }
}
