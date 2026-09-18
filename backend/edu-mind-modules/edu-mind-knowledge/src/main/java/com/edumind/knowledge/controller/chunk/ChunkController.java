package com.edumind.knowledge.controller.chunk;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.PageResult;
import com.edumind.knowledge.service.chunk.ChunkService;
import com.edumind.knowledge.vo.knowledge.ChunkTaskVO;
import com.edumind.knowledge.vo.knowledge.ChunkVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class ChunkController {

    private final ChunkService chunkService;

    @SaCheckPermission("knowledge:edit")
    @PostMapping("/{documentId}/chunk")
    public ApiResult<ChunkTaskVO> triggerChunk(@PathVariable("documentId") Long documentId) {
        return ApiResult.success(chunkService.triggerChunk(documentId));
    }

    @SaCheckPermission("knowledge:view")
    @GetMapping("/{documentId}/chunks")
    public ApiResult<PageResult<ChunkVO>> pageChunks(
            @PathVariable("documentId") Long documentId,
            @RequestParam(value = "page", defaultValue = "1") long page,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status) {
        return ApiResult.success(chunkService.pageChunks(documentId, page, pageSize, keyword, status));
    }
}
