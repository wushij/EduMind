package com.edumind.ai.service.memory;

import com.edumind.ai.dto.memory.MemoryConsentDTO;
import com.edumind.ai.dto.memory.MemoryFeedbackDTO;
import com.edumind.ai.dto.memory.MemoryItemCreateDTO;
import com.edumind.ai.dto.memory.MemoryItemUpdateDTO;
import com.edumind.ai.vo.memory.MemoryDecryptVO;
import com.edumind.ai.vo.memory.MemoryItemVO;
import com.edumind.ai.vo.memory.MemoryNamespaceVO;
import com.edumind.ai.vo.memory.MemoryOverviewVO;

import java.util.List;

public interface AgentMemoryService {

    MemoryNamespaceVO getNamespace(Long courseId);

    MemoryOverviewVO getMemoryOverview();

    void updateConsent(MemoryConsentDTO dto);

    Long createMemoryItem(MemoryItemCreateDTO dto);

    void updateMemoryItem(Long memoryId, MemoryItemUpdateDTO dto);

    void forgetMemory(Long memoryId);

    void forgetAll(Long courseId);

    void feedbackMemory(Long memoryId, MemoryFeedbackDTO dto);

    List<MemoryItemVO> retrieveMemories(Long courseId, String queryPrompt);

    int seedSampleMemories(Long courseId);

    List<MemoryItemVO> extractMemoriesFromActivity(Long courseId);

    MemoryDecryptVO decryptMemory(Long memoryId);

    int cleanupDuplicates(Long courseId);

    int confirmBatchCandidates(Long courseId, List<MemoryItemCreateDTO> candidates);
}
