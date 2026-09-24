package com.edumind.ai.vo.assistant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 追问建议出参。
 *
 * <p>{@code aiGenerated=false} 或 {@code prompts} 为空时，前端会回落到本地规则生成器，
 * 因此调用方无需把「模型不可用」当成错误处理，也不要向用户抛出异常提示。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpSuggestVO {

    private List<String> prompts;

    /** true=本轮追问由模型依据真实问答内容生成；false=模型未返回可用结果，交由前端规则兜底 */
    private boolean aiGenerated;

    private String sourceLabel;
}
