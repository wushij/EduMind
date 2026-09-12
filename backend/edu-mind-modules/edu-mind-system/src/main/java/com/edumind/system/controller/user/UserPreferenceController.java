package com.edumind.system.controller.user;

import com.edumind.common.api.ApiResult;
import com.edumind.system.dto.user.UserPreferenceDTO;
import com.edumind.system.service.user.UserPreferenceService;
import com.edumind.system.vo.user.UserPreferenceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    @GetMapping
    public ApiResult<UserPreferenceVO> getPreferences() {
        return ApiResult.success(userPreferenceService.getMyPreferences());
    }

    @PutMapping
    public ApiResult<UserPreferenceVO> savePreferences(@RequestBody UserPreferenceDTO dto) {
        return ApiResult.success(userPreferenceService.saveMyPreferences(dto));
    }
}
