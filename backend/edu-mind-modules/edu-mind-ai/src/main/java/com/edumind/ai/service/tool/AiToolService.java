package com.edumind.ai.service.tool;

import com.edumind.ai.vo.AiToolVO;

import java.util.List;

public interface AiToolService {
    List<AiToolVO> listTools(String category, String keyword);
}
