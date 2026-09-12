package com.edumind.system.service.user;

import com.edumind.system.dto.user.UserPreferenceDTO;
import com.edumind.system.vo.user.UserPreferenceVO;

public interface UserPreferenceService {

    UserPreferenceVO getMyPreferences();

    UserPreferenceVO saveMyPreferences(UserPreferenceDTO dto);
}
