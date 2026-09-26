package com.edumind.ai.controller.tool;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.edumind.ai.dto.tool.SummaryGenerateDTO;
import com.edumind.ai.dto.tool.SummaryRecordRenameDTO;
import com.edumind.ai.service.tool.SummaryService;
import com.edumind.ai.vo.tool.SummaryRecordDetailVO;
import com.edumind.ai.vo.tool.SummaryRecordVO;
import com.edumind.common.api.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI 智能总结。
 *
 * <p>权限沿用 {@code ai:summary:view}，并放开 {@code course:view} / {@code course:ai:use}：
 * 教师在自己的课程空间内即可直接使用，无需额外授予独立的总结权限。</p>
 *
 * <p>注意：Sa-Token 注解的 {@code value} 只接受「常量表达式 / 数组字面量」，
 * 因此这里每个方法内联权限数组，不能抽取为 static final String[] 常量
 * （数组变量不是常量表达式，会导致编译期类型不兼容）。</p>
 */
@RestController
@RequestMapping("/api/ai/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    /** 一次性生成并落库（同步）。 */
    @SaCheckPermission(value = {"ai:summary:view", "course:view", "course:ai:use"}, mode = SaMode.OR)
    @PostMapping
    public ApiResult<SummaryRecordVO> generate(@RequestBody SummaryGenerateDTO dto) {
        return ApiResult.success(summaryService.generate(dto));
    }

    /** 流式生成：SSE 事件流（stream / reasoning / delta / status / done / error）。 */
    @SaCheckPermission(value = {"ai:summary:view", "course:view", "course:ai:use"}, mode = SaMode.OR)
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody SummaryGenerateDTO dto) {
        return summaryService.stream(dto);
    }

    /** 中止某次流式生成。 */
    @SaCheckPermission(value = {"ai:summary:view", "course:view", "course:ai:use"}, mode = SaMode.OR)
    @DeleteMapping("/stream/{streamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelStream(@PathVariable("streamId") String streamId) {
        summaryService.cancelStream(streamId);
    }

    /** 历史总结列表（可按课程筛选 + 关键字搜索）。 */
    @SaCheckPermission(value = {"ai:summary:view", "course:view", "course:ai:use"}, mode = SaMode.OR)
    @GetMapping("/records")
    public ApiResult<List<SummaryRecordVO>> listRecords(
            @RequestParam(value = "courseId", required = false) Long courseId,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ApiResult.success(summaryService.listRecords(courseId, keyword));
    }

    /** 总结详情（含正文）。 */
    @SaCheckPermission(value = {"ai:summary:view", "course:view", "course:ai:use"}, mode = SaMode.OR)
    @GetMapping("/records/{id}")
    public ApiResult<SummaryRecordDetailVO> getRecord(@PathVariable("id") Long id) {
        return ApiResult.success(summaryService.getRecord(id));
    }

    /** 重命名总结标题。 */
    @SaCheckPermission(value = {"ai:summary:view", "course:view", "course:ai:use"}, mode = SaMode.OR)
    @PutMapping("/records/{id}")
    public ApiResult<Void> renameRecord(@PathVariable("id") Long id,
                                        @Valid @RequestBody SummaryRecordRenameDTO dto) {
        summaryService.renameRecord(id, dto);
        return ApiResult.success();
    }

    /** 删除总结记录。 */
    @SaCheckPermission(value = {"ai:summary:view", "course:view", "course:ai:use"}, mode = SaMode.OR)
    @DeleteMapping("/records/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRecord(@PathVariable("id") Long id) {
        summaryService.deleteRecord(id);
    }
}
