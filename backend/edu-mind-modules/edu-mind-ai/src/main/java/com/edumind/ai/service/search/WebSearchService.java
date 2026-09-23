package com.edumind.ai.service.search;

import java.util.List;

public interface WebSearchService {

    /**
     * 执行联网检索
     *
     * @param query 用户提问关键词
     * @param limit 返回最大条数
     * @return 检索结果列表
     */
    List<WebSearchResult> search(String query, int limit);

    /**
     * 将检索结果格式化为可直接注入大模型 System / User Prompt 的提示词块
     *
     * @param results 检索结果列表
     * @return 格式化后的上下文文本
     */
    String formatSearchResultsForPrompt(List<WebSearchResult> results);
}
