package com.edumind.ai.service.tool.impl;

import com.edumind.ai.converter.AiConverter;
import com.edumind.ai.dao.AiToolDao;
import com.edumind.ai.service.tool.AiToolService;
import com.edumind.ai.vo.AiToolVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiToolServiceImpl implements AiToolService {

    private final AiToolDao aiToolDao;
    private final AiConverter aiConverter;

    @Override
    public List<AiToolVO> listTools(String category, String keyword) {
        return aiToolDao.list(category, keyword).stream()
                .map(aiConverter::toToolVO)
                .collect(Collectors.toList());
    }
}
