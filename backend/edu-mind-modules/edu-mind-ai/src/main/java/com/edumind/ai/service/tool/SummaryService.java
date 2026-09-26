package com.edumind.ai.service.tool;

import com.edumind.ai.dto.tool.SummaryGenerateDTO;
import com.edumind.ai.dto.tool.SummaryRecordRenameDTO;
import com.edumind.ai.vo.tool.SummaryRecordDetailVO;
import com.edumind.ai.vo.tool.SummaryRecordVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * AI 智能总结服务：把课件 / 讲义 / 自由文本整理成结构化、可复习的知识总结，
 * 并沉淀为可回看、可导出、可复用的教学资产。
 */
public interface SummaryService {

    /** 一次性生成并落库（同步，非流式）。 */
    SummaryRecordVO generate(SummaryGenerateDTO dto);

    /** 流式生成：SSE 返回 stream/reasoning/delta/status/done/error 事件，完成后自动落库。 */
    SseEmitter stream(SummaryGenerateDTO dto);

    /** 中止某次流式生成（用户主动取消）。 */
    void cancelStream(String streamId);

    /** 当前用户的历史总结列表（可按课程筛选，按标题/来源关键字搜索）。 */
    List<SummaryRecordVO> listRecords(Long courseId, String keyword);

    /** 总结详情（含 Markdown 正文）。 */
    SummaryRecordDetailVO getRecord(Long id);

    /** 重命名总结标题。 */
    void renameRecord(Long id, SummaryRecordRenameDTO dto);

    /** 删除总结记录。 */
    void deleteRecord(Long id);
}
