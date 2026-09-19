package com.edumind.teaching.dto.assignment;

import lombok.Data;

@Data
public class AssignmentSettingsDTO {
    private Boolean aiGradingEnabled;
    private Boolean allowLate;
    private Boolean instantFeedback;
}
