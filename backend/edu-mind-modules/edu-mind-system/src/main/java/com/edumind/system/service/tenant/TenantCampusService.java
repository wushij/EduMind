package com.edumind.system.service.tenant;

import com.edumind.system.dto.tenant.CampusCreateDTO;
import com.edumind.system.dto.tenant.CampusUpdateDTO;
import com.edumind.system.vo.tenant.CampusVO;

import java.util.List;

public interface TenantCampusService {

    List<CampusVO> listCampuses(Long tenantId);

    Long createCampus(Long tenantId, CampusCreateDTO dto);

    void updateCampus(Long tenantId, Long campusId, CampusUpdateDTO dto);

    void updateCampusStatus(Long tenantId, Long campusId, Integer status);

    void deleteCampus(Long tenantId, Long campusId);
}
