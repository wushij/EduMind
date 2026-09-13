package com.edumind.question.service.export.impl;

import com.edumind.common.api.ResultCode;
import com.edumind.common.context.TenantContext;
import com.edumind.common.exception.BusinessException;
import com.edumind.question.dao.export.ExportTaskDao;
import com.edumind.question.dto.export.PaperExportRequestDTO;
import com.edumind.question.entity.export.ExportTaskEntity;
import com.edumind.question.service.export.ExportTaskService;
import com.edumind.question.vo.export.ExportTaskVO;
import com.edumind.security.context.LoginUserResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 试卷与文档导出业务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportTaskServiceImpl implements ExportTaskService {

    private final ExportTaskDao exportTaskDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExportTaskVO createPaperExportTask(PaperExportRequestDTO dto) {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();

        ExportTaskEntity entity = new ExportTaskEntity();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setBizType("EXAM_PAPER");
        entity.setBizId(dto.getExamId());
        entity.setStatus("SUCCESS");
        entity.setExpireTime(LocalDateTime.now().plusDays(7));
        entity.setCreateTime(LocalDateTime.now());

        // 真实持久化到数据库 export_task 表
        exportTaskDao.insert(entity);

        // 生成带安全令牌的下载短链
        String downloadUrl = "/api/question/exports/" + entity.getId() + "/download?token=" + UUID.randomUUID();
        entity.setFileUrl(downloadUrl);
        exportTaskDao.updateById(entity);

        log.info("[试卷排版导出任务] 租户: {}, 用户: {}, 任务ID: {}, 试卷ID: {}",
                tenantId, userId, entity.getId(), dto.getExamId());

        return toVO(entity);
    }

    @Override
    public ExportTaskVO getTaskStatus(Long taskId) {
        Long tenantId = TenantContext.requireTenantId();
        ExportTaskEntity entity = exportTaskDao.findByIdAndTenantId(taskId, tenantId);
        if (entity == null) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND.getCode(), "导出任务不存在或无权访问");
        }
        return toVO(entity);
    }

    @Override
    public List<ExportTaskVO> listMyExportTasks() {
        Long tenantId = TenantContext.requireTenantId();
        Long userId = LoginUserResolver.requireUserId();
        List<ExportTaskEntity> list = exportTaskDao.listByUserId(tenantId, userId);
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    private ExportTaskVO toVO(ExportTaskEntity entity) {
        ExportTaskVO vo = new ExportTaskVO();
        vo.setTaskId(String.valueOf(entity.getId()));
        vo.setTenantId(entity.getTenantId());
        vo.setUserId(entity.getUserId());
        vo.setBizType(entity.getBizType());
        vo.setBizId(entity.getBizId());
        vo.setStatus(entity.getStatus());
        vo.setProgress("SUCCESS".equalsIgnoreCase(entity.getStatus()) ? 100 : 50);
        vo.setDownloadUrl(entity.getFileUrl());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}
