package com.edumind.ai.service.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSearchResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 搜索结果标题 */
    private String title;

    /** 网页摘要片段 */
    private String snippet;

    /** 网页原始URL */
    private String url;

    /** 来源平台或站点名称 */
    private String source;
}
