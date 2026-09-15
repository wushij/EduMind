package com.edumind.ai.service.tool;

import com.edumind.ai.dto.tool.AiToolFlagsDTO;
import com.edumind.ai.dto.tool.AiToolSaveDTO;
import com.edumind.ai.dto.tool.AiToolUpdateDTO;
import com.edumind.ai.vo.tool.AiToolAdminVO;
import com.edumind.ai.vo.tool.AiToolStatsVO;

import java.util.List;

public interface AiToolManageService {

    List<AiToolAdminVO> list(String category, Integer status, String keyword, Boolean isRecommended);

    AiToolStatsVO stats();

    AiToolAdminVO getById(String id);

    String create(AiToolSaveDTO dto);

    void update(String id, AiToolUpdateDTO dto);

    void publish(String id);

    void offline(String id);

    void updateFlags(String id, AiToolFlagsDTO dto);

    void delete(String id);
}
