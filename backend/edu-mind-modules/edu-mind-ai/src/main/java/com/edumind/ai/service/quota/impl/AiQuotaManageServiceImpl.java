package com.edumind.ai.service.quota.impl;

import com.edumind.ai.converter.AiConverter;
import com.edumind.ai.dao.SysAiQuotaDao;
import com.edumind.ai.dto.quota.SysAiQuotaUpdateDTO;
import com.edumind.ai.entity.SysAiQuotaEntity;
import com.edumind.ai.service.quota.AiQuotaManageService;
import com.edumind.ai.vo.quota.SysAiQuotaVO;
import com.edumind.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiQuotaManageServiceImpl implements AiQuotaManageService {

    private final SysAiQuotaDao sysAiQuotaDao;
    private final AiConverter aiConverter;

    @Override
    public List<SysAiQuotaVO> listAll() {
        return sysAiQuotaDao.listAll().stream()
                .map(aiConverter::toQuotaVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserQuota(Long userId, SysAiQuotaUpdateDTO dto) {
        if (userId == null) {
            throw new BusinessException("用户 ID 不能为空");
        }
        SysAiQuotaEntity existing = sysAiQuotaDao.findByUserId(userId);
        if (existing == null) {
            SysAiQuotaEntity entity = new SysAiQuotaEntity();
            entity.setUserId(userId);
            entity.setDailyTokenLimit(dto.getDailyTokenLimit());
            entity.setDailyCallLimit(dto.getDailyCallLimit());
            sysAiQuotaDao.insert(entity);
            return;
        }
        if (dto.getDailyTokenLimit() != null) {
            existing.setDailyTokenLimit(dto.getDailyTokenLimit());
        }
        if (dto.getDailyCallLimit() != null) {
            existing.setDailyCallLimit(dto.getDailyCallLimit());
        }
        sysAiQuotaDao.updateById(existing);
    }
}
