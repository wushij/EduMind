package com.edumind.system.dto.user;

import lombok.Data;

@Data
public class UserPreferenceDTO {
    private String theme;
    private String language;
    private String defaultModel;
    private Boolean enableRag;
    private Boolean enableNotification;
    private String preferencesJson;
}
