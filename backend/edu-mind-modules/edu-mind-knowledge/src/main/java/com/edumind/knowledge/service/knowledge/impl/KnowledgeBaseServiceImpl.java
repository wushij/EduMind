package com.edumind.knowledge.service.knowledge.impl;

import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.course.api.CourseQueryApi;
import com.edumind.course.vo.course.CourseDetailVO;
import com.edumind.course.vo.course.CourseVO;
import com.edumind.knowledge.converter.KnowledgeBaseConverter;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.dao.KnowledgeChunkIndexDao;
import com.edumind.knowledge.dto.knowledge.KnowledgeBaseCreateDTO;
import com.edumind.knowledge.dto.knowledge.KnowledgeBaseUpdateDTO;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.service.knowledge.KnowledgeAccessService;
import com.edumind.knowledge.service.knowledge.KnowledgeBaseService;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseDao knowledgeBaseDao;
    private final KnowledgeBaseConverter knowledgeBaseConverter;
    private final KnowledgeAccessService knowledgeAccessService;
    private final CourseQueryApi courseQueryApi;
    private final KnowledgeChunkIndexDao knowledgeChunkIndexDao;

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
        return vos;
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
    public void delete(Long id) {
        knowledgeAccessService.assertAccessible(id);
        knowledgeBaseDao.deleteById(id);
    }
}
