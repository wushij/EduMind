package com.edumind.knowledge.service.graph.impl;

import com.edumind.common.api.analytics.KnowledgeMasteryQueryApi;
import com.edumind.course.vo.knowledge.KnowledgePointVO;
import com.edumind.knowledge.api.KnowledgePointQueryApi;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.dao.KnowledgeDocumentChunkDao;
import com.edumind.knowledge.dao.KnowledgePointRelationDao;
import com.edumind.knowledge.dto.graph.KnowledgePointRelationCreateDTO;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentChunkEntity;
import com.edumind.knowledge.entity.KnowledgePointRelationEntity;
import com.edumind.knowledge.service.graph.KnowledgeGraphService;
import com.edumind.knowledge.service.knowledge.KnowledgeAccessService;
import com.edumind.common.exception.BusinessException;
import com.edumind.knowledge.vo.graph.GraphGapVO;
import com.edumind.knowledge.vo.graph.KnowledgeGraphVO;
import com.edumind.knowledge.vo.graph.KnowledgePointRelationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeGraphServiceImpl implements KnowledgeGraphService {

    private final KnowledgeAccessService knowledgeAccessService;
    private final KnowledgeBaseDao knowledgeBaseDao;
    private final KnowledgeDocumentChunkDao knowledgeDocumentChunkDao;
    private final KnowledgePointQueryApi knowledgePointQueryApi;
    private final KnowledgePointRelationDao knowledgePointRelationDao;
    private final KnowledgeMasteryQueryApi knowledgeMasteryQueryApi;

    @Override
    public KnowledgeGraphVO buildGraph(Long knowledgeBaseId, Integer depth, List<String> types) {
        knowledgeAccessService.assertAccessible(knowledgeBaseId);
        int maxDepth = depth != null && depth > 0 ? Math.min(depth, 3) : 2;
        KnowledgeGraphVO graph = new KnowledgeGraphVO();
        KnowledgeBaseEntity kb = knowledgeBaseDao.findById(knowledgeBaseId);
        if (kb == null) {
            return graph;
        }

        Set<String> nodeIds = new HashSet<>();
        List<Long> kpIds = new ArrayList<>();
        if (kb.getCourseId() != null) {
            List<KnowledgePointVO> points = knowledgePointQueryApi.listByCourseId(kb.getCourseId());
            Map<Long, String> chapterNodeMap = new HashMap<>();
            for (KnowledgePointVO point : points) {
                kpIds.add(point.getId());
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
            addRelationEdges(graph, nodeIds, kpIds, types, maxDepth);
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

    private void addRelationEdges(KnowledgeGraphVO graph, Set<String> nodeIds, List<Long> kpIds,
                                    List<String> types, int maxDepth) {
        Set<Long> visited = new HashSet<>(kpIds);
        Set<Long> frontier = new HashSet<>(kpIds);
        for (int d = 0; d < maxDepth && !frontier.isEmpty(); d++) {
            List<KnowledgePointRelationEntity> relations = knowledgePointRelationDao.listBySourceIds(
                    new ArrayList<>(frontier), types);
            Set<Long> next = new HashSet<>();
            for (KnowledgePointRelationEntity rel : relations) {
                String source = "kp_" + rel.getSourceKnowledgePointId();
                String target = "kp_" + rel.getTargetKnowledgePointId();
                if (!nodeIds.contains(target)) {
                    addNode(graph, nodeIds, target, "知识点 " + rel.getTargetKnowledgePointId(), "KNOWLEDGE_POINT",
                            rel.getTargetKnowledgePointId());
                }
                addEdge(graph, source, target, rel.getRelationType());
                if (visited.add(rel.getTargetKnowledgePointId())) {
                    next.add(rel.getTargetKnowledgePointId());
                }
            }
            frontier = next;
        }
    }

    @Override
    public List<GraphGapVO> findGaps(Long knowledgeBaseId, Long studentId, Double masteryThreshold) {
        knowledgeAccessService.assertAccessible(knowledgeBaseId);
        KnowledgeBaseEntity kb = knowledgeBaseDao.findById(knowledgeBaseId);
        if (kb == null || kb.getCourseId() == null || studentId == null) {
            return List.of();
        }
        double threshold = masteryThreshold != null ? masteryThreshold : 0.6;
        Map<Long, Double> mastery = knowledgeMasteryQueryApi.getMasteryByStudentAndCourse(studentId, kb.getCourseId());
        List<KnowledgePointVO> points = knowledgePointQueryApi.listByCourseId(kb.getCourseId());
        Map<Long, String> titles = points.stream()
                .collect(Collectors.toMap(KnowledgePointVO::getId, KnowledgePointVO::getTitle, (a, b) -> a));
        List<GraphGapVO> gaps = new ArrayList<>();
        for (KnowledgePointVO point : points) {
            double score = mastery.getOrDefault(point.getId(), 0.0);
            if (score >= threshold) {
                continue;
            }
            List<KnowledgePointRelationEntity> prerequisites = knowledgePointRelationDao.listPrerequisites(point.getId());
            List<GraphGapVO.PrerequisiteVO> missing = new ArrayList<>();
            for (KnowledgePointRelationEntity pre : prerequisites) {
                double preScore = mastery.getOrDefault(pre.getTargetKnowledgePointId(), 0.0);
                if (preScore < threshold) {
                    GraphGapVO.PrerequisiteVO p = new GraphGapVO.PrerequisiteVO();
                    p.setId(pre.getTargetKnowledgePointId());
                    p.setTitle(titles.getOrDefault(pre.getTargetKnowledgePointId(), "前置知识点"));
                    missing.add(p);
                }
            }
            if (!missing.isEmpty()) {
                GraphGapVO gap = new GraphGapVO();
                gap.setKnowledgePointId(point.getId());
                gap.setTitle(point.getTitle());
                gap.setMissingPrerequisites(missing);
                gaps.add(gap);
            }
        }
        return gaps;
    }

    @Override
    public void createRelation(Long sourceKnowledgePointId, KnowledgePointRelationCreateDTO dto) {
        KnowledgePointRelationEntity entity = new KnowledgePointRelationEntity();
        entity.setSourceKnowledgePointId(sourceKnowledgePointId);
        entity.setTargetKnowledgePointId(dto.getTargetKnowledgePointId());
        entity.setRelationType(dto.getRelationType());
        knowledgePointRelationDao.insert(entity);
    }

    @Override
    public List<KnowledgePointRelationVO> listRelations(Long knowledgePointId) {
        return knowledgePointRelationDao.listByKnowledgePointId(knowledgePointId).stream()
                .map(this::toRelationVO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteRelation(Long relationId) {
        if (knowledgePointRelationDao.findById(relationId) == null) {
            throw new BusinessException("关系不存在");
        }
        knowledgePointRelationDao.deleteById(relationId);
    }

    private KnowledgePointRelationVO toRelationVO(KnowledgePointRelationEntity entity) {
        KnowledgePointRelationVO vo = new KnowledgePointRelationVO();
        vo.setId(entity.getId());
        vo.setSourceKnowledgePointId(entity.getSourceKnowledgePointId());
        vo.setTargetKnowledgePointId(entity.getTargetKnowledgePointId());
        vo.setRelationType(entity.getRelationType());
        return vo;
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

    @Override
    public List<Map<String, Object>> suggestRelations(Long knowledgeBaseId, Long sourceKnowledgePointId, Integer maxSuggestions) {
        knowledgeAccessService.assertAccessible(knowledgeBaseId);
        int max = maxSuggestions != null && maxSuggestions > 0 ? maxSuggestions : 5;
        KnowledgeBaseEntity kb = knowledgeBaseDao.findById(knowledgeBaseId);
        if (kb == null || kb.getCourseId() == null) {
            return List.of();
        }

        List<KnowledgePointVO> points = knowledgePointQueryApi.listByCourseId(kb.getCourseId());
        if (points.isEmpty()) {
            return List.of();
        }

        List<Map<String, Object>> suggestions = new ArrayList<>();
        KnowledgePointVO source = null;
        if (sourceKnowledgePointId != null) {
            source = points.stream().filter(p -> p.getId().equals(sourceKnowledgePointId)).findFirst().orElse(null);
        }
        if (source == null && !points.isEmpty()) {
            source = points.get(0);
        }
        if (source == null) {
            return List.of();
        }

        for (KnowledgePointVO target : points) {
            if (target.getId().equals(source.getId())) {
                continue;
            }
            Map<String, Object> item = new HashMap<>();
            item.put("sourceKnowledgePointId", source.getId());
            item.put("targetKnowledgePointId", target.getId());
            item.put("sourceTitle", source.getTitle());
            item.put("targetTitle", target.getTitle());
            item.put("relationType", "prerequisite");
            item.put("confidence", 0.90);
            item.put("reason", "「" + source.getTitle() + "」依赖「" + target.getTitle() + "」的先修概念，建议建立前置依赖关系。");
            suggestions.add(item);
            if (suggestions.size() >= max) {
                break;
            }
        }
        return suggestions;
    }
}
