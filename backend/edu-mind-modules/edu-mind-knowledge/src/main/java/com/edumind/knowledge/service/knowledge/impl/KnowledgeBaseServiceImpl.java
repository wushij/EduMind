package com.edumind.knowledge.service.knowledge.impl;

import com.edumind.ai.api.embedding.EmbeddingApi;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseKnowledgeBaseCommandApi;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.knowledge.converter.KnowledgeBaseConverter;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.dao.KnowledgeChunkIndexDao;
import com.edumind.knowledge.dao.KnowledgeDocumentDao;
import com.edumind.knowledge.dto.knowledge.KnowledgeBaseCreateDTO;
import com.edumind.knowledge.dto.knowledge.KnowledgeBaseUpdateDTO;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentEntity;
import com.edumind.knowledge.service.knowledge.DocumentService;
import com.edumind.knowledge.service.knowledge.KnowledgeAccessService;
import com.edumind.knowledge.service.knowledge.KnowledgeBaseService;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseDao knowledgeBaseDao;
    private final KnowledgeDocumentDao knowledgeDocumentDao;
    private final KnowledgeBaseConverter knowledgeBaseConverter;
    private final KnowledgeAccessService knowledgeAccessService;
    private final CourseQueryApi courseQueryApi;
    private final KnowledgeChunkIndexDao knowledgeChunkIndexDao;
    private final EmbeddingApi embeddingApi;
    /** 删除知识库时需同步解除课程绑定，否则 course.knowledge_base_id 会留下悬空引用 */
    private final CourseKnowledgeBaseCommandApi courseKnowledgeBaseCommandApi;
    /** 级联清理文档：复用单文档删除逻辑，连带清理切片、向量索引与对象存储 */
    private final DocumentService documentService;

    @Override
    public Long create(KnowledgeBaseCreateDTO dto) {
        KnowledgeBaseEntity entity = knowledgeBaseConverter.toEntity(dto);
        if (entity.getTenantId() == null) {
            entity.setTenantId(TenantContext.requireTenantId());
        }
        knowledgeBaseDao.insert(entity);
        return entity.getId();
    }

    @Override
    public KnowledgeBaseVO getById(Long id) {
        KnowledgeBaseEntity entity = knowledgeAccessService.assertAccessible(id);
        KnowledgeBaseVO vo = knowledgeBaseConverter.toVO(entity);
        if (vo != null && vo.getCourseId() != null) {
            try {
                CourseDetailVO course = courseQueryApi.getCourseById(vo.getCourseId());
                if (course != null) {
                    vo.setCourseName(course.getName());
                }
            } catch (Exception ignored) {
            }
        }
        reconcileIndexStatus(vo);
        applyEmbeddingRuntime(vo);
        return vo;
    }

    @Override
    public List<KnowledgeBaseVO> list(Long courseId) {
        List<KnowledgeBaseEntity> entities = knowledgeAccessService.listAccessibleKnowledgeBases(courseId);
        List<KnowledgeBaseVO> vos = entities.stream().map(knowledgeBaseConverter::toVO).collect(Collectors.toList());
        List<Long> courseIds = vos.stream()
                .map(KnowledgeBaseVO::getCourseId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!courseIds.isEmpty()) {
            try {
                Map<Long, String> courseMap = courseQueryApi.listCoursesByIds(courseIds).stream()
                        .collect(Collectors.toMap(CourseVO::getId, CourseVO::getName, (a, b) -> a));
                vos.forEach(v -> {
                    if (v.getCourseId() != null) {
                        v.setCourseName(courseMap.get(v.getCourseId()));
                    }
                });
            } catch (Exception ignored) {
            }
        }
        vos.forEach(this::reconcileIndexStatus);
        vos.forEach(this::applyEmbeddingRuntime);
        return vos;
    }

    /**
     * 回填当前向量化基座信息（模型名 + 是否 Mock 伪向量）。
     *
     * <p>此前该字段从未赋值，前端只能显示兜底文案「默认模型」，
     * 用户既看不到真实模型名，也无法判断当前向量是不是哈希伪向量。</p>
     */
    private void applyEmbeddingRuntime(KnowledgeBaseVO vo) {
        if (vo == null) {
            return;
        }
        try {
            vo.setEmbeddingModel(embeddingApi.getModelName());
            vo.setEmbeddingMocked(embeddingApi.isMockVector());
        } catch (Exception ex) {
            // 向量基座信息读取失败不应影响知识库列表本身的可用性
            log.warn("读取 Embedding 运行信息失败 kbId={}: {}", vo.getId(), ex.getMessage());
        }
    }

    /**
     * 课节讲义等场景下切片已向量化，但 knowledge_base.index_status 仍为 PENDING 时自动对齐。
     */
    private void reconcileIndexStatus(KnowledgeBaseVO vo) {
        if (vo == null || vo.getId() == null) {
            return;
        }
        String status = vo.getIndexStatus();
        if ("INDEXED".equalsIgnoreCase(status) || "INDEXING".equalsIgnoreCase(status)) {
            return;
        }
        int chunks = vo.getChunkCount() != null ? vo.getChunkCount() : 0;
        if (chunks <= 0) {
            return;
        }
        long indexed = knowledgeChunkIndexDao.countIndexedByKnowledgeBaseId(vo.getId());
        if (indexed >= chunks) {
            vo.setIndexStatus("INDEXED");
            KnowledgeBaseEntity entity = knowledgeBaseDao.findById(vo.getId());
            if (entity != null && !"INDEXED".equalsIgnoreCase(entity.getIndexStatus())) {
                entity.setIndexStatus("INDEXED");
                knowledgeBaseDao.updateById(entity);
            }
        } else if (indexed > 0) {
            vo.setIndexStatus("INDEXING");
        }
    }

    @Override
    public void update(Long id, KnowledgeBaseUpdateDTO dto) {
        KnowledgeBaseEntity entity = knowledgeAccessService.assertAccessible(id);
        if (entity == null) {
            throw new BusinessException("知识库不存在");
        }
        knowledgeBaseConverter.applyUpdate(entity, dto);
        knowledgeBaseDao.updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        knowledgeAccessService.assertAccessible(id);

        // 1. 级联清理文档：此前只删 knowledge_base 一行，会留下大量孤儿数据
        //    （实库已积累 17 条指向已删除知识库的文档、2 条切片）。
        //    复用单文档删除逻辑，连带清掉切片、文本、向量索引与对象存储。
        List<KnowledgeDocumentEntity> documents = knowledgeDocumentDao.findByKnowledgeBaseId(id);
        for (KnowledgeDocumentEntity document : documents) {
            documentService.delete(document.getId());
        }

        // 2. 删除知识库本体
        knowledgeBaseDao.deleteById(id);

        // 3. 解除课程绑定：course.knowledge_base_id 是无外键约束的裸列，
        //    留下悬空引用会让课程详情页持续请求已删除的知识库并报「知识库不存在」。
        //    与删除同事务，避免删除成功但解绑失败而重新制造脏数据。
        int unbound = courseKnowledgeBaseCommandApi.unbindByKnowledgeBaseId(id);
        log.info("知识库 {} 已删除，级联清理 {} 个文档，解除 {} 门课程的绑定", id, documents.size(), unbound);
    }
}
