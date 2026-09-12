package com.edumind.system.vo.user;

import lombok.Data;

@Data
public class UserPreferenceVO {
    private String theme;
    private String language;
    private String defaultModel;
    private Boolean enableRag;
    private Boolean enableNotification;
    private String preferencesJson;
}
