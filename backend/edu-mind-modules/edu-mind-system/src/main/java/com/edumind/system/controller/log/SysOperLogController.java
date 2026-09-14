package com.edumind.system.controller.log;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumind.common.annotation.OperationLog;
import com.edumind.common.api.ApiResult;
import com.edumind.common.api.PageResult;
import com.edumind.common.enums.BusinessType;
import com.edumind.system.dto.log.SysOperLogPageQueryDTO;
import com.edumind.system.service.log.SysOperLogService;
import com.edumind.system.vo.log.SysOperLogStatsVO;
import com.edumind.system.vo.log.SysOperLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志控制器（对标 E:\wu-admin OperLogController，升级为 EduMind 标准架构与多租户隔离）
 */
@RestController
@RequestMapping({"/api/system/oper-log", "/system/oper-log"})
@RequiredArgsConstructor
public class SysOperLogController {

    private final SysOperLogService sysOperLogService;

    @GetMapping("/page")
    @SaCheckPermission("system:operlog:query")
    public ApiResult<PageResult<SysOperLogVO>> page(SysOperLogPageQueryDTO query) {
        Page<SysOperLogVO> p = sysOperLogService.page(query);
        PageResult<SysOperLogVO> result = PageResult.<SysOperLogVO>builder()
                .total(p.getTotal())
                .pageNum(p.getCurrent())
                .page(p.getCurrent())
                .pageSize(p.getSize())
                .list(p.getRecords())
                .build();
        return ApiResult.success(result);
    }

    @GetMapping("/stats")
    @SaCheckPermission("system:operlog:query")
    public ApiResult<SysOperLogStatsVO> stats() {
        return ApiResult.success(sysOperLogService.getStats());
    }

    @GetMapping("/{id}")
    @SaCheckPermission("system:operlog:query")
    public ApiResult<SysOperLogVO> getById(@PathVariable("id") Long id) {
        return ApiResult.success(sysOperLogService.getById(id));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("system:operlog:delete")
    @OperationLog(module = "系统管理", title = "删除操作日志", businessType = BusinessType.DELETE)
    public ApiResult<Boolean> delete(@PathVariable("id") Long id) {
        sysOperLogService.delete(id);
        return ApiResult.success(true);
    }

    @PostMapping("/batch-delete")
    @SaCheckPermission("system:operlog:delete")
    @OperationLog(module = "系统管理", title = "批量删除操作日志", businessType = BusinessType.DELETE)
    public ApiResult<Boolean> batchDelete(@RequestBody List<Long> ids) {
        sysOperLogService.batchDelete(ids);
        return ApiResult.success(true);
    }

    @DeleteMapping("/clean")
    @SaCheckPermission("system:operlog:clear")
    @OperationLog(module = "系统管理", title = "清空操作日志", businessType = BusinessType.CLEAN)
    public ApiResult<Boolean> clean() {
        sysOperLogService.clean();
        return ApiResult.success(true);
    }

    @GetMapping("/user/{userId}")
    @SaCheckPermission("system:user:view")
    public ApiResult<List<SysOperLogVO>> listRecentByUser(
            @PathVariable("userId") Long userId,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        return ApiResult.success(sysOperLogService.listRecentByUser(userId, limit));
    }
}
