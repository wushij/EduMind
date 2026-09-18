package com.edumind.knowledge.vo.knowledge;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChunkKeywordSearchVO {

    /** 整句/短语 LIKE 召回，按 chunk_index 排序 */
    private List<Long> phraseRankedChunkIds;

    /** 分词 OR 召回 */
    private List<Long> tokenRankedChunkIds;

    /** 英文技术词 AND 召回（多词同现） */
    private List<Long> techTermRankedChunkIds;
}
