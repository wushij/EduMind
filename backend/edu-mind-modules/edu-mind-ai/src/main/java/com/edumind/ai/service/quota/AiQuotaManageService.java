package com.edumind.ai.service.quota;

import com.edumind.ai.dto.quota.SysAiQuotaUpdateDTO;
import com.edumind.ai.vo.quota.SysAiQuotaVO;

import java.util.List;

public interface AiQuotaManageService {

    List<SysAiQuotaVO> listAll();

    void updateUserQuota(Long userId, SysAiQuotaUpdateDTO dto);
}
