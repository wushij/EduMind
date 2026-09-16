package com.edumind.question.controller.export;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import com.edumind.common.annotation.OperationLog;
import com.edumind.common.api.ApiResult;
import com.edumind.common.enums.BusinessType;
import com.edumind.question.dto.export.PaperExportRequestDTO;
import com.edumind.question.service.export.ExportTaskService;
import com.edumind.question.vo.export.ExportTaskVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 试卷与文档导出控制器 (严格遵守 Controller -> DTO -> Service -> DAO -> Mapper -> Entity 分层规范)
 */
@RestController
@RequestMapping("/api/question/exports")
@RequiredArgsConstructor
@SaCheckLogin
@SaCheckPermission("exam:export")
public class ExportTaskController {

    private final ExportTaskService exportTaskService;

    @PostMapping("/paper")
    @OperationLog(module = "试卷导出", title = "创建导出任务", businessType = BusinessType.EXPORT)
    public ApiResult<ExportTaskVO> createPaperExportTask(@Valid @RequestBody PaperExportRequestDTO dto) {
        return ApiResult.success(exportTaskService.createPaperExportTask(dto));
    }

    @GetMapping("/my")
    public ApiResult<List<ExportTaskVO>> listMyExportTasks() {
        return ApiResult.success(exportTaskService.listMyExportTasks());
    }

    @GetMapping("/{taskId}")
    public ApiResult<ExportTaskVO> getTaskStatus(@PathVariable("taskId") Long taskId) {
        return ApiResult.success(exportTaskService.getTaskStatus(taskId));
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable("taskId") Long taskId) {
        exportTaskService.deleteMyTask(taskId);
    }

    @GetMapping("/{taskId}/download")
    public void download(@PathVariable("taskId") Long taskId,
                         @RequestParam("token") String token,
                         HttpServletResponse response) {
        exportTaskService.download(taskId, token, response);
    }
}
