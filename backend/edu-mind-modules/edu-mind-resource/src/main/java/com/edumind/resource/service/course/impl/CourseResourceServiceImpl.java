package com.edumind.resource.service.course.impl;

import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.utils.TenantObjectKeyBuilder;
import com.edumind.course.api.CourseAccessApi;
import com.edumind.infrastructure.oss.FileStorageService;
import com.edumind.resource.dao.CourseResourceDao;
import com.edumind.resource.dao.ResourceDao;
import com.edumind.resource.dto.course.CourseResourceCreateDTO;
import com.edumind.resource.entity.CourseResourceEntity;
import com.edumind.resource.entity.ResourceEntity;
import com.edumind.resource.service.course.CourseResourceService;
import com.edumind.resource.vo.CourseResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseResourceServiceImpl implements CourseResourceService {

    private final CourseResourceDao courseResourceDao;
    private final ResourceDao resourceDao;
    private final CourseAccessApi courseAccessApi;
    private final FileStorageService fileStorageService;

    @Value("${minio.bucketName:edumind}")
    private String bucketName;

    @Override
    public List<CourseResourceVO> listByCourseId(Long courseId) {
        return listByCourseId(courseId, null);
    }

    @Override
    public List<CourseResourceVO> listByCourseId(Long courseId, Long chapterId) {
        courseAccessApi.assertCanView(courseId);
        if (chapterId != null) {
            return resourceDao.findByCourseAndChapter(courseId, chapterId, null).stream()
                    .map(this::teachingToVO)
                    .collect(Collectors.toList());
        }
        return courseResourceDao.findByCourseId(courseId).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public Long addResource(Long courseId, CourseResourceCreateDTO dto) {
        courseAccessApi.assertCanEdit(courseId);
        CourseResourceEntity entity = new CourseResourceEntity();
        entity.setCourseId(courseId);
        entity.setResourceId(dto.getResourceId());
        entity.setDocumentId(dto.getDocumentId());
        entity.setTitle(dto.getTitle().trim());
        entity.setResourceType(resolveResourceType(dto.getResourceType(), dto.getTitle()).toUpperCase());
        entity.setCreateTime(LocalDateTime.now());
        courseResourceDao.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadResource(Long courseId, MultipartFile file, String title, String resourceType, Long chapterId) {
        courseAccessApi.assertCanEdit(courseId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "resource";
        String displayTitle = StringUtils.hasText(title) ? title.trim() : stripExtension(originalFilename);
        String type = resolveResourceType(resourceType, originalFilename).toUpperCase();

        Long tenantId = TenantContext.getTenantId();
        String objectKey = TenantObjectKeyBuilder.courseTeachingResource(
                tenantId, courseId, UUID.randomUUID().toString(), originalFilename);
        try {
            fileStorageService.uploadFile(bucketName, objectKey, file.getInputStream(), file.getContentType());
        } catch (IOException ex) {
            throw new BusinessException("课件文件上传失败");
        }

        String fileUrl = TenantObjectKeyBuilder.storageFileUrl(bucketName, objectKey);

        ResourceEntity resourceEntity = new ResourceEntity();
        resourceEntity.setCourseId(courseId);
        resourceEntity.setChapterId(chapterId);
        resourceEntity.setTitle(displayTitle);
        resourceEntity.setResourceType(type);
        resourceEntity.setFileUrl(fileUrl);
        resourceEntity.setStatus(1);
        resourceEntity.setCreateTime(LocalDateTime.now());
        resourceDao.insert(resourceEntity);

        CourseResourceEntity courseResource = new CourseResourceEntity();
        courseResource.setCourseId(courseId);
        courseResource.setResourceId(resourceEntity.getId());
        courseResource.setTitle(displayTitle);
        courseResource.setResourceType(type);
        courseResource.setCreateTime(LocalDateTime.now());
        courseResourceDao.insert(courseResource);
        return courseResource.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResource(Long resourceId) {
        CourseResourceEntity existing = courseResourceDao.findById(resourceId);
        if (existing != null) {
            courseAccessApi.assertCanEdit(existing.getCourseId());
            if (existing.getResourceId() != null) {
                ResourceEntity linked = resourceDao.findById(existing.getResourceId());
                if (linked != null) {
                    deleteStorageFile(linked.getFileUrl());
                    resourceDao.deleteById(linked.getId());
                }
            }
        }
        courseResourceDao.deleteById(resourceId);
    }

    private CourseResourceVO toVO(CourseResourceEntity entity) {
        CourseResourceVO vo = new CourseResourceVO();
        vo.setId(entity.getId());
        vo.setCourseId(entity.getCourseId());
        vo.setResourceId(entity.getResourceId());
        vo.setDocumentId(entity.getDocumentId());
        vo.setTitle(entity.getTitle());
        vo.setResourceType(entity.getResourceType());
        vo.setCreateTime(entity.getCreateTime());
        if (entity.getResourceId() != null) {
            ResourceEntity resource = resourceDao.findById(entity.getResourceId());
            if (resource != null) {
                vo.setChapterId(resource.getChapterId());
                vo.setDownloadUrl(resource.getFileUrl());
            }
        }
        return vo;
    }

    private CourseResourceVO teachingToVO(ResourceEntity entity) {
        CourseResourceVO vo = new CourseResourceVO();
        vo.setId(entity.getId());
        vo.setCourseId(entity.getCourseId());
        vo.setResourceId(entity.getId());
        vo.setTitle(entity.getTitle());
        vo.setResourceType(entity.getResourceType());
        vo.setChapterId(entity.getChapterId());
        vo.setDownloadUrl(entity.getFileUrl());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private void deleteStorageFile(String fileUrl) {
        if (!StringUtils.hasText(fileUrl) || !fileUrl.startsWith("/api/storage/files/")) {
            return;
        }
        String rest = fileUrl.substring("/api/storage/files/".length());
        int slash = rest.indexOf('/');
        if (slash <= 0) {
            return;
        }
        String bucket = rest.substring(0, slash);
        String objectKey = rest.substring(slash + 1);
        try {
            fileStorageService.deleteFile(bucket, objectKey);
        } catch (Exception ignored) {
            // 存储删除失败不阻断业务删除
        }
    }

    private static String resolveResourceType(String resourceType, String fileName) {
        if (StringUtils.hasText(resourceType)) {
            return resourceType.trim();
        }
        String lower = fileName != null ? fileName.toLowerCase() : "";
        if (lower.endsWith(".pdf")) {
            return "PDF";
        }
        if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) {
            return "PPT";
        }
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) {
            return "WORD";
        }
        if (lower.endsWith(".mp4") || lower.endsWith(".mov") || lower.endsWith(".avi")) {
            return "VIDEO";
        }
        if (lower.endsWith(".md") || lower.endsWith(".markdown")) {
            return "MD";
        }
        if (lower.endsWith(".txt")) {
            return "TXT";
        }
        return "DOCUMENT";
    }

    private static String stripExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "未命名资料";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            return fileName.substring(0, dot);
        }
        return fileName;
    }
}
