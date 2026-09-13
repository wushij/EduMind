package com.edumind.ai.service.routing;

import com.edumind.ai.vo.rag.CitationVO;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class IntentDispatchPlan {
    private String route;
    private String agentCode;
    private String targetCode;
    private String systemPrompt;
    private String userPrompt;
    private boolean useRag;
    private boolean skipLlm;
    private String directResponse;
    private Map<String, Object> navigatePayload;
    @Builder.Default
    private List<CitationVO> citations = new ArrayList<>();
}
