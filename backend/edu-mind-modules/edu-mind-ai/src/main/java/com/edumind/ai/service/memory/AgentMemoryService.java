package com.edumind.ai.service.memory;

import com.edumind.ai.dto.memory.MemoryConsentDTO;
import com.edumind.ai.dto.memory.MemoryFeedbackDTO;
import com.edumind.ai.dto.memory.MemoryItemCreateDTO;
import com.edumind.ai.vo.memory.MemoryItemVO;
import com.edumind.ai.vo.memory.MemoryNamespaceVO;

import java.util.List;

public interface AgentMemoryService {

    MemoryNamespaceVO getNamespace(Long courseId);

    void updateConsent(MemoryConsentDTO dto);

    Long createMemoryItem(MemoryItemCreateDTO dto);

    void forgetMemory(Long memoryId);

    void forgetAll(Long courseId);

    void feedbackMemory(Long memoryId, MemoryFeedbackDTO dto);

    List<MemoryItemVO> retrieveMemories(Long courseId, String queryPrompt);
}
