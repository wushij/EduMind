package com.edumind.ai.vo.memory;

import lombok.Data;
import java.util.List;

@Data
public class MemoryOverviewVO {
    private Integer totalMemories;
    private Integer preferenceCount;
    private Integer profileCount;
    private Integer episodicCount;
    private Integer feedbackCount;
    private Integer encryptedCount;
    private Boolean globalConsentGranted;
    private List<MemorySpaceItemVO> spaces;
}
