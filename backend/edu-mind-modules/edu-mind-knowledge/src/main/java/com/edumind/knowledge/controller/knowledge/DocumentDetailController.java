package com.edumind.knowledge.controller.knowledge;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.api.ApiResult;
import com.edumind.knowledge.service.knowledge.DocumentService;
import com.edumind.knowledge.vo.knowledge.KnowledgeDocumentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentDetailController {

    private final DocumentService documentService;

    @SaCheckPermission("knowledge:view")
    @GetMapping("/{id}")
    public ApiResult<KnowledgeDocumentVO> getById(@PathVariable("id") Long id) {
        return ApiResult.success(documentService.getById(id));
    }
}
