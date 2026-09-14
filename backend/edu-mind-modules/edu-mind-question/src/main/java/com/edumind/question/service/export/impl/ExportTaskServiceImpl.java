package com.edumind.question.service.export.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.edumind.common.api.ResultCode;
import com.edumind.common.constant.SecurityConstant;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.common.model.LoginUser;
import com.edumind.common.model.UserContext;
import com.edumind.common.utils.TenantObjectKeyBuilder;
import com.edumind.infrastructure.oss.FileStorageService;
import com.edumind.question.dao.export.ExportTaskDao;
import com.edumind.question.dto.export.PaperExportRequestDTO;
import com.edumind.question.entity.export.ExportTaskEntity;
import com.edumind.question.service.export.ExportTaskDispatcher;
import com.edumind.question.service.export.ExportTaskService;
import com.edumind.question.vo.export.ExportTaskVO;
import com.edumind.security.context.LoginUserResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 试卷与文档导出业务服务实现 (严格遵守 Controller -> DTO -> Service -> DAO -> Mapper -> Entity 分层规范)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportTaskServiceImpl implements ExportTaskService {

    private final ExportTaskDao exportTaskDao;
    private final ExportTaskDispatcher exportTaskDispatcher;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    @Value("${minio.bucketName:edumind}")
    private String bucketName;

    @Override
    public ExportTaskVO createPaperExportTask(PaperExportRequestDTO dto) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        if (dto == null || dto.getExamId() == null) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "试卷 ID 不能为空");
        }

        String paramsJson = null;
        try {
            paramsJson = objectMapper.writeValueAsString(dto);
        } catch (Exception e) {
            log.warn("[试卷导出] 序列化导出排版参数失败: {}", e.getMessage());
        }

        ExportTaskEntity entity = new ExportTaskEntity();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setBizType("EXAM_PAPER");
        entity.setBizId(dto.getExamId());
        entity.setStatus("PENDING");
        entity.setExportParams(paramsJson);
        entity.setExpireTime(LocalDateTime.now().plusDays(7));
        entity.setCreateTime(LocalDateTime.now());

        // 真实持久化到数据库 export_task 表
        exportTaskDao.insert(entity);

        // 异步派发 Worker
        exportTaskDispatcher.dispatchAsync(entity.getId(), tenantId);

        log.info("[试卷排版导出任务已创建] 租户: {}, 用户: {}, 任务ID: {}, 试卷ID: {}",
                tenantId, userId, entity.getId(), dto.getExamId());

        return toVO(entity);
    }

    @Override
    public ExportTaskVO getTaskStatus(Long taskId) {
        ExportTaskEntity entity = requireAccessibleTask(taskId);
        return toVO(entity);
    }

    @Override
    public List<ExportTaskVO> listMyExportTasks() {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();
        List<ExportTaskEntity> list = exportTaskDao.listByUserId(tenantId, userId);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public void download(Long taskId, String token, HttpServletResponse response) {
        ExportTaskEntity task = requireAccessibleTask(taskId);

        // 1. 校验 token 常量时间匹配
        if (token == null || task.getDownloadToken() == null ||
                !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), task.getDownloadToken().getBytes(StandardCharsets.UTF_8))) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "下载鉴权令牌无效或已失效 (越权拦截)");
        }

        // 2. 校验有效期
        if (task.getExpireTime() != null && task.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "下载链接已过期");
        }

        // 3. 校验状态为 SUCCESS 且 object_key 非空
        if (!"SUCCESS".equalsIgnoreCase(task.getStatus())) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED.getCode(), "导出任务尚未生成完毕，当前状态: " + task.getStatus());
        }

        if (task.getObjectKey() == null || task.getObjectKey().isBlank()) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "导出文件不存在");
        }

        // 4. 校验 objectKey 租户归属
        if (!TenantObjectKeyBuilder.validateTenantOwnership(task.getTenantId(), task.getObjectKey())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "拒绝跨租户访问存储文件");
        }

        // 5. 流式写出到客户端
        try (InputStream inputStream = fileStorageService.getFile(bucketName, task.getObjectKey())) {
            if (inputStream == null) {
                throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "存储介质中未找到目标文件");
            }
            String filename = task.getObjectKey().substring(task.getObjectKey().lastIndexOf('/') + 1);
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(filename, StandardCharsets.UTF_8));
            StreamUtils.copy(inputStream, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            log.error("[试卷导出下载] 读取或写出文件异常: taskId={}, error={}", taskId, e.getMessage());
            throw new BusinessException("文件下载流读取失败: " + e.getMessage());
        }
    }

    private ExportTaskEntity requireAccessibleTask(Long taskId) {
        ExportTaskEntity task = exportTaskDao.findByIdIgnoreTenant(taskId);
        if (task == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "导出任务不存在");
        }

        // 租户隔离校验 (IDOR 越权拦截)
        Long currentTenantId = TenantContext.getTenantId();
        if (currentTenantId != null && task.getTenantId() != null && !currentTenantId.equals(task.getTenantId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权访问其他租户的导出任务 (IDOR 越权拦截)");
        }

        // 用户归属校验 (默认仅创建者或系统管理员可查看与下载)
        Long currentUserId = LoginUserResolver.requireUserId();
        boolean isAdmin = isAdmin();
        if (!isAdmin && task.getUserId() != null && !task.getUserId().equals(currentUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "无权访问其他用户的导出任务");
        }

        return task;
    }

    private boolean isAdmin() {
        LoginUser user = UserContext.get();
        if (user != null && user.getRoles() != null && user.getRoles().contains(SecurityConstant.ROLE_ADMIN)) {
            return true;
        }
        if (StpUtil.isLogin()) {
            try {
                Long loginId = StpUtil.getLoginIdAsLong();
                return Long.valueOf(1L).equals(loginId) || StpUtil.hasRole(SecurityConstant.ROLE_ADMIN);
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private ExportTaskVO toVO(ExportTaskEntity entity) {
        ExportTaskVO vo = new ExportTaskVO();
        vo.setTaskId(String.valueOf(entity.getId()));
        vo.setTenantId(entity.getTenantId());
        vo.setUserId(entity.getUserId());
        vo.setBizType(entity.getBizType());
        vo.setBizId(entity.getBizId());
        vo.setStatus(entity.getStatus());
        vo.setErrorMsg(entity.getErrorMsg());

        int progress = 0;
        if ("SUCCESS".equalsIgnoreCase(entity.getStatus())) {
            progress = 100;
        } else if ("PROCESSING".equalsIgnoreCase(entity.getStatus())) {
            progress = 50;
        } else {
            progress = 0;
        }
        vo.setProgress(progress);

        vo.setDownloadUrl(entity.getFileUrl());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
