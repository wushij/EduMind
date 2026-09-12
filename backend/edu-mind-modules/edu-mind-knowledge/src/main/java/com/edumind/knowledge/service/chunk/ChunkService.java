package com.edumind.knowledge.service.chunk;

import com.edumind.common.api.PageResult;
import com.edumind.knowledge.vo.knowledge.ChunkStatsVO;
import com.edumind.knowledge.vo.knowledge.ChunkTaskVO;
import com.edumind.knowledge.vo.knowledge.ChunkVO;

public interface ChunkService {

    ChunkTaskVO triggerChunk(Long documentId);

    PageResult<ChunkVO> pageChunks(Long documentId, long page, long pageSize, String keyword);

    ChunkStatsVO getChunkStats(Long knowledgeBaseId);
}
