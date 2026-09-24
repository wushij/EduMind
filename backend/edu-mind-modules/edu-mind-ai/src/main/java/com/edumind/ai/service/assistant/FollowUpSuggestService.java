package com.edumind.ai.service.assistant;

import com.edumind.ai.dto.assistant.FollowUpSuggestDTO;
import com.edumind.ai.vo.assistant.FollowUpSuggestVO;

/** 基于本轮真实问答内容生成「下一步追问」，供全局助手 / 课程 AI / 题目辅导共用 */
public interface FollowUpSuggestService {

    FollowUpSuggestVO suggest(FollowUpSuggestDTO dto);
}
