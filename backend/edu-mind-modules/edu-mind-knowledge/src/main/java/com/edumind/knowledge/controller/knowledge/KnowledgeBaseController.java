package com.edumind.knowledge.controller.knowledge;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.knowledge.dto.knowledge.KnowledgeBaseCreateDTO;
import com.edumind.knowledge.dto.knowledge.KnowledgeBaseUpdateDTO;
import com.edumind.knowledge.service.chunk.ChunkService;
import com.edumind.knowledge.service.knowledge.KnowledgeBaseService;
import com.edumind.knowledge.vo.knowledge.ChunkStatsVO;
import com.edumind.knowledge.vo.knowledge.KnowledgeBaseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final ChunkService chunkService;

    @SaCheckPermission("knowledge:edit")
    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody KnowledgeBaseCreateDTO dto) {
        return ApiResult.success(knowledgeBaseService.create(dto));
    }

    @SaCheckPermission("knowledge:view")
    @GetMapping("/{id}")
    public ApiResult<KnowledgeBaseVO> getById(@PathVariable("id") Long id) {
        return ApiResult.success(knowledgeBaseService.getById(id));
    }

    @SaCheckPermission("knowledge:view")
    @GetMapping
    public ApiResult<List<KnowledgeBaseVO>> list(@RequestParam(value = "courseId", required = false) Long courseId) {
        return ApiResult.success(knowledgeBaseService.list(courseId));
    }

    @SaCheckPermission("knowledge:view")
    @GetMapping("/{id}/chunk-stats")
    public ApiResult<ChunkStatsVO> chunkStats(@PathVariable("id") Long id) {
        return ApiResult.success(chunkService.getChunkStats(id));
    }

    @SaCheckPermission("knowledge:edit")
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable("id") Long id, @RequestBody KnowledgeBaseUpdateDTO dto) {
        knowledgeBaseService.update(id, dto);
        return ApiResult.success();
    }

    @SaCheckPermission("knowledge:edit")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable("id") Long id) {
        knowledgeBaseService.delete(id);
        return ApiResult.success();
    }
}
