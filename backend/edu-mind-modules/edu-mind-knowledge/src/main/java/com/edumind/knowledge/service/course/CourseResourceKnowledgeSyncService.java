package com.edumind.knowledge.service.course;

import com.edumind.common.exception.BusinessException;
import com.edumind.knowledge.dao.KnowledgeBaseDao;
import com.edumind.knowledge.dao.KnowledgeDocumentDao;
import com.edumind.knowledge.dto.CourseResourceKnowledgeSyncDTO;
import com.edumind.knowledge.entity.KnowledgeBaseEntity;
import com.edumind.knowledge.entity.KnowledgeDocumentEntity;
import com.edumind.knowledge.service.knowledge.DocumentPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.edumind.knowledge.support.CourseResourceKnowledgeSyncSupport;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseResourceKnowledgeSyncService {

    private static final String SOURCE_COURSE_RESOURCE = "COURSE_RESOURCE";

    private final KnowledgeBaseDao knowledgeBaseDao;
    private final KnowledgeDocumentDao knowledgeDocumentDao;
    private final DocumentPipelineService documentPipelineService;

    @Transactional(rollbackFor = Exception.class)
    public Long sync(CourseResourceKnowledgeSyncDTO dto) {
        validate(dto);
        if (!CourseResourceKnowledgeSyncSupport.isSyncableType(dto.getResourceType(), dto.getFileName())) {
            throw new BusinessException("当前文件类型暂不支持同步至知识库，请上传 MD、TXT、PDF 或 Word 文档");
        }
        KnowledgeBaseEntity kb = knowledgeBaseDao.findById(dto.getKnowledgeBaseId());
        if (kb == null) {
            throw new BusinessException("关联知识库不存在");
        }
        KnowledgeDocumentEntity existing = knowledgeDocumentDao.findByCourseResourceId(dto.getCourseResourceId());

        KnowledgeDocumentEntity document = existing != null ? existing : new KnowledgeDocumentEntity();
        document.setKnowledgeBaseId(dto.getKnowledgeBaseId());
        document.setSourceType(SOURCE_COURSE_RESOURCE);
        document.setCourseId(dto.getCourseId());
        document.setCourseResourceId(dto.getCourseResourceId());
        document.setFileName(dto.getFileName());
        document.setFileType(CourseResourceKnowledgeSyncSupport.normalizeFileType(dto.getResourceType(), dto.getFileName()));
        document.setFileSize(dto.getFileSize());
        document.setObjectKey(dto.getObjectKey());
        document.setParseStatus("PENDING");
        document.setErrorMessage(null);
        document.setStatus(1);
        if (existing == null) {
            knowledgeDocumentDao.insert(document);
        } else {
            knowledgeDocumentDao.updateById(document);
        }
        refreshDocCount(dto.getKnowledgeBaseId());
        // 必须走 requestPipeline：本方法在事务内，直接调异步方法会让流水线线程读不到未提交的文档行
        documentPipelineService.requestPipeline(document.getId());
        log.info("Course resource synced to KB courseResourceId={} documentId={} kbId={}",
                dto.getCourseResourceId(), document.getId(), dto.getKnowledgeBaseId());
        return document.getId();
    }

    private void validate(CourseResourceKnowledgeSyncDTO dto) {
        if (dto == null || dto.getKnowledgeBaseId() == null || dto.getCourseId() == null) {
            throw new BusinessException("知识库同步参数不完整");
        }
        if (!StringUtils.hasText(dto.getObjectKey())) {
            throw new BusinessException("课件存储路径缺失，无法同步知识库");
        }
        if (!StringUtils.hasText(dto.getFileName())) {
            dto.setFileName("course-resource");
        }
    }

    private void refreshDocCount(Long knowledgeBaseId) {
        KnowledgeBaseEntity kb = knowledgeBaseDao.findById(knowledgeBaseId);
        if (kb != null) {
            kb.setDocCount((int) knowledgeDocumentDao.countByKnowledgeBaseId(knowledgeBaseId));
            knowledgeBaseDao.updateById(kb);
        }
    }
}
