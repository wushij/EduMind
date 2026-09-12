package com.edumind.ai.service.prompt;

import com.edumind.ai.dto.prompt.PromptTemplateDTO;
import com.edumind.ai.dto.prompt.PromptTestDTO;
import com.edumind.ai.vo.prompt.PromptTemplateVO;

import java.util.List;

public interface PromptManageService {

    List<PromptTemplateVO> list(String category, String status, String keyword);

    PromptTemplateVO getById(Long id);

    List<com.edumind.ai.vo.prompt.PromptTemplateVersionVO> listVersions(Long templateId);

    Long create(PromptTemplateDTO dto);

    void update(Long id, PromptTemplateDTO dto);

    void publish(Long id);

    String test(Long id, PromptTestDTO dto);
}
